package com.zkx.aipassagecreator.agent;

import com.zkx.aipassagecreator.annotation.AgentLog;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.zkx.aipassagecreator.config.SseEmitterManager;
import com.zkx.aipassagecreator.constant.ArticleConstant;
import com.zkx.aipassagecreator.model.context.ArticleTaskContext;
import com.zkx.aipassagecreator.model.enums.SSEMessageTypeEnum;
import com.zkx.aipassagecreator.model.vo.ImageItem;
import com.zkx.aipassagecreator.service.image.CosService;
import com.zkx.aipassagecreator.service.image.ImageFetchResult;
import com.zkx.aipassagecreator.service.image.ImageProvider;
import com.zkx.aipassagecreator.service.image.ImageProviderRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体5：获取配图（策略模式）
 * <p>
 * 按用户启用的配图方式（enabledImageMethods）轮询分配策略逐张生成，
 * 策略失败时逐级降级：分配策略 → 默认 Pexels（含 Picsum 兜底）→ Picsum 直链。
 * 全部完成后推送 AGENT5_COMPLETE(images)。
 */
@Slf4j
@Component
public class ImageFetchAgent {

    @Resource
    private ImageProviderRegistry providerRegistry;

    @Resource
    private CosService cosService;

    @Resource
    private SseEmitterManager sseEmitterManager;

    @AgentLog
    public void execute(ArticleTaskContext ctx) {
        long start = System.currentTimeMillis();
        try {
            List<ImageItem> requirements = ctx.getImageRequirements();
            List<ImageItem> images = new ArrayList<>();

            List<ImageProvider> providers = resolveProviders(ctx.getEnabledImageMethods());

            for (int i = 0; i < requirements.size(); i++) {
                if (ctx.isCancelled()) {
                    break;
                }
                ImageItem req = requirements.get(i);
                ImageItem img = fetchOne(req, providers, ctx.getTaskId(), i);
                images.add(img);
                sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.IMAGE_COMPLETE, null);
            }

            ctx.setImages(images);
            Map<String, Object> extra = new HashMap<>();
            extra.put(ArticleConstant.SSE_IMAGES, images);
            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.AGENT5_COMPLETE, extra);
        } catch (Exception e) {
            log.error("ImageFetchAgent 执行失败: {}", e.getMessage(), e);
            ctx.setCancelled(true);
            sseEmitterManager.error(ctx.getTaskId(), "配图生成失败: " + e.getMessage());
        } finally {
            ctx.recordDuration("ImageFetchAgent", System.currentTimeMillis() - start);
        }
    }

    /**
     * 将启用的配图方式解析为策略列表；未配置或全部无效时默认 Pexels
     */
    private List<ImageProvider> resolveProviders(List<String> enabledMethods) {
        List<ImageProvider> providers = new ArrayList<>();
        if (enabledMethods != null) {
            for (String m : enabledMethods) {
                ImageProvider p = providerRegistry.get(m);
                if (p != null && !providers.contains(p)) {
                    providers.add(p);
                }
            }
        }
        if (providers.isEmpty()) {
            providers.add(providerRegistry.getDefault());
        }
        return providers;
    }

    /**
     * 按顺序轮询选择策略（第 i 张图用第 i % n 种方式）
     */
    private ImageProvider pickProvider(List<ImageProvider> providers, int index) {
        return providers.get(index % providers.size());
    }

    /**
     * 获取单张配图：分配策略 → 失败降级默认 Pexels → 仍失败 Picsum 直链
     */
    private ImageItem fetchOne(ImageItem req, List<ImageProvider> providers, String taskId, int index) {
        String keywords = req.getKeywords() == null ? "technology" : req.getKeywords();

        ImageProvider assigned = pickProvider(providers, index);
        ImageFetchResult result = safeGenerate(assigned, req, taskId, index);
        // 分配策略失败/无结果 → 降级默认 Pexels（内部含 Picsum 兜底）
        if (result == null || !hasContent(result)) {
            result = safeGenerate(providerRegistry.getDefault(), req, taskId, index);
        }

        String finalUrl = null;
        String method = result == null ? "PICSUM" : result.getMethod();
        String ext = result == null ? "jpg" : result.getExt();
        if (result != null) {
            if (result.getData() != null) {
                // 自生成内容（Mermaid/SVG/AI 生图）：必须上传 COS，未配置则降级
                finalUrl = cosService.upload(result.getData(), taskId, "image_" + (index + 1) + "." + ext);
            } else if (StrUtil.isNotBlank(result.getUrl())) {
                // 远程图：尝试下载上传 COS，失败/未配置则保留源 URL
                finalUrl = uploadRemoteOrKeep(result.getUrl(), taskId, index, ext);
            }
        }
        if (StrUtil.isBlank(finalUrl)) {
            // 最终兜底：Picsum 直链
            finalUrl = picsumUrl(keywords);
            method = "PICSUM";
        }

        ImageItem img = new ImageItem();
        img.setPosition(req.getPosition() == null ? index + 1 : req.getPosition());
        img.setKeywords(keywords);
        img.setSectionTitle(req.getSectionTitle());
        img.setDescription(req.getDescription());
        img.setMethod(method);
        img.setUrl(finalUrl);
        return img;
    }

    private ImageFetchResult safeGenerate(ImageProvider provider, ImageItem req, String taskId, int index) {
        try {
            ImageFetchResult r = provider.generate(req, taskId, index);
            if (r != null && !hasContent(r)) {
                return null;
            }
            return r;
        } catch (Exception e) {
            log.warn("配图策略 {} 执行异常: {}", provider.method(), e.getMessage());
            return null;
        }
    }

    private boolean hasContent(ImageFetchResult r) {
        return r.getData() != null || StrUtil.isNotBlank(r.getUrl());
    }

    /**
     * 下载远程图片并上传 COS；COS 未配置或下载失败返回源 URL
     */
    private String uploadRemoteOrKeep(String url, String taskId, int index, String ext) {
        try {
            byte[] bytes = HttpUtil.downloadBytes(url);
            if (bytes != null && bytes.length > 0) {
                String cosUrl = cosService.upload(bytes, taskId, "image_" + (index + 1) + "." + ext);
                if (cosUrl != null) {
                    return cosUrl;
                }
            }
        } catch (Exception e) {
            log.warn("图片下载/上传 COS 失败: {}", e.getMessage());
        }
        return url;
    }

    private String picsumUrl(String keywords) {
        String seed = keywords == null || keywords.isBlank() ? "default" : keywords.replaceAll("\\s+", "-");
        return ArticleConstant.PICSUM_API_URL.replace("{seed}", seed);
    }
}

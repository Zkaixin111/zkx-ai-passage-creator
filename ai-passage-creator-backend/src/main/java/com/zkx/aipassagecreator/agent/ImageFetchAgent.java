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
import com.zkx.aipassagecreator.service.image.provider.ImageFetchResult;
import com.zkx.aipassagecreator.service.image.provider.ImageProvider;
import com.zkx.aipassagecreator.service.image.provider.ImageProviderRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 智能体5：获取配图（策略模式 + 并行抓取 + 三级降级）
 * <p>
 * 这是 6 个智能体中最复杂的一个。核心设计：
 * <p>
 * 1. 策略模式：6 种配图方式实现 ImageProvider 接口（Pexels/AI 生图/Mermaid/SVG/Iconify/表情包），
 *    通过 ImageProviderRegistry 注册表选择策略
 * <p>
 * 2. 并行抓取：N 张配图用 CompletableFuture 并行获取，总耗时 ≈ 单图耗时（而非 N × 单图）
 *    专用线程池 imageFetchExecutor（core=4, max=8）和智能体线程池隔离
 * <p>
 * 3. 三级降级链（保证最终一定有图）：
 *    第 1 级：分配策略（轮询选择，第 i 张图用第 i%n 种方式）
 *    第 2 级：默认 Pexels（含 Picsum 兜底）
 *    第 3 级：Picsum 直链（https://picsum.photos/seed/xxx）
 * <p>
 * 配图来源两种：远程 URL（Pexels）或自生成内容（Mermaid/SVG/AI 生图），
 * 自生成内容必须上传 COS，远程图尝试下载上传 COS（失败保留源 URL）。
 */
@Slf4j
@Component
public class ImageFetchAgent {

    /** 配图策略注册表，维护 method → ImageProvider 的映射 */
    @Resource
    private ImageProviderRegistry providerRegistry;

    /** 腾讯云 COS 上传服务，自生成内容必须上传后才能拿到 URL */
    @Resource
    private CosService cosService;

    @Resource
    private SseEmitterManager sseEmitterManager;

    /** 配图专用线程池（和智能体链路线程池隔离，避免配图慢拖影响后续任务） */
    @Resource
    @Qualifier("imageFetchExecutor")
    private Executor imageFetchExecutor;

    @AgentLog
    public void execute(ArticleTaskContext ctx) {
        long start = System.currentTimeMillis();
        try {
            List<ImageItem> requirements = ctx.getImageRequirements();  // 智能体4 产出的配图需求
            List<ImageProvider> providers = resolveProviders(ctx.getEnabledImageMethods());  // 用户选的配图方式
            List<ImageItem> images = new ArrayList<>();

            if (requirements == null || requirements.isEmpty()) {
                ctx.setImages(images);
            } else {
                int n = requirements.size();
                // 每张配图提交一个 CompletableFuture，用 imageFetchExecutor 并行执行
                // 并行效果：3 张图各耗时 5 秒 → 总耗时 ≈ 5 秒（非 15 秒）
                List<CompletableFuture<ImageItem>> futures = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    ImageItem req = requirements.get(i);
                    int index = i;
                    // 每张图都包一层异常兜底：单张图失败绝不能拖垮整批配图
                    futures.add(CompletableFuture.supplyAsync(
                            () -> fetchOneSafely(req, providers, ctx.getTaskId(), index), imageFetchExecutor));
                }
                // 主线程按顺序 join 收集结果（此时各图已并行完成或即将完成）
                // 每完成一张推送 SSE IMAGE_COMPLETE，前端进度条更新
                for (CompletableFuture<ImageItem> future : futures) {
                    images.add(future.join());
                    sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.IMAGE_COMPLETE, null);
                }
                ctx.setImages(images);
            }
            // 推送全部配图结果
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
     * 将用户选的配图方式名称解析为策略对象列表
     * 未配置或全部无效时默认使用 Pexels（providerRegistry.getDefault()）
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
     * 轮询选择策略：第 0 张图用第 0 种方式，第 1 张用第 1 种，第 n 张用第 n%n 种
     * 这样多张图会均匀分配到不同配图方式上
     */
    private ImageProvider pickProvider(List<ImageProvider> providers, int index) {
        return providers.get(index % providers.size());
    }

    /**
     * 获取单张配图（三级降级）
     * <p>
     * 第 1 级：用分配的策略尝试获取
     * 第 2 级：第 1 级失败 → 用默认 Pexels 再试（Pexels 内部也有 Picsum 兜底）
     * 第 3 级：第 2 级也失败 → 直接用 Picsum 直链（保证一定有图）
     */
    private ImageItem fetchOne(ImageItem req, List<ImageProvider> providers, String taskId, int index) {
        String keywords = req.getKeywords() == null ? "technology" : req.getKeywords();

        // 第 1 级：分配策略
        ImageProvider assigned = pickProvider(providers, index);
        ImageFetchResult result = safeGenerate(assigned, req, taskId, index);
        // 第 2 级：分配策略失败 → 降级默认 Pexels
        if (result == null || !hasContent(result)) {
            result = safeGenerate(providerRegistry.getDefault(), req, taskId, index);
        }

        // 处理获取到的配图：自生成内容上传 COS，远程图下载后上传 COS
        String finalUrl = null;
        String method = result == null ? "PICSUM" : result.getMethod();
        String ext = result == null ? "jpg" : result.getExt();
        if (result != null) {
            if (result.getData() != null) {
                // 自生成内容（Mermaid SVG / AI 生图）：拿到的是 byte[]，必须上传 COS 才有 URL
                try {
                    finalUrl = cosService.upload(result.getData(), taskId, "image_" + (index + 1) + "." + ext);
                } catch (Exception e) {
                    // COS 不可用不能让整张图失败，交给下面的 Picsum 兜底
                    log.warn("配图 {} 上传 COS 失败: {}", index + 1, e.getMessage());
                }
            } else if (StrUtil.isNotBlank(result.getUrl())) {
                // 远程图（Pexels）：尝试下载后上传 COS（统一 CDN），失败则保留源 URL
                finalUrl = uploadRemoteOrKeep(result.getUrl(), taskId, index, ext);
            }
        }
        // 第 3 级：所有策略都失败 → Picsum 直链兜底
        if (StrUtil.isBlank(finalUrl)) {
            finalUrl = picsumUrl(keywords);
            method = "PICSUM";
        }
        return buildImage(req, keywords, index, method, finalUrl);
    }

    /**
     * 带异常兜底的获取单张配图
     * <p>
     * fetchOne 内部虽已做三级降级，但仍有未预期的运行时异常（如第三方 SDK 抛错）。
     * 这里统一兜住并返回 Picsum 直链，保证 CompletableFuture.join() 永远不抛异常，
     * 单张图的失败不会导致整批配图链路失败、任务被标记取消。
     */
    private ImageItem fetchOneSafely(ImageItem req, List<ImageProvider> providers, String taskId, int index) {
        try {
            return fetchOne(req, providers, taskId, index);
        } catch (Exception e) {
            log.warn("配图 {} 获取异常，降级为 Picsum 兜底: {}", index + 1, e.getMessage());
            String keywords = req == null || req.getKeywords() == null ? "technology" : req.getKeywords();
            return buildImage(req, keywords, index, "PICSUM", picsumUrl(keywords));
        }
    }

    /** 构造最终返回的 ImageItem */
    private ImageItem buildImage(ImageItem req, String keywords, int index, String method, String url) {
        ImageItem img = new ImageItem();
        img.setPosition(req == null || req.getPosition() == null ? index + 1 : req.getPosition());
        img.setKeywords(keywords);
        img.setSectionTitle(req == null ? null : req.getSectionTitle());
        img.setDescription(req == null ? null : req.getDescription());
        img.setMethod(method);
        img.setUrl(url);
        return img;
    }

    /**
     * 安全调用策略（捕获异常，返回 null 而非抛出，避免一张图失败影响其他图）
     */
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

    /** 判断配图结果是否有内容（有 byte[] 数据 或 有远程 URL） */
    private boolean hasContent(ImageFetchResult r) {
        return r.getData() != null || StrUtil.isNotBlank(r.getUrl());
    }

    /**
     * 下载远程图片并上传 COS
     * COS 未配置或下载失败时返回源 URL（降级保留原图链接）
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
        return url;  // COS 不可用就保留源 URL
    }

    /**
     * 生成 Picsum 兜底直链（根据关键词做 seed 保证每次相同关键词出相同的图）
     * 如 https://picsum.photos/seed/ai-education/1280/720
     */
    private String picsumUrl(String keywords) {
        String seed = keywords == null || keywords.isBlank() ? "default" : keywords.replaceAll("\\s+", "-");
        return ArticleConstant.PICSUM_API_URL.replace("{seed}", seed);
    }
}

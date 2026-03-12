package com.zkx.aipassagecreator.agent;

import cn.hutool.http.HttpUtil;
import com.zkx.aipassagecreator.config.SseEmitterManager;
import com.zkx.aipassagecreator.model.context.ArticleTaskContext;
import com.zkx.aipassagecreator.model.enums.SSEMessageTypeEnum;
import com.zkx.aipassagecreator.model.vo.ImageItem;
import com.zkx.aipassagecreator.service.image.CosService;
import com.zkx.aipassagecreator.service.image.PexelsImageSearchService;
import com.zkx.aipassagecreator.service.image.PicsumImageSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体5：获取配图（Pexels → Picsum 兜底 → 上传 COS）
 * <p>
 * 逐张获取并推送 IMAGE_COMPLETE，全部完成后推送 AGENT5_COMPLETE(images)。
 */
@Slf4j
@Component
public class ImageFetchAgent {

    @Resource
    private PexelsImageSearchService pexelsImageSearchService;

    @Resource
    private PicsumImageSearchService picsumImageSearchService;

    @Resource
    private CosService cosService;

    @Resource
    private SseEmitterManager sseEmitterManager;

    public void execute(ArticleTaskContext ctx) {
        long start = System.currentTimeMillis();
        try {
            List<ImageItem> requirements = ctx.getImageRequirements();
            List<ImageItem> images = new ArrayList<>();

            for (int i = 0; i < requirements.size(); i++) {
                if (ctx.isCancelled()) {
                    break;
                }
                ImageItem req = requirements.get(i);
                ImageItem img = fetchOne(req, ctx.getTaskId(), i);
                images.add(img);
                sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.IMAGE_COMPLETE, null);
            }

            ctx.setImages(images);
            Map<String, Object> extra = new HashMap<>();
            extra.put("images", images);
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
     * 获取单张配图：Pexels 搜索 → 失败则 Picsum 兜底 → 上传 COS（可选）
     */
    private ImageItem fetchOne(ImageItem req, String taskId, int index) {
        String keywords = req.getKeywords() == null ? "technology" : req.getKeywords();
        String url = pexelsImageSearchService.search(keywords);
        String method = "PEXELS";
        if (url == null) {
            url = picsumImageSearchService.search(keywords);
            method = "PICSUM";
        }
        // 尝试上传 COS（未配置时返回 null，使用源 URL）
        String cosUrl = uploadToCos(url, taskId, index);
        if (cosUrl != null) {
            url = cosUrl;
        }
        ImageItem img = new ImageItem();
        img.setPosition(req.getPosition() == null ? index + 1 : req.getPosition());
        img.setKeywords(keywords);
        img.setSectionTitle(req.getSectionTitle());
        img.setDescription(req.getDescription());
        img.setMethod(method);
        img.setUrl(url);
        return img;
    }

    /**
     * 下载图片并上传 COS；未配置 COS 或下载失败返回 null
     */
    private String uploadToCos(String url, String taskId, int index) {
        try {
            byte[] bytes = HttpUtil.downloadBytes(url);
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            return cosService.upload(bytes, taskId, "image_" + (index + 1) + ".jpg");
        } catch (Exception e) {
            log.warn("图片下载/上传 COS 失败: {}", e.getMessage());
            return null;
        }
    }
}

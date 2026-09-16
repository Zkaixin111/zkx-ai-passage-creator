package com.zkx.aipassagecreator.service.image.provider;

import cn.hutool.core.util.StrUtil;
import com.zkx.aipassagecreator.model.enums.ImageMethodEnum;
import com.zkx.aipassagecreator.model.vo.ImageItem;
import com.zkx.aipassagecreator.service.image.search.PexelsImageSearchService;
import com.zkx.aipassagecreator.service.image.search.PicsumImageSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * Pexels 配图策略（默认方式）
 * <p>
 * 优先 Pexels 图库搜索；未配置 key 或失败时降级 Picsum 随机图（seed 可复现）。
 * 只负责产出图片 URL，下载与 COS 上传由调用方（ImageFetchAgent）统一处理。
 */
@Slf4j
@Component
public class PexelsImageProvider implements ImageProvider {

    @Resource
    private PexelsImageSearchService pexelsImageSearchService;

    @Resource
    private PicsumImageSearchService picsumImageSearchService;

    @Override
    public ImageMethodEnum method() {
        return ImageMethodEnum.PEXELS;
    }

    @Override
    public ImageFetchResult generate(ImageItem requirement, String taskId, int index) {
        String keywords = StrUtil.isBlank(requirement.getKeywords()) ? "technology" : requirement.getKeywords();
        String url = pexelsImageSearchService.search(keywords);
        if (StrUtil.isNotBlank(url)) {
            return new ImageFetchResult(url, null, ImageMethodEnum.PEXELS.getValue(), "jpg");
        }
        // Picsum 兜底（seed 保证同关键词可复现）
        String picsumUrl = picsumImageSearchService.search(keywords);
        return new ImageFetchResult(picsumUrl, null, ImageMethodEnum.PICSUM.getValue(), "jpg");
    }
}

package com.zkx.aipassagecreator.service.image;

import com.zkx.aipassagecreator.constant.ArticleConstant;
import org.springframework.stereotype.Service;

/**
 * Picsum 随机图兜底实现
 * <p>
 * 当 Pexels 搜索失败或未配置时使用，通过 seed 保证同关键词返回可复现图片。
 */
@Service
public class PicsumImageSearchService implements ImageSearchService {

    @Override
    public String search(String keywords) {
        String seed = keywords == null || keywords.isBlank()
                ? "default"
                : keywords.replaceAll("\\s+", "-");
        return ArticleConstant.PICSUM_API_URL.replace("{seed}", seed);
    }
}

package com.zkx.aipassagecreator.service.image;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkx.aipassagecreator.constant.ArticleConstant;
import com.zkx.aipassagecreator.model.enums.ImageMethodEnum;
import com.zkx.aipassagecreator.model.vo.ImageItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Iconify 图标配图策略
 * <p>
 * 通过 Iconify 图标搜索 API 按关键词查图标，返回 SVG 图标直链（远程图，由调用方下载上传 COS）。
 */
@Slf4j
@Component
public class IconifyImageProvider implements ImageProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public ImageMethodEnum method() {
        return ImageMethodEnum.ICONIFY;
    }

    @Override
    public ImageFetchResult generate(ImageItem requirement, String taskId, int index) {
        String keywords = StrUtil.isBlank(requirement.getKeywords()) ? "image" : requirement.getKeywords();
        try {
            String query = URLEncoder.encode(keywords, StandardCharsets.UTF_8);
            String url = ArticleConstant.ICONIFY_SEARCH_URL.replace("{query}", query);
            String body = HttpRequest.get(url).timeout(10_000).execute().body();
            JsonNode root = OBJECT_MAPPER.readTree(body);
            JsonNode icons = root.get("icons");
            if (icons != null && icons.isArray() && !icons.isEmpty()) {
                String icon = icons.get(0).asText();
                String iconUrl = ArticleConstant.ICONIFY_ICON_URL.replace("{icon}", icon);
                return new ImageFetchResult(iconUrl, null, ImageMethodEnum.ICONIFY.getValue(), "svg");
            }
            log.warn("Iconify 未搜索到图标: keywords={}", keywords);
        } catch (Exception e) {
            log.warn("Iconify 调用失败: keywords={}, error={}", keywords, e.getMessage());
        }
        return null;
    }
}

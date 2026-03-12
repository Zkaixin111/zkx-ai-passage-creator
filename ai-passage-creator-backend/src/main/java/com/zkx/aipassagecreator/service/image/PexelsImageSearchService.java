package com.zkx.aipassagecreator.service.image;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkx.aipassagecreator.constant.ArticleConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Pexels 图片搜索实现
 * <p>
 * 调用 Pexels REST API，需配置 API key。失败或无结果返回 null，由上层降级到 Picsum。
 */
@Slf4j
@Service
public class PexelsImageSearchService implements ImageSearchService {

    @Value("${article.pexels.api-key:}")
    private String apiKey;

    @Value("${article.pexels.per-page:1}")
    private int perPage;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String search(String keywords) {
        if (StrUtil.isBlank(apiKey)) {
            log.warn("Pexels API key 未配置，跳过 Pexels 搜索");
            return null;
        }
        try {
            String encodedQuery = URLEncoder.encode(keywords == null ? "" : keywords, StandardCharsets.UTF_8);
            String url = ArticleConstant.PEXELS_API_URL + "?query=" + encodedQuery + "&per_page=" + perPage;
            String body = HttpRequest.get(url)
                    .header("Authorization", apiKey)
                    .timeout(10_000)
                    .execute()
                    .body();
            JsonNode root = OBJECT_MAPPER.readTree(body);
            JsonNode photos = root.get("photos");
            if (photos != null && photos.isArray() && !photos.isEmpty()) {
                String src = photos.get(0).path("src").path("large").asText(null);
                if (StrUtil.isNotBlank(src)) {
                    return src;
                }
            }
            log.warn("Pexels 未返回图片结果: keywords={}", keywords);
        } catch (Exception e) {
            log.warn("Pexels 调用失败: keywords={}, error={}", keywords, e.getMessage());
        }
        return null;
    }
}

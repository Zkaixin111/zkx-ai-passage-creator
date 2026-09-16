package com.zkx.aipassagecreator.service.image.provider;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zkx.aipassagecreator.constant.ArticleConstant;
import com.zkx.aipassagecreator.model.enums.ImageMethodEnum;
import com.zkx.aipassagecreator.model.vo.ImageItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 生图配图策略（DashScope qwen-image-3.0-pro）
 * <p>
 * 调用 DashScope OpenAI 兼容文生图接口（同步）：直接发送 prompt 返回图片 URL。
 * 未配置 API key 或调用失败返回 null，由调用方降级。
 */
@Slf4j
@Component
public class AiImageProvider implements ImageProvider {

    /** 文生图模型（配置项 article.image.ai.model） */
    @Value("${article.image.ai.model:qwen-image-3.0-pro}")
    private String model;

    /** DashScope API key（复用 spring.ai.openai.api-key） */
    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public ImageMethodEnum method() {
        return ImageMethodEnum.NANO_BANANA;
    }

    @Override
    public ImageFetchResult generate(ImageItem requirement, String taskId, int index) {
        if (StrUtil.isBlank(apiKey)) {
            log.warn("DashScope API key 未配置，跳过 AI 生图");
            return null;
        }
        String prompt = buildPrompt(requirement);
        try {
            String imageUrl = generateImage(prompt);
            if (StrUtil.isBlank(imageUrl)) {
                return null;
            }
            byte[] bytes = HttpUtil.downloadBytes(imageUrl);
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            return new ImageFetchResult(null, bytes, ImageMethodEnum.NANO_BANANA.getValue(), "png");
        } catch (Exception e) {
            log.warn("AI 生图失败: prompt={}, error={}", StrUtil.maxLength(prompt, 100), e.getMessage());
            return null;
        }
    }

    private String buildPrompt(ImageItem requirement) {
        String base = StrUtil.blankToDefault(requirement.getDescription(), requirement.getKeywords());
        return base + (StrUtil.isBlank(requirement.getKeywords()) ? "" : "，关键词：" + requirement.getKeywords())
                + "，高清插画风格";
    }

    /**
     * 同步调用 DashScope OpenAI 兼容文生图接口，返回生成图片 URL；失败返回 null
     */
    private String generateImage(String prompt) throws Exception {
        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("model", model);
        body.put("prompt", StrUtil.maxLength(prompt, 1000));
        // OpenAI 协议用字母 x 作分隔符（非 DashScope 的星号 *）
        body.put("size", "1280x720");
        body.put("n", 1);

        String resp = HttpRequest.post(ArticleConstant.DASHSCOPE_IMAGE_GEN_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(60_000)
                .body(body.toString())
                .execute()
                .body();
        JsonNode root = OBJECT_MAPPER.readTree(resp);
        JsonNode data = root.path("data");
        if (data.isArray() && !data.isEmpty()) {
            return data.get(0).path("url").asText(null);
        }
        log.warn("DashScope 文生图失败: {}", root.path("error").path("message").asText("未知错误"));
        return null;
    }
}

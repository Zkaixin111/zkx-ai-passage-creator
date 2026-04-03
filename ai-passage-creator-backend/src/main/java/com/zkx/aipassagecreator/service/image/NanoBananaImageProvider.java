package com.zkx.aipassagecreator.service.image;

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

import java.util.HashMap;
import java.util.Map;

/**
 * AI 生图配图策略（NanoBanana → DashScope 通义万相文生图）
 * <p>
 * 调用 DashScope 文生图异步接口：提交任务 → 轮询任务状态 → 下载生成图片。
 * 未配置 API key 或调用失败返回 null，由调用方降级。
 */
@Slf4j
@Component
public class NanoBananaImageProvider implements ImageProvider {

    /** 文生图模型（配置项 article.image.nano-banana.model） */
    @Value("${article.image.nano-banana.model:wanx2.1-t2i-turbo}")
    private String model;

    /** DashScope API key（复用 spring.ai.openai.api-key） */
    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final int POLL_INTERVAL_MS = 2000;

    private static final int MAX_POLL_TIMES = 30;

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
            String taskId2 = submitTask(prompt);
            if (StrUtil.isBlank(taskId2)) {
                return null;
            }
            String imageUrl = pollTask(taskId2);
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
     * 提交文生图任务，返回 task_id；失败返回 null
     */
    private String submitTask(String prompt) throws Exception {
        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("model", model);
        body.putObject("input").put("prompt", StrUtil.maxLength(prompt, 1000));
        body.putObject("parameters").put("size", "1024*1024").put("n", 1);

        String resp = HttpRequest.post(ArticleConstant.DASHSCOPE_T2I_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(15_000)
                .body(body.toString())
                .execute()
                .body();
        JsonNode root = OBJECT_MAPPER.readTree(resp);
        JsonNode output = root.path("output");
        String taskId = output.path("task_id").asText(null);
        if (StrUtil.isBlank(taskId)) {
            log.warn("DashScope 文生图提交失败: {}", root.path("message").asText("未知错误"));
        }
        return taskId;
    }

    /**
     * 轮询任务直至成功，返回生成图片 URL；失败返回 null
     */
    private String pollTask(String taskId) throws Exception {
        String url = ArticleConstant.DASHSCOPE_TASK_URL.replace("{taskId}", taskId);
        for (int i = 0; i < MAX_POLL_TIMES; i++) {
            Thread.sleep(POLL_INTERVAL_MS);
            String resp = HttpRequest.get(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(10_000)
                    .execute()
                    .body();
            JsonNode root = OBJECT_MAPPER.readTree(resp);
            JsonNode output = root.path("output");
            String status = output.path("task_status").asText("");
            if ("SUCCEEDED".equals(status)) {
                JsonNode results = output.path("results");
                if (results.isArray() && !results.isEmpty()) {
                    return results.get(0).path("url").asText(null);
                }
                return null;
            }
            if ("FAILED".equals(status) || "CANCELED".equals(status)) {
                log.warn("DashScope 文生图任务失败: status={}, message={}",
                        status, output.path("message").asText(""));
                return null;
            }
        }
        log.warn("DashScope 文生图任务轮询超时: taskId={}", taskId);
        return null;
    }
}

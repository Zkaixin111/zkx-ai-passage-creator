package com.zkx.aipassagecreator.agent;

import com.zkx.aipassagecreator.common.JsonExtractUtil;
import com.zkx.aipassagecreator.config.SseEmitterManager;
import com.zkx.aipassagecreator.constant.ArticleConstant;
import com.zkx.aipassagecreator.constant.PromptConstant;
import com.zkx.aipassagecreator.model.context.ArticleTaskContext;
import com.zkx.aipassagecreator.model.enums.SSEMessageTypeEnum;
import com.zkx.aipassagecreator.model.vo.ImageItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体4：分析正文，确定配图位置与英文搜索关键词
 */
@Slf4j
@Component
public class ImageAnalyzeAgent {

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Resource
    private SseEmitterManager sseEmitterManager;

    public void execute(ArticleTaskContext ctx) {
        long start = System.currentTimeMillis();
        try {
            String content = ctx.getContent().toString();
            Map<String, String> vars = new HashMap<>();
            vars.put("content", content == null ? "" : content);
            String prompt = PromptConstant.render(PromptConstant.IMAGE_ANALYZE_PROMPT, vars);

            ChatClient chatClient = chatClientBuilder.build();
            String result = chatClient.prompt().user(prompt).call().content();
            List<ImageItem> requirements = JsonExtractUtil.parseList(result, ImageItem.class);
            if (requirements == null || requirements.isEmpty()) {
                // AI 未返回配图需求时，生成一条兜底需求
                requirements = new ArrayList<>();
                ImageItem fallback = new ImageItem();
                fallback.setPosition(1);
                fallback.setKeywords("technology");
                fallback.setSectionTitle("配图");
                fallback.setDescription("文章配图");
                requirements.add(fallback);
            }
            ctx.setImageRequirements(requirements);

            Map<String, Object> extra = new HashMap<>();
            extra.put(ArticleConstant.SSE_IMAGE_REQUIREMENTS, requirements);
            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.AGENT4_COMPLETE, extra);
        } catch (Exception e) {
            log.error("ImageAnalyzeAgent 执行失败: {}", e.getMessage(), e);
            ctx.setCancelled(true);
            sseEmitterManager.error(ctx.getTaskId(), "配图分析失败: " + e.getMessage());
        } finally {
            ctx.recordDuration("ImageAnalyzeAgent", System.currentTimeMillis() - start);
        }
    }
}

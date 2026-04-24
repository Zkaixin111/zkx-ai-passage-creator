package com.zkx.aipassagecreator.agent;

import com.zkx.aipassagecreator.annotation.AgentLog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkx.aipassagecreator.config.SseEmitterManager;
import com.zkx.aipassagecreator.constant.ArticleConstant;
import com.zkx.aipassagecreator.constant.PromptConstant;
import com.zkx.aipassagecreator.model.context.ArticleTaskContext;
import com.zkx.aipassagecreator.model.enums.SSEMessageTypeEnum;
import com.zkx.aipassagecreator.model.vo.OutlineItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体3：根据大纲撰写正文（流式输出）
 * <p>
 * 将大纲序列化为 JSON 填入 Prompt，流式片段通过 AGENT3_STREAMING 推送。
 */
@Slf4j
@Component
public class ContentAgent {

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Resource
    private SseEmitterManager sseEmitterManager;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @AgentLog
    public void execute(ArticleTaskContext ctx) {
        long start = System.currentTimeMillis();
        log.info("[{}] ContentAgent 开始执行", ctx.getTaskId());
        try {
            String outlineJson;
            try {
                outlineJson = OBJECT_MAPPER.writeValueAsString(ctx.getOutline());
            } catch (Exception e) {
                outlineJson = "[]";
            }
            Map<String, String> vars = new HashMap<>();
            vars.put("outline", outlineJson);
            String prompt = PromptConstant.render(PromptConstant.CONTENT_PROMPT, vars);

            StringBuilder content = new StringBuilder();
            chatClientBuilder.build().prompt().user(prompt).stream().content()
                    .doOnNext(chunk -> {
                        content.append(chunk);
                        Map<String, Object> extra = new HashMap<>();
                        extra.put(ArticleConstant.SSE_CONTENT, chunk);
                        sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.AGENT3_STREAMING, extra);
                    })
                    .blockLast();
            log.info("[{}] ContentAgent 流式完成, 正文字数={}", ctx.getTaskId(), content.length());

            ctx.getContent().append(content);
            ctx.recordDuration("ContentAgent", System.currentTimeMillis() - start);
            ctx.addLog("ContentAgent", "SUCCESS", prompt, outlineJson, content.toString(), null,
                    System.currentTimeMillis() - start);
            log.info("[{}] ContentAgent 完成", ctx.getTaskId());
            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.AGENT3_COMPLETE, null);
        } catch (Exception e) {
            log.error("ContentAgent 执行失败: {}", e.getMessage(), e);
            ctx.setCancelled(true);
            sseEmitterManager.error(ctx.getTaskId(), "正文生成失败: " + e.getMessage());
        } finally {
            ctx.recordDuration("ContentAgent", System.currentTimeMillis() - start);
        }
    }
}

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
 * 和 OutlineAgent 几乎一样的写法，区别：
 * 1. prompt 里填的是大纲 JSON + 字数约束
 * 2. 流式 chunk 通过 AGENT3_STREAMING 推送
 * 3. 流式结束后直接把完整正文存入 ctx.content（不需要 JSON 解析）
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
            // 1. 把大纲序列化为 JSON 字符串填入 prompt
            // 如 [{"section":1,"title":"引言","points":["AI的发展历程"]}]
            String outlineJson;
            try {
                outlineJson = OBJECT_MAPPER.writeValueAsString(ctx.getOutline());
            } catch (Exception e) {
                outlineJson = "[]";  // 序列化失败给空数组，AI 会自己生成大纲
            }

            // 2. 构造 prompt 变量：大纲 JSON + 目标字数
            Map<String, String> vars = new HashMap<>();
            vars.put("outline", outlineJson);
            vars.put("wordCount", String.valueOf(ctx.getWordCount()));  // 你加的字数功能
            String prompt = PromptConstant.render(PromptConstant.CONTENT_PROMPT, vars);

            // 3. 流式调用 AI（和 OutlineAgent 写法一样）
            // 每个 chunk 追加到 content + SSE 推送 AGENT3_STREAMING
            // blockLast() 阻塞等待所有 chunk 接收完毕
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

            // 4. 把完整正文存入 ctx.content（StringBuilder，append 一次）
            // 后续 MergeAgent 会从这里取正文做图文合成
            ctx.getContent().append(content);
            ctx.recordDuration("ContentAgent", System.currentTimeMillis() - start);
            ctx.addLog("ContentAgent", "SUCCESS", prompt, outlineJson, content.toString(), null,
                    System.currentTimeMillis() - start);
            log.info("[{}] ContentAgent 完成", ctx.getTaskId());

            // 5. 推送正文完成，前端切换到配图确认 UI
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

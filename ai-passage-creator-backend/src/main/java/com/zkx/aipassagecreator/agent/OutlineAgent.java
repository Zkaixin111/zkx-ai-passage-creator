package com.zkx.aipassagecreator.agent;

import com.zkx.aipassagecreator.annotation.AgentLog;

import com.zkx.aipassagecreator.common.JsonExtractUtil;
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
 * 智能体2：根据标题生成大纲（流式输出）
 * <p>
 * 流式片段通过 AGENT2_STREAMING 推送，完成时解析为结构化大纲并推送 OUTLINE_GENERATED。
 */
@Slf4j
@Component
public class OutlineAgent {

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Resource
    private SseEmitterManager sseEmitterManager;

    @AgentLog
    public void execute(ArticleTaskContext ctx) {
        long start = System.currentTimeMillis();
        log.info("[{}] OutlineAgent 开始执行, mainTitle={}", ctx.getTaskId(), ctx.getSelectedMainTitle());
        try {
            Map<String, String> vars = new HashMap<>();
            vars.put("mainTitle", ctx.getSelectedMainTitle() == null ? "" : ctx.getSelectedMainTitle());
            vars.put("subTitle", ctx.getSelectedSubTitle() == null ? "" : ctx.getSelectedSubTitle());
            vars.put("userDescription", ctx.getUserDescription() == null ? "" : ctx.getUserDescription());
            String prompt = PromptConstant.render(PromptConstant.OUTLINE_PROMPT, vars);

            StringBuilder raw = new StringBuilder();
            chatClientBuilder.build().prompt().user(prompt).stream().content()
                    .doOnNext(chunk -> {
                        raw.append(chunk);
                        Map<String, Object> extra = new HashMap<>();
                        extra.put(ArticleConstant.SSE_CONTENT, chunk);
                        sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.AGENT2_STREAMING, extra);
                    })
                    .blockLast();
            log.info("[{}] OutlineAgent 流式完成, 原始长度={}", ctx.getTaskId(), raw.length());

            List<OutlineItem> outline = JsonExtractUtil.parseList(raw.toString(), OutlineItem.class);
            ctx.setOutline(outline);
            ctx.recordDuration("OutlineAgent", System.currentTimeMillis() - start);
            ctx.addLog("OutlineAgent", "SUCCESS", prompt, ctx.getSelectedMainTitle(), raw.toString(), null,
                    System.currentTimeMillis() - start);
            log.info("[{}] OutlineAgent 完成, 大纲章节 {} 个", ctx.getTaskId(), outline.size());

            Map<String, Object> extra = new HashMap<>();
            extra.put(ArticleConstant.SSE_OUTLINE, outline);
            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.OUTLINE_GENERATED, extra);
            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.AGENT2_COMPLETE, null);
        } catch (Exception e) {
            log.error("OutlineAgent 执行失败: {}", e.getMessage(), e);
            ctx.setCancelled(true);
            sseEmitterManager.error(ctx.getTaskId(), "大纲生成失败: " + e.getMessage());
        } finally {
            ctx.recordDuration("OutlineAgent", System.currentTimeMillis() - start);
        }
    }
}

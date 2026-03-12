package com.zkx.aipassagecreator.agent;

import com.zkx.aipassagecreator.common.JsonExtractUtil;
import com.zkx.aipassagecreator.config.SseEmitterManager;
import com.zkx.aipassagecreator.constant.ArticleConstant;
import com.zkx.aipassagecreator.constant.PromptConstant;
import com.zkx.aipassagecreator.model.context.ArticleTaskContext;
import com.zkx.aipassagecreator.model.enums.SSEMessageTypeEnum;
import com.zkx.aipassagecreator.model.vo.TitleOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体1：根据选题生成爆款标题方案
 */
@Slf4j
@Component
public class TitleAgent {

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Resource
    private SseEmitterManager sseEmitterManager;

    public void execute(ArticleTaskContext ctx) {
        long start = System.currentTimeMillis();
        log.info("[{}] TitleAgent 开始执行, topic={}", ctx.getTaskId(), ctx.getTopic());
        try {
            Map<String, String> vars = new HashMap<>();
            vars.put("topic", ctx.getTopic() == null ? "" : ctx.getTopic());
            vars.put("style", ctx.getStyle() == null ? "默认" : ctx.getStyle());
            String prompt = PromptConstant.render(PromptConstant.TITLE_PROMPT, vars);

            ChatClient chatClient = chatClientBuilder.build();
            String result = chatClient.prompt().user(prompt).call().content();
            log.info("[{}] TitleAgent AI 返回: {}", ctx.getTaskId(), result == null ? "null" : result.substring(0, Math.min(result.length(), 200)));
            List<TitleOption> titleOptions = JsonExtractUtil.parseList(result, TitleOption.class);
            ctx.setTitleOptions(titleOptions);
            ctx.recordDuration("TitleAgent", System.currentTimeMillis() - start);
            ctx.addLog("TitleAgent", "SUCCESS", prompt, ctx.getTopic(), result, null, System.currentTimeMillis() - start);
            log.info("[{}] TitleAgent 完成, 生成标题方案 {} 个", ctx.getTaskId(), titleOptions.size());

            // 推送：智能体1完成 → 标题方案就绪
            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.AGENT1_COMPLETE, null);
            Map<String, Object> extra = new HashMap<>();
            extra.put(ArticleConstant.SSE_TITLE_OPTIONS, titleOptions);
            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.TITLES_GENERATED, extra);
        } catch (Exception e) {
            log.error("TitleAgent 执行失败: {}", e.getMessage(), e);
            ctx.setCancelled(true);
            sseEmitterManager.error(ctx.getTaskId(), "标题生成失败: " + e.getMessage());
        } finally {
            ctx.recordDuration("TitleAgent", System.currentTimeMillis() - start);
        }
    }
}

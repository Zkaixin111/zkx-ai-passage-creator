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
 * 智能体2：根据用户选的标题生成大纲（流式输出）
 * <p>
 * 和 TitleAgent 的区别：这里用 stream() 而非 call()，AI 返回时逐字推送 SSE。
 * <p>
 * 执行流程：
 * 1. 从 ctx 取出用户选的主标题/副标题/补充描述，填入 prompt
 * 2. 调用 AI（流式），每个 chunk 通过 AGENT2_STREAMING SSE 推送给前端
 * 3. 流式结束后，把完整文本解析为 OutlineItem 列表
 * 4. 存入 ctx.outline，推送 OUTLINE_GENERATED（前端切换到大纲编辑 UI）
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
            // 1. 构造 prompt 变量：主标题 + 副标题 + 用户补充描述
            Map<String, String> vars = new HashMap<>();
            vars.put("mainTitle", ctx.getSelectedMainTitle() == null ? "" : ctx.getSelectedMainTitle());
            vars.put("subTitle", ctx.getSelectedSubTitle() == null ? "" : ctx.getSelectedSubTitle());
            vars.put("userDescription", ctx.getUserDescription() == null ? "" : ctx.getUserDescription());
            String prompt = PromptConstant.render(PromptConstant.OUTLINE_PROMPT, vars);

            // 2. 流式调用 AI
            // stream().content() 返回 Reactor Flux<String>，每来一个 chunk 触发 doOnNext
            // doOnNext：把 chunk 追加到 raw + SSE 推送给前端（前端实时显示打字效果）
            // blockLast()：阻塞直到所有 chunk 接收完毕（流式同步等待）
            StringBuilder raw = new StringBuilder();
            chatClientBuilder.build().prompt().user(prompt).stream().content()
                    .doOnNext(chunk -> {
                        raw.append(chunk);  // 累积完整文本，后续解析用
                        ctx.getOutlineRaw().append(chunk);  // 同步累积到上下文，SSE 重连时补发已流式部分
                        Map<String, Object> extra = new HashMap<>();
                        extra.put(ArticleConstant.SSE_CONTENT, chunk);  // 前端用这个字段做打字动画
                        sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.AGENT2_STREAMING, extra);
                    })
                    .blockLast();
            log.info("[{}] OutlineAgent 流式完成, 原始长度={}", ctx.getTaskId(), raw.length());

            // 3. 流式结束后，把完整文本解析为结构化大纲
            // AI 返回的是 JSON 数组，如 [{"section":1,"title":"引言","points":["..."]}]
            List<OutlineItem> outline = JsonExtractUtil.parseList(raw.toString(), OutlineItem.class);
            ctx.setOutline(outline);
            ctx.recordDuration("OutlineAgent", System.currentTimeMillis() - start);
            ctx.addLog("OutlineAgent", "SUCCESS", prompt, ctx.getSelectedMainTitle(), raw.toString(), null,
                    System.currentTimeMillis() - start);
            log.info("[{}] OutlineAgent 完成, 大纲章节 {} 个", ctx.getTaskId(), outline.size());

            // 4. 推送完整大纲（前端用这个切换到大纲编辑 UI，而不是用流式累积的碎片）
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

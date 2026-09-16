package com.zkx.aipassagecreator.agent;

import com.zkx.aipassagecreator.annotation.AgentLog;

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
 * 智能体1：根据选题 + 风格生成 5 个爆款标题方案
 * <p>
 * 执行流程：
 * 1. 从 ctx 取出选题(topic)和风格(style)，填入 prompt 模板的占位符
 * 2. 调用 AI（同步，非流式），等待返回
 * 3. 从 AI 返回的文本中提取 JSON，解析为 TitleOption 列表
 * 4. 存入 ctx.titleOptions，推送 SSE 通知前端"标题方案已就绪"
 * <p>
 * 异常处理：失败时设 ctx.cancelled=true + SSE 推送错误，
 * ArticleTaskServiceImpl 的异步块检查 cancelled 后跳过后续逻辑。
 */
@Slf4j
@Component
public class TitleAgent {

    /** Spring AI 的 ChatClient 构建器，用于调用大模型 */
    @Resource
    private ChatClient.Builder chatClientBuilder;

    /** SSE 推送管理器，通过 taskId 推送消息给前端 */
    @Resource
    private SseEmitterManager sseEmitterManager;

    /**
     * 执行标题生成
     * @AgentLog 注解会被 AOP 切面拦截，自动记录执行耗时和输入输出到 ctx.logs
     */
    @AgentLog
    public void execute(ArticleTaskContext ctx) {
        long start = System.currentTimeMillis();
        log.info("[{}] TitleAgent 开始执行, topic={}", ctx.getTaskId(), ctx.getTopic());
        try {
            // 1. 构造 prompt 变量：选题 + 风格
            Map<String, String> vars = new HashMap<>();
            vars.put("topic", ctx.getTopic() == null ? "" : ctx.getTopic());
            vars.put("style", ctx.getStyle() == null ? "默认" : ctx.getStyle());
            // PromptConstant.render 把 {topic}/{style} 占位符替换为实际值
            String prompt = PromptConstant.render(PromptConstant.TITLE_PROMPT, vars);

            // 2. 同步调用 AI（call() 阻塞等待完整响应，不流式）
            ChatClient chatClient = chatClientBuilder.build();
            String result = chatClient.prompt().user(prompt).call().content();
            log.info("[{}] TitleAgent AI 返回: {}", ctx.getTaskId(), result == null ? "null" : result.substring(0, Math.min(result.length(), 200)));

            // 3. 从 AI 返回文本中提取 JSON 列表，解析为 TitleOption 对象
            // AI 可能返回 ```json ... ``` 包裹的内容，JsonExtractUtil 会自动提取
            List<TitleOption> titleOptions = JsonExtractUtil.parseList(result, TitleOption.class);

            // 4. 存入 ctx（后续 ArticleTaskServiceImpl 会落库 titleOptions 字段）
            ctx.setTitleOptions(titleOptions);
            ctx.recordDuration("TitleAgent", System.currentTimeMillis() - start);
            ctx.addLog("TitleAgent", "SUCCESS", prompt, ctx.getTopic(), result, null, System.currentTimeMillis() - start);
            log.info("[{}] TitleAgent 完成, 生成标题方案 {} 个", ctx.getTaskId(), titleOptions.size());

            // 5. 推送 SSE：先推"智能体1完成"（前端切到选标题 UI），再推标题方案数据
            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.AGENT1_COMPLETE, null);
            Map<String, Object> extra = new HashMap<>();
            extra.put(ArticleConstant.SSE_TITLE_OPTIONS, titleOptions);
            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.TITLES_GENERATED, extra);
        } catch (Exception e) {
            // 失败处理：标记取消 + 推送错误 + SSE 关闭
            log.error("TitleAgent 执行失败: {}", e.getMessage(), e);
            ctx.setCancelled(true);
            sseEmitterManager.error(ctx.getTaskId(), "标题生成失败: " + e.getMessage());
        } finally {
            ctx.recordDuration("TitleAgent", System.currentTimeMillis() - start);
        }
    }
}

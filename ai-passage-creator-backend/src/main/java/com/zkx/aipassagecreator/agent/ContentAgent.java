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
 * 智能体3：根据大纲撰写正文（分段流式输出）
 * <p>
 * 为什么分段生成而不是整篇一次性生成？
 * 1. 体感反馈：整篇 1200-2000 字需要 30-60 秒，用户只能看着光标干等；
 *    分段后每段 5-10 秒，段落开始/完成都会推送事件，前端可按段落展示进度。
 * 2. 断点恢复：每段内容实时写入 ctx.sectionContents，
 *    SSE 断线重连时通过 SNAPSHOT 把已生成段落一次性补发给前端。
 * <p>
 * 执行流程：
 * 1. 遍历 ctx.outline，逐章调用 AI（每次只写一章）
 * 2. 每章开始推 CONTENT_SECTION_START，章内 chunk 推 AGENT3_STREAMING（带 sectionIndex）
 * 3. 每章完成推 CONTENT_SECTION_COMPLETE，并把该章内容追加到 ctx.sectionContents
 * 4. 全部章节完成后拼接为完整正文存入 ctx.content，推 AGENT3_COMPLETE
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
        log.info("[{}] ContentAgent 开始执行（分段模式）", ctx.getTaskId());
        try {
            List<OutlineItem> outline = ctx.getOutline();
            if (outline == null || outline.isEmpty()) {
                throw new IllegalStateException("大纲为空，无法分段生成正文");
            }
            int total = outline.size();
            ctx.setTotalSections(total);
            ctx.setCompletedSections(0);
            ctx.setCurrentSectionIndex(-1);

            // 把大纲序列化为 JSON 字符串填入 prompt（让 AI 知道全文结构，保持章节连贯）
            String outlineJson;
            try {
                outlineJson = OBJECT_MAPPER.writeValueAsString(outline);
            } catch (Exception e) {
                outlineJson = "[]";
            }

            // 字数均摊到每章（首尾章的引言/总结通过 positionHint 补充要求）
            int sectionWordCount = Math.max(200, ctx.getWordCount() / total);

            StringBuilder fullContent = new StringBuilder();
            for (int i = 0; i < total; i++) {
                if (ctx.isCancelled()) {
                    return;
                }
                OutlineItem item = outline.get(i);
                ctx.setCurrentSectionIndex(i);
                // 段开始时先占位一个空 StringBuilder，段内流式 chunk 实时追加
                // （SSE 重连时 SNAPSHOT 能把"当前段已流式部分"一并补发给前端）
                StringBuilder sectionBuf = new StringBuilder();
                ctx.getSectionContents().add(sectionBuf);

                // 1. 推送段落开始事件：前端据此高亮当前段、展示"第 X/Y 段"
                Map<String, Object> startExtra = new HashMap<>();
                startExtra.put(ArticleConstant.SSE_SECTION_INDEX, i);
                startExtra.put(ArticleConstant.SSE_TOTAL_SECTIONS, total);
                startExtra.put(ArticleConstant.SSE_SECTION_TITLE, item.getTitle());
                sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.CONTENT_SECTION_START, startExtra);

                // 2. 构造本章 prompt
                Map<String, String> vars = new HashMap<>();
                vars.put("topic", ctx.getTopic() == null ? "" : ctx.getTopic());
                vars.put("style", ctx.getStyle() == null ? "默认" : ctx.getStyle());
                vars.put("mainTitle", ctx.getSelectedMainTitle() == null ? "" : ctx.getSelectedMainTitle());
                vars.put("subTitle", ctx.getSelectedSubTitle() == null ? "" : ctx.getSelectedSubTitle());
                vars.put("outline", outlineJson);
                vars.put("sectionIndex", String.valueOf(i + 1));
                vars.put("totalSections", String.valueOf(total));
                vars.put("sectionTitle", item.getTitle() == null ? "" : item.getTitle());
                vars.put("sectionPoints", item.getPoints() == null ? "" : String.join("；", item.getPoints()));
                vars.put("sectionWordCount", String.valueOf(sectionWordCount));
                vars.put("positionHint", positionHint(i, total));
                String prompt = PromptConstant.render(PromptConstant.CONTENT_SECTION_PROMPT, vars);

                // 3. 流式调用 AI：chunk 同时写本地缓冲（段落完整性）和 ctx（断点恢复）
                final int sectionIndex = i;
                chatClientBuilder.build().prompt().user(prompt).stream().content()
                        .doOnNext(chunk -> {
                            sectionBuf.append(chunk);
                            Map<String, Object> extra = new HashMap<>();
                            extra.put(ArticleConstant.SSE_CONTENT, chunk);
                            extra.put(ArticleConstant.SSE_SECTION_INDEX, sectionIndex);
                            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.AGENT3_STREAMING, extra);
                        })
                        .blockLast();

                // 4. 段落收尾：兜底补章节标题（AI 偶尔不会严格以 "## 标题" 开头）
                String sectionText = sectionBuf.toString().trim();
                if (item.getTitle() != null && !sectionText.startsWith("##")) {
                    sectionText = "## " + item.getTitle() + "\n\n" + sectionText;
                    // 同步修正 ctx 中的缓冲，保证 SNAPSHOT 与最终正文一致
                    sectionBuf.setLength(0);
                    sectionBuf.append(sectionText);
                }
                fullContent.append(sectionText).append("\n\n");
                ctx.setCompletedSections(i + 1);
                log.info("[{}] ContentAgent 第 {}/{} 段完成, 字数={}", ctx.getTaskId(), i + 1, total, sectionText.length());

                // 5. 推送段落完成事件
                Map<String, Object> doneExtra = new HashMap<>();
                doneExtra.put(ArticleConstant.SSE_SECTION_INDEX, i);
                doneExtra.put(ArticleConstant.SSE_TOTAL_SECTIONS, total);
                sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.CONTENT_SECTION_COMPLETE, doneExtra);
            }

            // 6. 拼接完整正文存入 ctx.content（后续 MergeAgent 从这里取正文做图文合成）
            ctx.getContent().append(fullContent.toString().trim());
            ctx.recordDuration("ContentAgent", System.currentTimeMillis() - start);
            ctx.addLog("ContentAgent", "SUCCESS", "分段生成(" + total + "段)", outlineJson,
                    ctx.getContent().toString(), null, System.currentTimeMillis() - start);
            log.info("[{}] ContentAgent 完成, 总字数={}", ctx.getTaskId(), ctx.getContent().length());

            // 7. 推送正文完成，前端切换到配图确认 UI
            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.AGENT3_COMPLETE, null);
        } catch (Exception e) {
            log.error("ContentAgent 执行失败: {}", e.getMessage(), e);
            ctx.setCancelled(true);
            sseEmitterManager.error(ctx.getTaskId(), "正文生成失败: " + e.getMessage());
        } finally {
            ctx.recordDuration("ContentAgent", System.currentTimeMillis() - start);
        }
    }

    /**
     * 首尾章节的特殊写作要求：第一章带引言，最后一章带总结升华，中间章节返回空串。
     */
    private String positionHint(int index, int total) {
        if (index == 0) {
            return "4.1 作为开篇章节，正文开头先写一段吸引人的引言，再进入本章要点";
        }
        if (index == total - 1 && total > 1) {
            return "4.1 作为结尾章节，正文末尾给出全文总结或升华";
        }
        return "";
    }
}

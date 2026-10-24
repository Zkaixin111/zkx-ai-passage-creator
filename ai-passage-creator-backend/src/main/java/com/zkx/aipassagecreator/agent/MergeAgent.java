package com.zkx.aipassagecreator.agent;

import com.zkx.aipassagecreator.annotation.AgentLog;

import com.zkx.aipassagecreator.config.SseEmitterManager;
import com.zkx.aipassagecreator.constant.ArticleConstant;
import com.zkx.aipassagecreator.model.context.ArticleTaskContext;
import com.zkx.aipassagecreator.model.enums.SSEMessageTypeEnum;
import com.zkx.aipassagecreator.model.vo.ImageItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体6：图文合成 —— DAG 流水线最后一个节点
 * <p>
 * 职责：把正文（Markdown）和配图列表合并为完整图文 Markdown。
 * <p>
 * 合成规则：按配图的 position 字段，找到正文中第 position 个 "## " 章节标题，
 * 在该章节末尾（下一个章节标题之前）插入图片 Markdown 语法。
 * <p>
 * 完成后推送 MERGE_COMPLETE（携带完整图文）。
 * <p>
 * 注意：ALL_COMPLETE 不在这里推送。文章落库（persistCompleted）发生在编排服务里，
 * 若在落库前就通知前端"完成"，用户立刻刷新会看到数据库仍是 PROCESSING 的文章。
 * 因此由 ArticleTaskServiceImpl 在落库成功后统一推送 ALL_COMPLETE 并关闭 SSE。
 */
@Slf4j
@Component
public class MergeAgent {

    @Resource
    private SseEmitterManager sseEmitterManager;

    @AgentLog
    public void execute(ArticleTaskContext ctx) {
        long start = System.currentTimeMillis();
        log.info("[{}] MergeAgent 进入，正文字数={}，配图数={}",
                ctx.getTaskId(),
                ctx.getContent() == null ? -1 : ctx.getContent().length(),
                ctx.getImages() == null ? -1 : ctx.getImages().size());
        try {
            // 1. 从 ctx 取正文和配图列表
            String content = ctx.getContent().toString();
            // 2. 合成：把配图按 position 插入正文对应章节位置
            String fullContent = merge(content, ctx.getImages());
            // 3. 存入 ctx.fullContent（后续 ArticleTaskServiceImpl.persistCompleted 落库）
            ctx.setFullContent(fullContent);

            // 4. 推送图文合成结果给前端（ALL_COMPLETE 由编排服务在落库后推送）
            Map<String, Object> extra = new HashMap<>();
            extra.put(ArticleConstant.SSE_FULL_CONTENT, fullContent);
            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.MERGE_COMPLETE, extra);
        } catch (Exception e) {
            log.error("MergeAgent 执行失败: {}", e.getMessage(), e);
            ctx.setCancelled(true);
            sseEmitterManager.error(ctx.getTaskId(), "图文合成失败: " + e.getMessage());
        } finally {
            ctx.recordDuration("MergeAgent", System.currentTimeMillis() - start);
        }
    }

    /**
     * 将配图按 position 升序插入正文
     * <p>
     * position=1 → 插入到第 1 个 "## " 章节之后
     * position=2 → 插入到第 2 个 "## " 章节之后
     * position=null 或超出章节数 → 追加到正文末尾
     */
    private String merge(String content, List<ImageItem> images) {
        if (images == null || images.isEmpty()) {
            return content;  // 没有配图，直接返回原正文
        }
        String result = content;
        // 按 position 升序排序（null 排最后）
        List<ImageItem> sorted = images.stream()
                .sorted(Comparator.comparingInt(img -> img.getPosition() == null ? Integer.MAX_VALUE : img.getPosition()))
                .toList();
        for (ImageItem img : sorted) {
            // 构造 Markdown 图片语法：![描述](URL)
            String markdown = String.format("\n\n![%s](%s)\n\n",
                    img.getDescription() == null ? "配图" : img.getDescription(),
                    img.getUrl() == null ? "" : img.getUrl());
            int sectionNo = img.getPosition() == null ? -1 : img.getPosition();
            result = insertAfterSection(result, markdown, sectionNo);
        }
        return result;
    }

    /**
     * 在第 sectionNo 个 "## " 章节标题段落之后插入内容
     * <p>
     * 实现：先收集所有以 "## " 开头的行首下标，再取第 sectionNo 个章节的结束位置
     * （即下一个 "## " 标题的行首，若已是最后一章则取文末）。
     * <p>
     * 为什么不能直接用 indexOf("\n## ") 计数：正文的第一章标题位于字符串开头（前面没有换行），
     * 这样会导致第 1 章被漏数，position=1 的配图被插到第 2 章末尾，后续全部错位。
     */
    private String insertAfterSection(String content, String markdown, int sectionNo) {
        if (sectionNo <= 0) {
            return content + markdown;  // position<=0 直接追加到末尾
        }
        // 收集所有二级标题的行首下标（包含正文第一行就是 "## " 的情况）
        List<Integer> headingPositions = new ArrayList<>();
        int lineStart = 0;
        while (lineStart < content.length()) {
            int lineEnd = content.indexOf('\n', lineStart);
            if (content.startsWith("## ", lineStart)) {
                headingPositions.add(lineStart);
            }
            if (lineEnd == -1) {
                break;
            }
            lineStart = lineEnd + 1;
        }
        // 找不到第 sectionNo 个章节，追加到末尾
        if (headingPositions.size() < sectionNo) {
            return content + markdown;
        }
        // 第 sectionNo 个章节的结束位置 = 下一章节的行首；最后一章则到文末
        int insertAt = sectionNo < headingPositions.size()
                ? headingPositions.get(sectionNo)
                : content.length();
        return content.substring(0, insertAt) + markdown + content.substring(insertAt);
    }
}

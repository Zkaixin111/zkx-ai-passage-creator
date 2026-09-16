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
 * 完成后推送 3 条 SSE：
 * 1. MERGE_COMPLETE(fullContent) —— 推送完整图文内容
 * 2. ALL_COMPLETE —— 通知前端"全部完成"
 * 3. sseEmitterManager.complete() —— 关闭 SSE 连接
 */
@Slf4j
@Component
public class MergeAgent {

    @Resource
    private SseEmitterManager sseEmitterManager;

    @AgentLog
    public void execute(ArticleTaskContext ctx) {
        long start = System.currentTimeMillis();
        try {
            // 1. 从 ctx 取正文和配图列表
            String content = ctx.getContent().toString();
            // 2. 合成：把配图按 position 插入正文对应章节位置
            String fullContent = merge(content, ctx.getImages());
            // 3. 存入 ctx.fullContent（后续 ArticleTaskServiceImpl.persistCompleted 落库）
            ctx.setFullContent(fullContent);

            // 4. 推送最终结果给前端
            Map<String, Object> extra = new HashMap<>();
            extra.put(ArticleConstant.SSE_FULL_CONTENT, fullContent);
            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.MERGE_COMPLETE, extra);
            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.ALL_COMPLETE, null);
            // 5. 全部完成，关闭 SSE 连接
            sseEmitterManager.complete(ctx.getTaskId());
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
     * 实现：在正文中找第 N 个 "\n## " 的位置，在该位置到下一个章节标题之间插入图片。
     * 如果找不到对应章节，追加到末尾。
     */
    private String insertAfterSection(String content, String markdown, int sectionNo) {
        if (sectionNo <= 0) {
            return content + markdown;  // position<=0 直接追加到末尾
        }
        // 数 "\n## " 出现的次数，找到第 sectionNo 个的位置
        int count = 0;
        int idx = -1;
        while ((idx = content.indexOf("\n## ", idx + 1)) != -1) {
            count++;
            if (count == sectionNo) {
                break;
            }
        }
        if (idx == -1) {
            // 没找到第 sectionNo 个章节，追加到末尾
            return content + markdown;
        }
        // 找下一个章节标题的位置（"## " 或 "# "），图片插入到这两个标题之间
        int next = content.indexOf("\n## ", idx + 1);
        if (next == -1) {
            next = content.indexOf("\n# ", idx + 1);  // 也检查一级标题
        }
        if (next == -1) {
            next = content.length();  // 没有下一个章节，插到正文末尾
        }
        // 在 next 位置前插入图片 Markdown
        return content.substring(0, next) + markdown + content.substring(next);
    }
}

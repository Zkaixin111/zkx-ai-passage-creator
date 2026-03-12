package com.zkx.aipassagecreator.agent;

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
 * 智能体6：图文合成
 * <p>
 * 将配图按 position 插入正文对应章节，生成完整图文 Markdown。
 * 完成后推送 MERGE_COMPLETE(fullContent) → ALL_COMPLETE，并关闭 SSE。
 */
@Slf4j
@Component
public class MergeAgent {

    @Resource
    private SseEmitterManager sseEmitterManager;

    public void execute(ArticleTaskContext ctx) {
        long start = System.currentTimeMillis();
        try {
            String content = ctx.getContent().toString();
            String fullContent = merge(content, ctx.getImages());
            ctx.setFullContent(fullContent);

            Map<String, Object> extra = new HashMap<>();
            extra.put(ArticleConstant.SSE_FULL_CONTENT, fullContent);
            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.MERGE_COMPLETE, extra);
            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.ALL_COMPLETE, null);
            // 全部完成，关闭连接
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
     * 将配图插入正文对应章节位置
     * <p>
     * 按 position 升序，从后往前插入避免位置偏移；position 超出章节数则追加到末尾。
     */
    private String merge(String content, List<ImageItem> images) {
        if (images == null || images.isEmpty()) {
            return content;
        }
        String result = content;
        List<ImageItem> sorted = images.stream()
                .sorted(Comparator.comparingInt(img -> img.getPosition() == null ? Integer.MAX_VALUE : img.getPosition()))
                .toList();
        for (ImageItem img : sorted) {
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
     */
    private String insertAfterSection(String content, String markdown, int sectionNo) {
        if (sectionNo <= 0) {
            return content + markdown;
        }
        int count = 0;
        int idx = -1;
        while ((idx = content.indexOf("\n## ", idx + 1)) != -1) {
            count++;
            if (count == sectionNo) {
                break;
            }
        }
        if (idx == -1) {
            // 未找到对应章节，追加到末尾
            return content + markdown;
        }
        int next = content.indexOf("\n## ", idx + 1);
        if (next == -1) {
            next = content.indexOf("\n# ", idx + 1);
        }
        if (next == -1) {
            next = content.length();
        }
        return content.substring(0, next) + markdown + content.substring(next);
    }
}

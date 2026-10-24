package com.zkx.aipassagecreator.agent;

import com.zkx.aipassagecreator.annotation.AgentLog;

import com.zkx.aipassagecreator.common.JsonExtractUtil;
import com.zkx.aipassagecreator.config.SseEmitterManager;
import com.zkx.aipassagecreator.constant.ArticleConstant;
import com.zkx.aipassagecreator.constant.PromptConstant;
import com.zkx.aipassagecreator.model.context.ArticleTaskContext;
import com.zkx.aipassagecreator.model.enums.SSEMessageTypeEnum;
import com.zkx.aipassagecreator.model.vo.ImageItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体4：分析正文，确定每段需要什么配图
 * <p>
 * 同步调用 AI（非流式），返回 ImageItem 列表，每个包含：
 * - position：配图插入位置（第几个章节后）
 * - keywords：英文搜索关键词（给 Pexels/AI 生图用）
 * - sectionTitle：章节标题
 * - description：图片描述
 * <p>
 * 如果 AI 未返回配图需求，生成一条兜底需求（position=1, keywords="technology"）
 */
@Slf4j
@Component
public class ImageAnalyzeAgent {

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Resource
    private SseEmitterManager sseEmitterManager;

    @AgentLog
    public void execute(ArticleTaskContext ctx) {
        long start = System.currentTimeMillis();
        try {
            // 1. 从 ctx 取出正文内容填入 prompt
            String content = ctx.getContent().toString();
            Map<String, String> vars = new HashMap<>();
            vars.put("content", content == null ? "" : content);
            String prompt = PromptConstant.render(PromptConstant.IMAGE_ANALYZE_PROMPT, vars);

            // 2. 同步调用 AI，让它分析正文每段需要什么图
            ChatClient chatClient = chatClientBuilder.build();
            String result = chatClient.prompt().user(prompt).call().content();

            // 3. 解析 AI 返回的 JSON 为 ImageItem 列表
            List<ImageItem> requirements = JsonExtractUtil.parseList(result, ImageItem.class);
            if (requirements == null || requirements.isEmpty()) {
                // 兜底：AI 没分析出配图需求时，给一条默认需求
                requirements = new ArrayList<>();
                ImageItem fallback = new ImageItem();
                fallback.setPosition(1);
                fallback.setKeywords("technology");
                fallback.setSectionTitle("配图");
                fallback.setDescription("文章配图");
                requirements.add(fallback);
            }

            // 4. 数量上限保护：AI 可能返回过多配图需求，
            // 超过上限会导致并行拉图与 AI 生图费用失控，这里直接截断
            int maxImages = ArticleConstant.DEFAULT_IMAGE_COUNT;
            if (requirements.size() > maxImages) {
                log.info("配图需求 {} 条超过上限 {}，截断处理", requirements.size(), maxImages);
                requirements = new ArrayList<>(requirements.subList(0, maxImages));
            }

            // 5. 存入 ctx.imageRequirements（智能体5 ImageFetchAgent 会取这个列表并行获取配图）
            ctx.setImageRequirements(requirements);

            // 6. SSE 推送配图需求（前端展示配图进度）
            Map<String, Object> extra = new HashMap<>();
            extra.put(ArticleConstant.SSE_IMAGE_REQUIREMENTS, requirements);
            sseEmitterManager.send(ctx.getTaskId(), SSEMessageTypeEnum.AGENT4_COMPLETE, extra);
        } catch (Exception e) {
            log.error("ImageAnalyzeAgent 执行失败: {}", e.getMessage(), e);
            ctx.setCancelled(true);
            sseEmitterManager.error(ctx.getTaskId(), "配图分析失败: " + e.getMessage());
        } finally {
            ctx.recordDuration("ImageAnalyzeAgent", System.currentTimeMillis() - start);
        }
    }
}

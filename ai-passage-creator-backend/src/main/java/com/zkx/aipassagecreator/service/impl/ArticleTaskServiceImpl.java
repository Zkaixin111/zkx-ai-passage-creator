package com.zkx.aipassagecreator.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.zkx.aipassagecreator.common.JsonExtractUtil;
import com.zkx.aipassagecreator.config.ArticleAgentGraph;
import com.zkx.aipassagecreator.config.SseEmitterManager;
import com.zkx.aipassagecreator.constant.ArticleConstant;
import com.zkx.aipassagecreator.constant.PromptConstant;
import com.zkx.aipassagecreator.exception.BusinessException;
import com.zkx.aipassagecreator.exception.ErrorCode;
import com.zkx.aipassagecreator.mapper.ArticleMapper;
import com.zkx.aipassagecreator.mapper.UserMapper;
import com.zkx.aipassagecreator.model.context.ArticleTaskContext;
import com.zkx.aipassagecreator.model.dto.ArticleAiModifyOutlineRequest;
import com.zkx.aipassagecreator.model.dto.ArticleConfirmContentRequest;
import com.zkx.aipassagecreator.model.dto.ArticleConfirmOutlineRequest;
import com.zkx.aipassagecreator.model.dto.ArticleConfirmTitleRequest;
import com.zkx.aipassagecreator.model.dto.ArticleCreateRequest;
import com.zkx.aipassagecreator.model.entity.Article;
import com.zkx.aipassagecreator.model.entity.User;
import com.zkx.aipassagecreator.model.enums.ArticlePhaseEnum;
import com.zkx.aipassagecreator.model.enums.ArticleStatusEnum;
import com.zkx.aipassagecreator.model.enums.ImageMethodEnum;
import com.zkx.aipassagecreator.model.vo.AgentExecutionStats;
import com.zkx.aipassagecreator.model.vo.OutlineItem;
import com.zkx.aipassagecreator.model.vo.OutlineSection;
import com.zkx.aipassagecreator.service.ArticleTaskService;
import com.zkx.aipassagecreator.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 文章任务编排服务实现
 * <p>
 * 三层架构中的「异步执行层」：提交线程池执行 StateGraph 智能体图，接口快速返回。
 * 内存维护 taskId → ArticleTaskContext，任务完成后清理。
 */
@Slf4j
@Service
public class ArticleTaskServiceImpl implements ArticleTaskService {

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    @Qualifier("articleTaskExecutor")
    private Executor taskExecutor;

    @Resource
    private ArticleAgentGraph articleAgentGraph;

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Resource
    private SseEmitterManager sseEmitterManager;

    @Resource
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    private final Map<String, ArticleTaskContext> contextMap = new ConcurrentHashMap<>();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String createTask(ArticleCreateRequest request, Long userId) {
        // 配额校验与扣减：VIP/管理员不限次，免费用户原子扣减（quotaUsed < quotaTotal 防超扣）
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        if (!userService.isVip(user) && userMapper.deductQuota(userId) == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创作配额已用完，升级 VIP 可无限次创作");
        }

        String taskId = UUID.randomUUID().toString();

        Article article = new Article();
        article.setTaskId(taskId);
        article.setUserId(userId);
        article.setTopic(request.getTopic());
        article.setStatus(ArticleStatusEnum.PROCESSING.getValue());
        article.setPhase(ArticlePhaseEnum.TITLE_GENERATING.getValue());
        // MyBatis-Flex insert 为全字段插入，NOT NULL 列需显式赋值
        LocalDateTime now = LocalDateTime.now();
        article.setCreateTime(now);
        article.setUpdateTime(now);
        article.setIsDelete(0);
        articleMapper.insert(article);

        ArticleTaskContext ctx = new ArticleTaskContext();
        ctx.setTaskId(taskId);
        ctx.setUserId(userId);
        ctx.setTopic(request.getTopic());
        ctx.setStyle(request.getStyle());
        ctx.setEnabledImageMethods(request.getEnabledImageMethods());
        ctx.setStartTime(System.currentTimeMillis());
        contextMap.put(taskId, ctx);

        // 异步执行 StateGraph 智能体1（生成标题），完成后持久化标题方案并暂停等待用户确认
        taskExecutor.execute(() -> {
            try {
                articleAgentGraph.invokeTitle(ctx);
                if (ctx.isCancelled()) {
                    return; // 智能体内部已推送错误
                }
                Article art = getArticleByTaskId(taskId);
                art.setTitleOptions(toJson(ctx.getTitleOptions()));
                art.setPhase(ArticlePhaseEnum.TITLE_SELECTING.getValue());
                art.setUpdateTime(LocalDateTime.now());
                articleMapper.update(art);
            } catch (Exception e) {
                log.error("创建任务后智能体1执行异常: {}", e.getMessage(), e);
                markFailed(taskId, "标题生成失败");
            }
        });
        return taskId;
    }

    @Override
    public void confirmTitle(ArticleConfirmTitleRequest request, Long userId) {
        String taskId = request.getTaskId();
        ArticleTaskContext ctx = getContext(taskId, userId);
        // 幂等防重：防止重复点击"确认标题"导致大纲被重复生成
        if (!ctx.markOutlineStarted()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "大纲正在生成中，请勿重复操作");
        }
        ctx.setSelectedMainTitle(request.getSelectedMainTitle());
        ctx.setSelectedSubTitle(request.getSelectedSubTitle());
        ctx.setUserDescription(request.getUserDescription());

        Article article = getArticleByTaskId(taskId);
        article.setMainTitle(request.getSelectedMainTitle());
        article.setSubTitle(request.getSelectedSubTitle());
        article.setUserDescription(request.getUserDescription());
        article.setPhase(ArticlePhaseEnum.OUTLINE_GENERATING.getValue());
        articleMapper.update(article);

        // 异步执行 StateGraph 智能体2（生成大纲，流式），完成后持久化大纲并暂停等待用户确认
        taskExecutor.execute(() -> {
            try {
                articleAgentGraph.invokeOutline(ctx);
                if (ctx.isCancelled()) {
                    return; // 智能体内部已推送错误
                }
                Article art = getArticleByTaskId(taskId);
                art.setOutline(toJson(ctx.getOutline()));
                art.setPhase(ArticlePhaseEnum.OUTLINE_EDITING.getValue());
                art.setUpdateTime(LocalDateTime.now());
                articleMapper.update(art);
            } catch (Exception e) {
                log.error("确认标题后智能体2执行异常: {}", e.getMessage(), e);
                markFailed(taskId, "大纲生成失败");
            }
        });
    }

    @Override
    public void confirmOutline(ArticleConfirmOutlineRequest request, Long userId) {
        String taskId = request.getTaskId();
        ArticleTaskContext ctx = getContext(taskId, userId);
        // 幂等防重：防止重复点击"确认大纲"导致正文链路被重复执行
        if (!ctx.markContentStarted()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "正文正在生成中，请勿重复操作");
        }
        // 保存用户编辑后的大纲
        List<OutlineItem> outline = request.getOutline().stream().map(section -> {
            OutlineItem item = new OutlineItem();
            item.setSection(section.getSection());
            item.setTitle(section.getTitle());
            item.setPoints(section.getPoints());
            return item;
        }).toList();
        ctx.setOutline(outline);

        Article article = getArticleByTaskId(taskId);
        article.setOutline(toJson(outline));
        article.setPhase(ArticlePhaseEnum.CONTENT_GENERATING.getValue());
        articleMapper.update(article);

        // 异步执行 StateGraph 智能体3（生成正文），正文完成后暂停等待用户确认是否配图
        taskExecutor.execute(() -> {
            try {
                articleAgentGraph.invokeContent(ctx);
                if (ctx.isCancelled()) {
                    return;
                }
                // 持久化正文，暂停等待用户确认配图方案（confirmContent 再继续链路）
                Article art = getArticleByTaskId(taskId);
                art.setContent(ctx.getContent().toString());
                art.setPhase(ArticlePhaseEnum.CONTENT_CONFIRMING.getValue());
                art.setUpdateTime(LocalDateTime.now());
                articleMapper.update(art);
                // 注意：此处不 remove contextMap，由 confirmContent 的异步任务完成后清理
            } catch (Exception e) {
                log.error("确认大纲后智能体3执行异常: {}", e.getMessage(), e);
                markFailed(taskId, "正文生成失败");
            }
        });
    }

    @Override
    public void confirmContent(ArticleConfirmContentRequest request, Long userId) {
        String taskId = request.getTaskId();
        ArticleTaskContext ctx = getContext(taskId, userId);
        // VIP 权限校验：AI 生图(NANO_BANANA)/SVG 图表(SVG_DIAGRAM) 仅限 VIP 会员
        User vipUser = userService.getById(userId);
        if (request.getEnabledImageMethods() != null && !userService.isVip(vipUser)
                && request.getEnabledImageMethods().stream().anyMatch(this::isVipOnlyImageMethod)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "AI 智能生图与 SVG 图表仅限 VIP 会员使用");
        }
        // 幂等防重：防止重复确认配图导致链路被重复执行
        if (!ctx.markImageTaskStarted()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "正在处理中，请勿重复操作");
        }
        boolean generateImages = Boolean.TRUE.equals(request.getGenerateImages());
        // 记录用户选择的配图方式（不生成图片时传空数组，ImageFetchAgent 默认 Pexels 兜底但不会执行）
        ctx.setEnabledImageMethods(generateImages ? request.getEnabledImageMethods() : List.of());

        Article article = getArticleByTaskId(taskId);
        if (generateImages) {
            article.setPhase(ArticlePhaseEnum.IMAGE_GENERATING.getValue());
            article.setUpdateTime(LocalDateTime.now());
            articleMapper.update(article);
        }

        // 异步执行 StateGraph 智能体4-6（配图分析→配图并行→合成）；不生成图片则仅执行合成
        taskExecutor.execute(() -> {
            try {
                if (generateImages) {
                    articleAgentGraph.invokeImage(ctx);
                } else {
                    articleAgentGraph.invokeMergeOnly(ctx);
                }
                if (ctx.isCancelled()) {
                    return;
                }
                persistCompleted(article, ctx);
            } catch (Exception e) {
                log.error("配图/合成链路执行异常: {}", e.getMessage(), e);
                markFailed(taskId, "内容生成失败");
            } finally {
                contextMap.remove(taskId);
            }
        });
    }

    @Override
    public List<OutlineSection> aiModifyOutline(ArticleAiModifyOutlineRequest request, Long userId) {
        // VIP 权限校验：AI 修改大纲仅限 VIP 会员
        User vipUser = userService.getById(userId);
        if (!userService.isVip(vipUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "AI 修改大纲仅限 VIP 会员使用");
        }
        String taskId = request.getTaskId();
        Article article = getArticleByTaskId(taskId);
        if (!article.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作该任务");
        }
        String outlineJson = article.getOutline() == null ? "[]" : article.getOutline();
        Map<String, String> vars = new HashMap<>();
        vars.put("outline", outlineJson);
        vars.put("suggestion", request.getModifySuggestion() == null ? "" : request.getModifySuggestion());
        String prompt = PromptConstant.render(PromptConstant.AI_MODIFY_OUTLINE_PROMPT, vars);

        String result = chatClientBuilder.build().prompt().user(prompt).call().content();
        List<OutlineSection> newOutline = JsonExtractUtil.parseList(result, OutlineSection.class);

        // 同步更新数据库与内存上下文
        article.setOutline(toJson(newOutline));
        articleMapper.update(article);
        ArticleTaskContext ctx = contextMap.get(taskId);
        if (ctx != null) {
            ctx.touch();
            ctx.setOutline(newOutline.stream().map(s -> {
                OutlineItem item = new OutlineItem();
                item.setSection(s.getSection());
                item.setTitle(s.getTitle());
                item.setPoints(s.getPoints());
                return item;
            }).toList());
        }
        return newOutline;
    }

    // ==================== 私有辅助 ====================

    /**
     * 判断是否为仅 VIP 可用的配图方式
     */
    private boolean isVipOnlyImageMethod(String method) {
        return ImageMethodEnum.NANO_BANANA.getValue().equals(method)
                || ImageMethodEnum.SVG_DIAGRAM.getValue().equals(method);
    }

    /**
     * 获取任务执行统计/日志（供 execution-logs 接口使用）
     * <p>
     * 任务进行中从内存上下文读取；完成后返回基础统计。
     */
    public AgentExecutionStats getExecutionStats(String taskId, Long userId) {
        Article article = getArticleByTaskId(taskId);
        if (!article.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该任务");
        }
        AgentExecutionStats stats = new AgentExecutionStats();
        stats.setTaskId(taskId);
        stats.setOverallStatus(article.getStatus());
        ArticleTaskContext ctx = contextMap.get(taskId);
        if (ctx != null) {
            stats.setAgentCount(ctx.getAgentDurations().size());
            stats.setAgentDurations(ctx.getAgentDurations());
            stats.setTotalDurationMs(System.currentTimeMillis() - ctx.getStartTime());
            stats.setLogs(ctx.getLogs());
        } else {
            stats.setAgentCount(0);
            stats.setAgentDurations(new HashMap<>());
            stats.setTotalDurationMs(0L);
            stats.setLogs(new ArrayList<>());
        }
        return stats;
    }

    private ArticleTaskContext getContext(String taskId, Long userId) {
        if (taskId == null || taskId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务 ID 不能为空");
        }
        ArticleTaskContext ctx = contextMap.get(taskId);
        if (ctx == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务不存在或已过期，请重新创建");
        }
        if (!ctx.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作该任务");
        }
        // 惰性过期检查：用户长时间未操作（超过任务总超时时间）视为任务过期，清理并提示
        if (System.currentTimeMillis() - ctx.getLastActiveTime() > ArticleConstant.TASK_TIMEOUT_MS) {
            log.info("[{}] 任务因长时间未操作而过期", taskId);
            contextMap.remove(taskId);
            sseEmitterManager.complete(taskId);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务已过期，请重新创建");
        }
        ctx.touch();
        return ctx;
    }

    /**
     * 定时清理过期任务上下文（防止用户长时间不操作导致的内存泄漏，默认 5 分钟扫描一次）
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void cleanExpiredContexts() {
        long now = System.currentTimeMillis();
        contextMap.forEach((taskId, ctx) -> {
            if (now - ctx.getLastActiveTime() > ArticleConstant.TASK_TIMEOUT_MS) {
                log.info("[{}] 定时清理过期任务上下文", taskId);
                contextMap.remove(taskId);
                sseEmitterManager.complete(taskId);
            }
        });
    }

    private Article getArticleByTaskId(String taskId) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("taskId", taskId);
        Article article = articleMapper.selectOneByQuery(queryWrapper);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文章不存在");
        }
        return article;
    }

    private String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 全链路完成后持久化最终结果
     */
    private void persistCompleted(Article article, ArticleTaskContext ctx) {
        article.setContent(ctx.getContent().toString());
        article.setFullContent(ctx.getFullContent());
        article.setImages(toJson(ctx.getImages()));
        if (ctx.getImages() != null && !ctx.getImages().isEmpty()) {
            article.setCoverImage(ctx.getImages().get(0).getUrl());
        }
        article.setStatus(ArticleStatusEnum.COMPLETED.getValue());
        article.setPhase(ArticlePhaseEnum.COMPLETED.getValue());
        article.setCompletedTime(LocalDateTime.now());
        articleMapper.update(article);
    }

    /**
     * 标记任务失败并推送错误
     */
    private void markFailed(String taskId, String message) {
        try {
            Article article = getArticleByTaskId(taskId);
            article.setStatus(ArticleStatusEnum.FAILED.getValue());
            article.setPhase(ArticlePhaseEnum.FAILED.getValue());
            article.setErrorMessage(message);
            articleMapper.update(article);
        } catch (Exception e) {
            log.error("标记任务失败异常: {}", e.getMessage());
        }
    }
}

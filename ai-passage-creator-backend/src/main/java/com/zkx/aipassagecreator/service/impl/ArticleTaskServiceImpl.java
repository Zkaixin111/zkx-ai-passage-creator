package com.zkx.aipassagecreator.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.zkx.aipassagecreator.common.JsonExtractUtil;
import com.zkx.aipassagecreator.config.ArticleAgentGraph;
import com.zkx.aipassagecreator.config.ArticleTaskRegistry;
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
import com.zkx.aipassagecreator.model.enums.SSEMessageTypeEnum;
import com.zkx.aipassagecreator.model.vo.AgentExecutionStats;
import com.zkx.aipassagecreator.model.vo.ImageItem;
import com.zkx.aipassagecreator.model.vo.OutlineItem;
import com.zkx.aipassagecreator.model.vo.OutlineSection;
import com.zkx.aipassagecreator.model.vo.TitleOption;
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
import java.util.concurrent.Executor;

/**
 * 文章任务编排服务 —— 整个项目的「大脑」
 * <p>
 * 职责：管理 4 个异步执行块（对应 6 个智能体），每次执行完后暂停等待用户确认。
 * 内存中通过 ArticleTaskRegistry 维护 taskId → ArticleTaskContext 映射。
 * <p>
 * 核心流程（4 个异步块）：
 * <pre>
 * createTask()       → 线程池执行 invokeTitle()    → 暂停，等用户选标题
 *   ↓ 用户点确认标题
 * confirmTitle()     → 线程池执行 invokeOutline()  → 暂停，等用户编辑大纲
 *   ↓ 用户点确认大纲
 * confirmOutline()   → 线池执行 invokeContent()   → 暂停，等用户确认配图
 *   ↓ 用户点确认配图
 * confirmContent()   → 线程池执行 invokeImage()    → 全部完成，清理 Registry
 * </pre>
 */
@Slf4j
@Service
public class ArticleTaskServiceImpl implements ArticleTaskService {

    @Resource
    private ArticleMapper articleMapper;

    /** 智能体链路专用线程池（core=2, max=10），和配图线程池隔离 */
    @Resource
    @Qualifier("articleTaskExecutor")
    private Executor taskExecutor;

    /** StateGraph 编排组件，包含 5 段子图的编译和调用 */
    @Resource
    private ArticleAgentGraph articleAgentGraph;

    /** Spring AI 的 ChatClient 构建器，用于 AI 修改大纲等非主流程的同步调用 */
    @Resource
    private ChatClient.Builder chatClientBuilder;

    /** SSE 推送管理器，通过 taskId 找到对应的 SseEmitter 推送消息 */
    @Resource
    private SseEmitterManager sseEmitterManager;

    @Resource
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    /** 任务注册表：ConcurrentHashMap<taskId, ArticleTaskContext>，所有智能体通过它共享上下文 */
    @Resource
    private ArticleTaskRegistry articleTaskRegistry;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    // ==================== 异步块 1：创建任务 → 生成标题 ====================

    /**
     * 创建文章任务
     * <p>
     * 同步部分：配额扣减 → 落库 Article 记录 → 构建 ctx → 注册到 Registry
     * 异步部分：线程池执行智能体1（TitleAgent 生成 5 个标题方案）
     * 返回：taskId（前端立即拿到，不等 AI 执行完）
     */
    @Override
    public String createTask(ArticleCreateRequest request, Long userId) {
        // 1. 配额校验与扣减
        // VIP/管理员不限次；免费用户走 SQL 原子扣减（UPDATE ... SET quotaUsed = quotaUsed + 1 WHERE quotaUsed < quotaTotal）
        // 返回 0 表示配额已用完，防并发超扣
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        boolean vip = userService.isVip(user);
        boolean quotaDeducted = false;
        if (!vip && userMapper.deductQuota(userId) == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创作配额已用完，升级 VIP 可无限次创作");
        }
        if (!vip) {
            quotaDeducted = true;
        }

        // 2. 生成唯一 taskId
        String taskId = UUID.randomUUID().toString();

        // 3. 落库一条 Article 记录，状态=处理中，阶段=标题生成中
        Article article = new Article();
        article.setTaskId(taskId);
        article.setUserId(userId);
        article.setTopic(request.getTopic());
        article.setStatus(ArticleStatusEnum.PROCESSING.getValue());
        article.setPhase(ArticlePhaseEnum.TITLE_GENERATING.getValue());
        // MyBatis-Flex insert 为全字段插入，NOT NULL 列需显式赋值，否则会报错
        LocalDateTime now = LocalDateTime.now();
        article.setCreateTime(now);
        article.setUpdateTime(now);
        article.setIsDelete(0);
        try {
            articleMapper.insert(article);
        } catch (RuntimeException e) {
            // 落库失败：退还刚扣掉的配额，避免用户没拿到任务却白扣一次
            log.error("创建文章任务落库失败，退还配额: {}", e.getMessage(), e);
            if (quotaDeducted) {
                userMapper.refundQuota(userId);
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建任务失败，请重试");
        }

        // 4. 构建 ArticleTaskContext —— 贯穿整个任务生命周期的上下文对象
        // 6 个智能体都通过它共享数据（选题、风格、字数、大纲、正文、配图等）
        ArticleTaskContext ctx = new ArticleTaskContext();
        ctx.setTaskId(taskId);
        ctx.setUserId(userId);
        ctx.setTopic(request.getTopic());
        ctx.setStyle(request.getStyle());
        // 字数处理：前端传了有效值就用，否则用默认值；同时限制上限，
        // 防止传入超大字数导致 token 成本和生成耗时失控
        int requestedWordCount = request.getWordCount() == null ? 0 : request.getWordCount();
        ctx.setWordCount(requestedWordCount > 0
                ? Math.min(requestedWordCount, ArticleConstant.MAX_WORD_COUNT)
                : ArticleConstant.DEFAULT_WORD_COUNT);
        ctx.setEnabledImageMethods(request.getEnabledImageMethods());
        ctx.setStartTime(System.currentTimeMillis());
        // 5. 注册到 Registry —— 后续所有智能体通过 taskId 从这里取 ctx
        // （不通过方法参数传递是因为 StateGraph 的 OverAllState 会深拷贝输入，复杂对象无法生效）
        articleTaskRegistry.put(taskId, ctx);

        // 6. 异步执行智能体1（TitleAgent 生成 5 个标题方案）
        // 提交后立即 return taskId，不等 AI 执行完
        taskExecutor.execute(() -> {
            try {
                // 调用 StateGraph 执行标题生成子图
                articleAgentGraph.invokeTitle(ctx);
                // 智能体内部失败时会 setCancelled 且不抛异常，这里必须落库为 FAILED，
                // 否则数据库永远停在「标题生成中」，历史页与看门狗都无法结束该任务
                if (ctx.isCancelled()) {
                    markFailed(taskId, "标题生成失败");
                    return;
                }
                // 智能体1执行完后，把标题方案落库，阶段流转为 TITLE_SELECTING（等用户选标题）
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

    // ==================== 异步块 2：确认标题 → 生成大纲 ====================

    /**
     * 确认标题（第 1 次用户打断）
     * <p>
     * 同步部分：CAS 防重 → 存标题到 ctx → 落库 → 阶段流转
     * 异步部分：线程池执行智能体2（OutlineAgent 流式生成大纲）
     */
    @Override
    public void confirmTitle(ArticleConfirmTitleRequest request, Long userId) {
        String taskId = request.getTaskId();
        // 从 Registry 取 ctx（内部有权限校验 + 惰性过期检查）
        ArticleTaskContext ctx = getContext(taskId, userId);

        // 把用户选的标题存入 ctx（后续大纲/正文智能体会用到）
        ctx.setSelectedMainTitle(request.getSelectedMainTitle());
        ctx.setSelectedSubTitle(request.getSelectedSubTitle());
        ctx.setUserDescription(request.getUserDescription());

        // 落库用户选择的标题，阶段流转为大纲生成中
        Article article = getArticleByTaskId(taskId);
        article.setMainTitle(request.getSelectedMainTitle());
        article.setSubTitle(request.getSelectedSubTitle());
        article.setUserDescription(request.getUserDescription());
        article.setPhase(ArticlePhaseEnum.OUTLINE_GENERATING.getValue());
        articleMapper.update(article);

        // CAS 防重放在所有落库操作之后、异步提交之前：
        // 若前面的校验/落库抛异常，标记不会被提前置位，用户可以重新确认；
        // 否则任务会永久卡在「大纲正在生成中，请勿重复操作」
        if (!ctx.markOutlineStarted()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "大纲正在生成中，请勿重复操作");
        }

        // 异步执行智能体2（OutlineAgent 流式生成大纲 JSON，通过 SSE 逐字推送）
        taskExecutor.execute(() -> {
            try {
                articleAgentGraph.invokeOutline(ctx);
                // 智能体内部失败（cancelled）不会抛异常，必须显式标记失败，避免状态卡死
                if (ctx.isCancelled()) {
                    markFailed(taskId, "大纲生成失败");
                    return;
                }
                // 大纲生成完后落库，阶段流转为大纲编辑中（等用户编辑/确认大纲）
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

    // ==================== 异步块 3：确认大纲 → 生成正文 ====================

    /**
     * 确认大纲（第 2 次用户打断）
     * <p>
     * 同步部分：CAS 防重 → 存用户编辑后的大纲到 ctx → 落库 → 阶段流转
     * 异步部分：线程池执行智能体3（ContentAgent 流式写正文，带字数约束）
     */
    @Override
    public void confirmOutline(ArticleConfirmOutlineRequest request, Long userId) {
        String taskId = request.getTaskId();
        ArticleTaskContext ctx = getContext(taskId, userId);

        // 把前端传来的大纲（用户可能编辑过）转换为内部 OutlineItem 格式，存入 ctx
        List<OutlineItem> outline = request.getOutline().stream().map(section -> {
            OutlineItem item = new OutlineItem();
            item.setSection(section.getSection());
            item.setTitle(section.getTitle());
            item.setPoints(section.getPoints());
            return item;
        }).toList();
        ctx.setOutline(outline);

        // 落库大纲，阶段流转为正文生成中
        Article article = getArticleByTaskId(taskId);
        article.setOutline(toJson(outline));
        article.setPhase(ArticlePhaseEnum.CONTENT_GENERATING.getValue());
        articleMapper.update(article);

        // CAS 防重放在落库之后：避免落库异常时标记已置位，导致正文链路永久无法启动
        if (!ctx.markContentStarted()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "正文正在生成中，请勿重复操作");
        }

        // 异步执行智能体3（ContentAgent 流式写正文，通过 SSE 逐字推送）
        // 正文写完后暂停，等用户确认配图方案（不 remove registry，由 confirmContent 清理）
        taskExecutor.execute(() -> {
            try {
                articleAgentGraph.invokeContent(ctx);
                // 智能体内部失败（cancelled）不会抛异常，必须显式标记失败，避免状态卡死
                if (ctx.isCancelled()) {
                    markFailed(taskId, "正文生成失败");
                    return;
                }
                // 持久化正文，阶段流转为正文确认中（等用户确认配图方案）
                Article art = getArticleByTaskId(taskId);
                art.setContent(ctx.getContent().toString());
                art.setPhase(ArticlePhaseEnum.CONTENT_CONFIRMING.getValue());
                art.setUpdateTime(LocalDateTime.now());
                articleMapper.update(art);
                // 注意：此处不 remove registry，由 confirmContent 的异步任务完成后清理
            } catch (Exception e) {
                log.error("确认大纲后智能体3执行异常: {}", e.getMessage(), e);
                markFailed(taskId, "正文生成失败");
            }
        });
    }

    // ==================== 异步块 4：确认配图 → 配图分析+并行配图+合成 ====================

    /**
     * 确认配图方案（第 3 次用户打断，也是最后一次）
     * <p>
     * 同步部分：VIP 权限校验 → CAS 防重 → 存配图方式到 ctx
     * 异步部分：线程池执行智能体4-6（ImageAnalysisAgent → ImageAgent → CompositionAgent）
     * 完成后：persistCompleted 落库最终结果 → finally 从 Registry 清理
     */
    @Override
    public void confirmContent(ArticleConfirmContentRequest request, Long userId) {
        String taskId = request.getTaskId();
        ArticleTaskContext ctx = getContext(taskId, userId);

        // VIP 权限校验：AI 生图和 SVG 图表(SVG_DIAGRAM)仅限 VIP 会员
        User vipUser = userService.getById(userId);
        if (request.getEnabledImageMethods() != null && !userService.isVip(vipUser)
                && request.getEnabledImageMethods().stream().anyMatch(this::isVipOnlyImageMethod)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "AI 智能生图与 SVG 图表仅限 VIP 会员使用");
        }

        boolean generateImages = Boolean.TRUE.equals(request.getGenerateImages());
        // 记录用户选择的配图方式（不生成图片时传空数组）
        ctx.setEnabledImageMethods(generateImages ? request.getEnabledImageMethods() : List.of());

        Article article = getArticleByTaskId(taskId);
        if (generateImages) {
            article.setPhase(ArticlePhaseEnum.IMAGE_GENERATING.getValue());
            article.setUpdateTime(LocalDateTime.now());
            articleMapper.update(article);
        }

        // CAS 防重放在落库之后：避免落库异常时标记已置位，导致配图链路永久无法启动
        if (!ctx.markImageTaskStarted()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "正在处理中，请勿重复操作");
        }

        // 异步执行智能体4-6
        // 生成图片：invokeImage() = 配图分析 → 并行配图 → 合成
        // 不生成图片：invokeMergeOnly() = 仅合成（把正文拼接为最终内容）
        taskExecutor.execute(() -> {
            try {
                long mergeStart = System.currentTimeMillis();
                log.info("[{}] 配图/合成链路启动，generateImages={}", taskId, generateImages);
                if (generateImages) {
                    articleAgentGraph.invokeImage(ctx);
                } else {
                    articleAgentGraph.invokeMergeOnly(ctx);
                }
                log.info("[{}] 配图/合成链路返回，耗时 {}ms，cancelled={}", taskId,
                        System.currentTimeMillis() - mergeStart, ctx.isCancelled());
                if (ctx.isCancelled()) {
                    log.warn("[{}] 链路被取消（智能体内部失败），标记任务失败", taskId);
                    markFailed(taskId, "内容生成失败");
                    return;
                }
                // 全链路完成，持久化最终结果（正文+图文+封面图）
                log.info("[{}] 开始持久化完成态 persistCompleted", taskId);
                persistCompleted(article, ctx);
                log.info("[{}] persistCompleted 完成", taskId);
                // 落库成功后才推送 ALL_COMPLETE：否则前端会先收到"完成"，
                // 但此时数据库仍是 PROCESSING，刷新/查看详情会看到未完成的文章
                sseEmitterManager.send(taskId, SSEMessageTypeEnum.ALL_COMPLETE, null);
                sseEmitterManager.complete(taskId);
            } catch (Exception e) {
                log.error("配图/合成链路执行异常: {}", e.getMessage(), e);
                markFailed(taskId, "内容生成失败");
            } finally {
                // 无论成功还是失败，都从 Registry 清理，释放内存
                articleTaskRegistry.remove(taskId);
            }
        });
    }

    // ==================== AI 修改大纲（独立于主流程） ====================

    /**
     * AI 修改大纲（VIP 功能，同步返回）
     * 不走 StateGraph，直接调 ChatClient 同步请求 AI 返回新大纲。
     */
    @Override
    public List<OutlineSection> aiModifyOutline(ArticleAiModifyOutlineRequest request, Long userId) {
        // VIP 权限校验
        User vipUser = userService.getById(userId);
        if (!userService.isVip(vipUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "AI 修改大纲仅限 VIP 会员使用");
        }
        String taskId = request.getTaskId();
        Article article = getArticleByTaskId(taskId);
        if (!article.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作该任务");
        }

        // 构造 prompt：把当前大纲 + 用户修改建议填入模板
        String outlineJson = article.getOutline() == null ? "[]" : article.getOutline();
        Map<String, String> vars = new HashMap<>();
        vars.put("outline", outlineJson);
        vars.put("suggestion", request.getModifySuggestion() == null ? "" : request.getModifySuggestion());
        String prompt = PromptConstant.render(PromptConstant.AI_MODIFY_OUTLINE_PROMPT, vars);

        // 同步调用 AI，等待返回
        String result = chatClientBuilder.build().prompt().user(prompt).call().content();
        // 从 AI 返回的文本中提取 JSON 列表
        List<OutlineSection> newOutline = JsonExtractUtil.parseList(result, OutlineSection.class);

        // 同步更新数据库与内存上下文
        article.setOutline(toJson(newOutline));
        articleMapper.update(article);
        ArticleTaskContext ctx = articleTaskRegistry.get(taskId);
        if (ctx != null) {
            ctx.touch();  // 刷新活跃时间，防止过期清理
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

    // ==================== 私有辅助方法 ====================

    /**
     * 判断是否为仅 VIP 可用的配图方式
     */
    private boolean isVipOnlyImageMethod(String method) {
        return ImageMethodEnum.NANO_BANANA.getValue().equals(method)
                || ImageMethodEnum.SVG_DIAGRAM.getValue().equals(method);
    }

    /**
     * 获取任务执行统计/日志（供 execution-logs 接口使用）
     * 任务进行中：从内存上下文读取实时数据
     * 任务完成后：返回基础统计（ctx 已被清理）
     */
    public AgentExecutionStats getExecutionStats(String taskId, Long userId, boolean isAdmin) {
        Article article = getArticleByTaskId(taskId);
        // 权限校验：非管理员只能查看自己的任务
        if (!isAdmin && !article.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该任务");
        }
        AgentExecutionStats stats = new AgentExecutionStats();
        stats.setTaskId(taskId);
        stats.setOverallStatus(article.getStatus());
        ArticleTaskContext ctx = articleTaskRegistry.get(taskId);
        if (ctx != null) {
            // 任务还在进行中，从内存读取实时数据
            stats.setAgentCount(ctx.getAgentDurations().size());
            stats.setAgentDurations(ctx.getAgentDurations());
            stats.setTotalDurationMs(System.currentTimeMillis() - ctx.getStartTime());
            stats.setLogs(ctx.getLogs());
        } else {
            // 任务已结束（ctx 已被清理），返回空统计
            stats.setAgentCount(0);
            stats.setAgentDurations(new HashMap<>());
            stats.setTotalDurationMs(0L);
            stats.setLogs(new ArrayList<>());
        }
        return stats;
    }

    /**
     * 从 Registry 获取任务上下文 —— 所有 confirm 方法的公共前置
     * <p>
     * 包含三层检查：
     * 1. ctx 是否存在（不存在 = 任务已过期或已完成）
     * 2. 用户是否有权限操作该任务
     * 3. 任务是否已超过总超时时间（30 分钟无操作 → 过期清理）
     */
    private ArticleTaskContext getContext(String taskId, Long userId) {
        if (taskId == null || taskId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务 ID 不能为空");
        }
        ArticleTaskContext ctx = articleTaskRegistry.get(taskId);
        if (ctx == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务不存在或已过期，请重新创建");
        }
        if (!ctx.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作该任务");
        }
        // 惰性过期检查：用户长时间未操作（超过 30 分钟）视为任务过期
        // 这是防内存泄漏的第 1 道保险（第 2 道是定时清理）
        if (System.currentTimeMillis() - ctx.getLastActiveTime() > ArticleConstant.TASK_TIMEOUT_MS) {
            log.info("[{}] 任务因长时间未操作而过期", taskId);
            articleTaskRegistry.remove(taskId);
            sseEmitterManager.complete(taskId);  // 关闭 SSE 连接
            // 内存已清理，数据库也必须收尾，否则历史页会永远停留在"生成中"
            markFailed(taskId, "任务已超时，请重新创建");
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务已过期，请重新创建");
        }
        ctx.touch();  // 刷新活跃时间，重置 30 分钟倒计时
        return ctx;
    }

    /**
     * 校验任务归属，防止通过 taskId 越权读取他人创作内容
     */
    @Override
    public void checkTaskOwnership(String taskId, Long userId) {
        Article article = getArticleByTaskId(taskId);
        if (!article.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该任务");
        }
    }

    /**
     * SSE 连接建立后立即推送任务快照 —— 断点恢复的核心
     * <p>
     * 场景：用户切换页面/刷新/网络断开导致 EventSource 断开，期间智能体仍在后台执行，
     * 这段时间的流式消息全部丢失。前端重连后先收到 SNAPSHOT，
     * 一次性恢复当前阶段与已生成内容，后续的实时流式消息继续追加。
     * <p>
     * 数据源优先级：实时进度（content/段落进度）读内存 ctx；标题/大纲等落库字段读 article 表兜底。
     * 任务已完成/失败时也会推快照（携带 fullContent/errorMessage），前端直接跳完成态或报错。
     */
    @Override
    public void pushSnapshot(String taskId) {
        try {
            Article article;
            try {
                article = getArticleByTaskId(taskId);
            } catch (Exception e) {
                // 文章不存在：不推快照，让前端走自己的超时/错误处理
                return;
            }
            ArticleTaskContext ctx = articleTaskRegistry.get(taskId);
            // 僵尸任务检测：数据库显示"处理中/待处理"，但内存上下文已不存在
            // （服务重启、超时清理、或账号换了设备），此时前端若按旧 phase 恢复，
            // 后续任何确认操作都会报"任务已过期"。这里直接把库状态收尾为 FAILED，
            // 让用户看到明确的失败提示并重新创建，而不是卡在一个死界面里。
            if (ctx == null && isProcessingStatus(article.getStatus())) {
                log.info("[{}] 快照发现僵尸任务（无内存上下文），标记为失败", taskId);
                markFailed(taskId, "任务已过期，请重新创建");
                try {
                    article = getArticleByTaskId(taskId);  // 重新读取最新的 FAILED 状态
                } catch (Exception ignored) {
                    return;
                }
            }
            Map<String, Object> snap = new HashMap<>();
            snap.put("phase", article.getPhase());
            snap.put("status", article.getStatus());
            snap.put("topic", article.getTopic());
            snap.put("mainTitle", article.getMainTitle());
            snap.put("subTitle", article.getSubTitle());
            snap.put("errorMessage", article.getErrorMessage());

            // 标题方案：优先内存（最新），兜底数据库
            if (ctx != null && ctx.getTitleOptions() != null && !ctx.getTitleOptions().isEmpty()) {
                snap.put("titleOptions", ctx.getTitleOptions());
            } else if (article.getTitleOptions() != null) {
                snap.put("titleOptions", JsonExtractUtil.parseList(article.getTitleOptions(), TitleOption.class));
            }
            // 大纲：优先内存，兜底数据库
            if (ctx != null && ctx.getOutline() != null && !ctx.getOutline().isEmpty()) {
                snap.put("outline", ctx.getOutline());
            } else if (article.getOutline() != null) {
                snap.put("outline", JsonExtractUtil.parseList(article.getOutline(), OutlineItem.class));
            }
            // 大纲流式原文：仅内存有（用于 OUTLINE_GENERATING 阶段重连后续传）
            if (ctx != null && ctx.getOutlineRaw().length() > 0) {
                snap.put("outlineRaw", ctx.getOutlineRaw().toString());
            }
            // 正文：优先内存实时累积值（生成中场景），兜底数据库（CONTENT_CONFIRMING 及之后）
            if (ctx != null && ctx.getContent().length() > 0) {
                snap.put("content", ctx.getContent().toString());
            } else if (article.getContent() != null) {
                snap.put("content", article.getContent());
            }
            // 分段进度：仅内存有
            if (ctx != null) {
                snap.put("currentSectionIndex", ctx.getCurrentSectionIndex());
                snap.put("completedSections", ctx.getCompletedSections());
                snap.put("totalSections", ctx.getTotalSections());
                List<String> sections = new ArrayList<>();
                // synchronizedList 遍历时需要手动加同步，防止智能体线程并发 add 导致 CME
                synchronized (ctx.getSectionContents()) {
                    for (StringBuilder sb : ctx.getSectionContents()) {
                        sections.add(sb.toString());
                    }
                }
                snap.put("sectionContents", sections);
                if (ctx.getImageRequirements() != null) {
                    snap.put("totalImages", ctx.getImageRequirements().size());
                }
            }
            // 配图列表：优先内存（IMAGE_GENERATING 实时进度），兜底数据库
            if (ctx != null && ctx.getImages() != null && !ctx.getImages().isEmpty()) {
                snap.put("images", ctx.getImages());
            } else if (article.getImages() != null) {
                snap.put("images", JsonExtractUtil.parseList(article.getImages(), ImageItem.class));
            }
            // 完整图文（COMPLETED 场景直接恢复成稿）
            if (article.getFullContent() != null) {
                snap.put("fullContent", article.getFullContent());
            }
            sseEmitterManager.send(taskId, SSEMessageTypeEnum.SNAPSHOT, snap);
        } catch (Exception e) {
            // 快照推送失败不影响主流程，前端还有 watchdog 兜底
            log.warn("[{}] 推送任务快照失败: {}", taskId, e.getMessage());
        }
    }

    /**
     * 定时清理过期任务上下文 —— 防内存泄漏的第 2 道保险
     * <p>
     * 每 5 分钟扫一次 Registry，清理超过 30 分钟没操作的任务。
     * 如果用户创建任务后直接关闭浏览器，惰性检查不会触发，需要靠定时清理。
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void cleanExpiredContexts() {
        long now = System.currentTimeMillis();
        articleTaskRegistry.forEach((taskId, ctx) -> {
            if (now - ctx.getLastActiveTime() > ArticleConstant.TASK_TIMEOUT_MS) {
                log.info("[{}] 定时清理过期任务上下文", taskId);
                articleTaskRegistry.remove(taskId);
                sseEmitterManager.complete(taskId);
                // 同步收尾数据库状态，否则用户关浏览器后任务会永久显示"生成中"，
                // 历史页还会出现可点「继续创作」的僵尸条目
                markFailed(taskId, "任务已超时，请重新创建");
            }
        });
    }

    /**
     * 根据 taskId 从数据库查询 Article 实体
     */
    private Article getArticleByTaskId(String taskId) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("taskId", taskId);
        Article article = articleMapper.selectOneByQuery(queryWrapper);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文章不存在");
        }
        return article;
    }

    /**
     * 对象转 JSON 字符串，失败返回 null（不抛异常）
     */
    private String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 全链路完成后持久化最终结果
     * 把 ctx 中的正文、图文内容、配图列表、封面图全部写入数据库。
     */
    private void persistCompleted(Article article, ArticleTaskContext ctx) {
        article.setContent(ctx.getContent().toString());
        article.setFullContent(ctx.getFullContent());
        article.setImages(toJson(ctx.getImages()));
        // 第一张配图作为封面
        if (ctx.getImages() != null && !ctx.getImages().isEmpty()) {
            article.setCoverImage(ctx.getImages().get(0).getUrl());
        }
        article.setStatus(ArticleStatusEnum.COMPLETED.getValue());
        article.setPhase(ArticlePhaseEnum.COMPLETED.getValue());
        article.setCompletedTime(LocalDateTime.now());
        articleMapper.update(article);
    }

    /**
     * 判断文章状态是否仍处于未完成（待处理 / 处理中）
     */
    private boolean isProcessingStatus(String status) {
        return ArticleStatusEnum.PENDING.getValue().equals(status)
                || ArticleStatusEnum.PROCESSING.getValue().equals(status);
    }

    /**
     * 标记任务失败
     * 把数据库中的 Article 状态改为 FAILED，阶段改为 FAILED。
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

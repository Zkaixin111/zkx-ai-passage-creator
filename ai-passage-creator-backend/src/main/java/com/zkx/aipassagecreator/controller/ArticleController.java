package com.zkx.aipassagecreator.controller;

import com.mybatisflex.core.paginate.Page;
import com.zkx.aipassagecreator.annotation.AuthCheck;
import com.zkx.aipassagecreator.common.BaseResponse;
import com.zkx.aipassagecreator.common.DeleteRequest;
import com.zkx.aipassagecreator.common.ResultUtils;
import com.zkx.aipassagecreator.config.SseEmitterManager;
import com.zkx.aipassagecreator.exception.ErrorCode;
import com.zkx.aipassagecreator.exception.ThrowUtils;
import com.zkx.aipassagecreator.model.dto.*;
import com.zkx.aipassagecreator.model.entity.User;
import com.zkx.aipassagecreator.model.vo.AgentExecutionStats;
import com.zkx.aipassagecreator.model.vo.ArticleVO;
import com.zkx.aipassagecreator.model.vo.OutlineSection;
import com.zkx.aipassagecreator.service.ArticleService;
import com.zkx.aipassagecreator.service.ArticleTaskService;
import com.zkx.aipassagecreator.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 文章生成模块控制器 —— 整个项目的 HTTP 入口
 * <p>
 * 核心设计：所有 AI 生成接口都是【异步】的。
 * 接口立即返回 taskId，AI 在后台线程池执行，前端通过 SSE 长连接收进度。
 * <p>
 * 交互流程（Human-in-the-Loop，3 次用户确认打断）：
 * 1. POST /create          → 后台启动智能体1（生成标题），返回 taskId
 * 2. 前端用 taskId 建立 SSE 连接，实时收标题方案
 * 3. POST /confirm-title   → 用户选完标题，后台启动智能体2（生成大纲）
 * 4. POST /confirm-outline → 用户编辑完大纲，后台启动智能体3（写正文）
 * 5. POST /confirm-content → 用户确认配图方案，后台启动智能体4-6（配图+合成）
 * 6. 全部完成，前端展示文章
 */
@Tag(name = "文章模块")
@RestController
@RequestMapping("/article")
public class ArticleController {

    @Resource
    private ArticleService articleService;

    @Resource
    private ArticleTaskService articleTaskService;

    @Resource
    private UserService userService;

    @Resource
    private SseEmitterManager sseEmitterManager;

    /**
     * 创建文章任务（异步）
     * 前端调用后立即拿到 taskId，AI 在后台线程池跑智能体1（生成 5 个标题方案）。
     * 前端用 taskId 建立 SSE 连接，实时接收标题方案。
     */
    @AuthCheck
    @Operation(summary = "创建文章任务（异步，返回 taskId）")
    @PostMapping("/create")
    public BaseResponse<String> createArticle(@RequestBody ArticleCreateRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        // 校验选题非空
        ThrowUtils.throwIf(request.getTopic() == null || request.getTopic().isBlank(), ErrorCode.PARAMS_ERROR, "选题不能为空");
        // 从 session 获取当前登录用户
        User loginUser = userService.getLoginUser(httpRequest);
        // 调 Service 创建异步任务，返回 taskId（内部提交线程池，不等 AI 执行完）
        String taskId = articleTaskService.createTask(request, loginUser.getId());
        return ResultUtils.success(taskId);
    }

    /**
     * 确认标题 —— 第 1 次用户打断
     * 用户从 3 个标题方案中选一个，后端收到后启动智能体2（流式生成大纲）。
     * 内部有 CAS 防重，防止用户快速双击导致大纲被重复生成。
     */
    @AuthCheck
    @Operation(summary = "确认标题，触发大纲生成")
    @PostMapping("/confirm-title")
    public BaseResponse<Void> confirmTitle(@RequestBody ArticleConfirmTitleRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        // 调 Service，内部会 CAS 防重 + 异步提交智能体2
        articleTaskService.confirmTitle(request, loginUser.getId());
        return ResultUtils.success(null);
    }

    /**
     * 确认大纲 —— 第 2 次用户打断
     * 用户编辑完大纲后点确认，后端启动智能体3（流式写正文，带字数约束）。
     */
    @AuthCheck
    @Operation(summary = "确认大纲，触发正文与配图生成")
    @PostMapping("/confirm-outline")
    public BaseResponse<Void> confirmOutline(@RequestBody ArticleConfirmOutlineRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        articleTaskService.confirmOutline(request, loginUser.getId());
        return ResultUtils.success(null);
    }

    /**
     * AI 修改大纲（VIP 功能，同步返回）
     * 用户输入修改建议，AI 返回新大纲。和主流程独立，不经过 StateGraph。
     */
    @AuthCheck
    @Operation(summary = "AI 修改大纲（同步返回新大纲）")
    @PostMapping("/ai-modify-outline")
    public BaseResponse<List<OutlineSection>> aiModifyOutline(@RequestBody ArticleAiModifyOutlineRequest request,
                                                              HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        List<OutlineSection> newOutline = articleTaskService.aiModifyOutline(request, loginUser.getId());
        return ResultUtils.success(newOutline);
    }

    /**
     * 确认配图方案 —— 第 3 次用户打断
     * 用户选择是否配图、用哪些配图方式，后端启动智能体4-6（配图分析→并行配图→合成）。
     * 全链路完成后，任务从内存 Registry 中清理。
     */
    @AuthCheck
    @Operation(summary = "确认正文后的配图方案（是否生成配图及配图方式）")
    @PostMapping("/confirm-content")
    public BaseResponse<Void> confirmContent(@RequestBody ArticleConfirmContentRequest request,
                                             HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null || request.getTaskId() == null || request.getTaskId().isBlank(),
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        articleTaskService.confirmContent(request, loginUser.getId());
        return ResultUtils.success(null);
    }

    /**
     * SSE 长连接端点 —— 前端通过 EventSource 连接此接口实时接收进度
     * <p>
     * 超时设为 0L（永不超时），因为用户选标题/大纲的思考时间不可控，
     * 如果设固定超时会导致连接被服务端提前掐断，进度丢失。
     * 连接生命周期由任务上下文的过期清理控制（30 分钟无操作 → 清理 + complete SSE）。
     */
    @AuthCheck
    @Operation(summary = "获取文章生成进度（SSE 长连接）")
    @GetMapping("/progress/{taskId}")
    public SseEmitter getProgress(@PathVariable String taskId, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.isBlank(), ErrorCode.PARAMS_ERROR);
        // 越权校验：SSE 会推送标题/大纲/正文全文，必须先确认任务属于当前登录用户，
        // 校验放在创建 emitter 之前，避免校验失败时把连接遗留在管理器里
        User loginUser = userService.getLoginUser(httpRequest);
        articleTaskService.checkTaskOwnership(taskId, loginUser.getId());
        SseEmitter emitter = new SseEmitter(0L);  // 0L = 永不超时
        sseEmitterManager.register(taskId, emitter);  // 注册到管理器，后续通过 taskId 推送消息
        // 连接结束/超时/异常时从管理器移除，避免内存泄漏
        // 按实例移除：多标签页等场景下，旧连接的回调不能误删新注册的连接
        emitter.onCompletion(() -> sseEmitterManager.remove(taskId, emitter));
        emitter.onTimeout(() -> sseEmitterManager.remove(taskId, emitter));
        emitter.onError(e -> sseEmitterManager.remove(taskId, emitter));
        // 建立后立即补发任务快照：切页/刷新/断线重连场景下前端据此恢复到当前阶段
        articleTaskService.pushSnapshot(taskId);
        return emitter;
    }

    // ==================== 以下为常规 CRUD 接口 ====================

    @AuthCheck
    @Operation(summary = "获取文章详情")
    @GetMapping("/{taskId}")
    public BaseResponse<ArticleVO> getArticle(@PathVariable String taskId, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.isBlank(), ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        boolean isAdmin = userService.isAdmin(httpRequest);
        ArticleVO articleVO = articleService.getArticle(taskId, loginUser.getId(), isAdmin);
        return ResultUtils.success(articleVO);
    }

    @AuthCheck
    @Operation(summary = "分页查询文章列表")
    @PostMapping("/list")
    public BaseResponse<Page<ArticleVO>> listArticle(@RequestBody ArticleQueryRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        boolean isAdmin = userService.isAdmin(httpRequest);
        Page<ArticleVO> page = articleService.listArticle(request, loginUser.getId(), isAdmin);
        return ResultUtils.success(page);
    }

    @AuthCheck
    @Operation(summary = "删除文章")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteArticle(@RequestBody DeleteRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        boolean isAdmin = userService.isAdmin(httpRequest);
        boolean result = articleService.deleteArticle(request.getId(), loginUser.getId(), isAdmin);
        return ResultUtils.success(result);
    }

    /**
     * 获取任务执行日志（智能体耗时、状态、输入输出摘要）
     * 任务进行中：从内存上下文 ArticleTaskContext 读取实时数据
     * 任务完成后：从数据库读取基础统计
     */
    @AuthCheck
    @Operation(summary = "获取任务执行日志")
    @GetMapping("/execution-logs/{taskId}")
    public BaseResponse<AgentExecutionStats> getExecutionLogs(@PathVariable String taskId, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.isBlank(), ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        boolean isAdmin = userService.isAdmin(httpRequest);
        AgentExecutionStats stats = articleService.getExecutionLogs(taskId, loginUser.getId(), isAdmin);
        return ResultUtils.success(stats);
    }
}

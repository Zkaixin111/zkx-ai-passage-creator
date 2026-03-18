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
 * 文章生成模块控制器
 * <p>
 * 提供文章创建（异步）、标题/大纲确认、SSE 进度推送、查询与删除接口。
 * 接口契约与前端 {@code articleController.ts} 完全对齐。
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

    @AuthCheck
    @Operation(summary = "创建文章任务（异步，返回 taskId）")
    @PostMapping("/create")
    public BaseResponse<String> createArticle(@RequestBody ArticleCreateRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getTopic() == null || request.getTopic().isBlank(), ErrorCode.PARAMS_ERROR, "选题不能为空");
        User loginUser = userService.getLoginUser(httpRequest);
        String taskId = articleTaskService.createTask(request, loginUser.getId());
        return ResultUtils.success(taskId);
    }

    @AuthCheck
    @Operation(summary = "确认标题，触发大纲生成")
    @PostMapping("/confirm-title")
    public BaseResponse<Void> confirmTitle(@RequestBody ArticleConfirmTitleRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        articleTaskService.confirmTitle(request, loginUser.getId());
        return ResultUtils.success(null);
    }

    @AuthCheck
    @Operation(summary = "确认大纲，触发正文与配图生成")
    @PostMapping("/confirm-outline")
    public BaseResponse<Void> confirmOutline(@RequestBody ArticleConfirmOutlineRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        articleTaskService.confirmOutline(request, loginUser.getId());
        return ResultUtils.success(null);
    }

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

    @Operation(summary = "获取文章生成进度（SSE 长连接）")
    @GetMapping("/progress/{taskId}")
    public SseEmitter getProgress(@PathVariable String taskId) {
        ThrowUtils.throwIf(taskId == null || taskId.isBlank(), ErrorCode.PARAMS_ERROR);
        // 不设置超时：连接生命周期交给任务上下文过期清理控制，
        // 避免用户等待确认期间（超过固定超时）连接被服务端提前掐断导致进度丢失。
        SseEmitter emitter = new SseEmitter(0L);
        sseEmitterManager.register(taskId, emitter);
        // 连接结束/超时/异常时清理
        emitter.onCompletion(() -> sseEmitterManager.remove(taskId));
        emitter.onTimeout(() -> sseEmitterManager.remove(taskId));
        emitter.onError(e -> sseEmitterManager.remove(taskId));
        return emitter;
    }

    @AuthCheck
    @Operation(summary = "获取文章详情")
    @GetMapping("/{taskId}")
    public BaseResponse<ArticleVO> getArticle(@PathVariable String taskId, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.isBlank(), ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        ArticleVO articleVO = articleService.getArticle(taskId, loginUser.getId());
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

    @AuthCheck
    @Operation(summary = "获取任务执行日志")
    @GetMapping("/execution-logs/{taskId}")
    public BaseResponse<AgentExecutionStats> getExecutionLogs(@PathVariable String taskId, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(taskId == null || taskId.isBlank(), ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        AgentExecutionStats stats = articleService.getExecutionLogs(taskId, loginUser.getId());
        return ResultUtils.success(stats);
    }
}

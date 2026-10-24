package com.zkx.aipassagecreator.service;

import com.zkx.aipassagecreator.model.dto.ArticleAiModifyOutlineRequest;
import com.zkx.aipassagecreator.model.dto.ArticleConfirmContentRequest;
import com.zkx.aipassagecreator.model.dto.ArticleConfirmOutlineRequest;
import com.zkx.aipassagecreator.model.dto.ArticleConfirmTitleRequest;
import com.zkx.aipassagecreator.model.dto.ArticleCreateRequest;
import com.zkx.aipassagecreator.model.vo.OutlineSection;

import java.util.List;

/**
 * 文章任务编排服务
 * <p>
 * 负责创建任务、触发智能体执行链路、处理用户确认交互。
 */
public interface ArticleTaskService {

    /**
     * 创建文章任务，异步启动智能体1（生成标题），返回 taskId
     */
    String createTask(ArticleCreateRequest request, Long userId);

    /**
     * 确认标题，异步启动智能体2（生成大纲）
     */
    void confirmTitle(ArticleConfirmTitleRequest request, Long userId);

    /**
     * 确认大纲，异步启动智能体3（生成正文），正文完成后暂停等待用户确认是否配图
     */
    void confirmOutline(ArticleConfirmOutlineRequest request, Long userId);

    /**
     * 确认正文后的配图方案：生成配图则启动智能体4-6（配图分析→配图→合成），
     * 不生成则直接合成（智能体6）
     */
    void confirmContent(ArticleConfirmContentRequest request, Long userId);

    /**
     * AI 修改大纲（同步返回新大纲）
     */
    List<OutlineSection> aiModifyOutline(ArticleAiModifyOutlineRequest request, Long userId);

    /**
     * SSE 连接建立后立即推送一条任务快照（SNAPSHOT），
     * 让前端在切换页面/刷新/断线重连后能恢复到当前阶段与已生成内容。
     * 数据源：实时进度优先读内存上下文（ctx），落库字段兜底读 article 表。
     */
    void pushSnapshot(String taskId);

    /**
     * 校验任务归属（SSE 进度接口调用）
     * <p>
     * SSE 会推送标题方案、大纲、正文全文，属于用户私密数据，
     * 建立连接前必须确认该 taskId 属于当前登录用户。
     */
    void checkTaskOwnership(String taskId, Long userId);
}

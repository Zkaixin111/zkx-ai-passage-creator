package com.zkx.aipassagecreator.service;

import com.zkx.aipassagecreator.model.dto.ArticleAiModifyOutlineRequest;
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
     * 确认大纲，异步启动智能体3-6（正文→配图→合成）
     */
    void confirmOutline(ArticleConfirmOutlineRequest request, Long userId);

    /**
     * AI 修改大纲（同步返回新大纲）
     */
    List<OutlineSection> aiModifyOutline(ArticleAiModifyOutlineRequest request, Long userId);
}

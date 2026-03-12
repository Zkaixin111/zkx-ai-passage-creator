package com.zkx.aipassagecreator.service;

import com.mybatisflex.core.paginate.Page;
import com.zkx.aipassagecreator.model.dto.ArticleQueryRequest;
import com.zkx.aipassagecreator.model.vo.AgentExecutionStats;
import com.zkx.aipassagecreator.model.vo.ArticleVO;

/**
 * 文章服务（查询/删除/执行日志）
 */
public interface ArticleService {

    /**
     * 获取文章详情
     */
    ArticleVO getArticle(String taskId, Long userId);

    /**
     * 分页查询文章列表（非管理员只看自己的）
     */
    Page<ArticleVO> listArticle(ArticleQueryRequest request, Long userId, boolean isAdmin);

    /**
     * 删除文章（本人或管理员）
     */
    boolean deleteArticle(Long id, Long userId, boolean isAdmin);

    /**
     * 获取任务执行日志/统计
     */
    AgentExecutionStats getExecutionLogs(String taskId, Long userId);
}

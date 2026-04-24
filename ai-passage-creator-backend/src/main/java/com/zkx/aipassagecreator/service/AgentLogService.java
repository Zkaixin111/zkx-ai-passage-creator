package com.zkx.aipassagecreator.service;

import com.zkx.aipassagecreator.model.entity.AgentLog;

import java.util.List;

/**
 * 智能体执行日志服务
 * <p>
 * 日志先写入 Redis 队列（LPUSH，限长），定时任务批量落库 agent_log，
 * 减少数据库写入压力；查询走数据库。
 */
public interface AgentLogService {

    /**
     * 写入 Redis 日志队列（供 AOP 切面调用，异步落库由定时任务完成）
     */
    void enqueue(AgentLog log);

    /**
     * 定时批量落库：从 Redis 队列取出并批量插入 agent_log 表
     *
     * @return 本次落库条数
     */
    int flushQueueToDb();

    /**
     * 按任务 ID 查询执行日志
     */
    List<com.zkx.aipassagecreator.model.vo.AgentLog> listByTaskId(String taskId);
}

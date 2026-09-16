package com.zkx.aipassagecreator.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 智能体执行统计
 */
@Data
public class AgentExecutionStats implements Serializable {

    /** 任务 ID */
    private String taskId;

    /** 总耗时（毫秒） */
    private Long totalDurationMs;

    /** 智能体数量 */
    private Integer agentCount;

    /** 各智能体耗时（Map） */
    private Map<String, Long> agentDurations;

    /** 总体状态 */
    private String overallStatus;

    /** 执行日志列表 */
    private List<AgentLog> logs;

    private static final long serialVersionUID = 1L;
}

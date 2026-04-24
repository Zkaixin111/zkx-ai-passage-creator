package com.zkx.aipassagecreator.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 智能体执行统计
 */
@Data
public class AgentStatVO implements Serializable {

    /** 智能体名称 */
    private String agentName;

    /** 执行次数 */
    private Long execCount;

    /** 成功次数 */
    private Long successCount;

    /** 失败次数 */
    private Long failCount;

    /** 平均耗时（毫秒） */
    private Double avgDurationMs;

    /** 成功率（0-100） */
    private Double successRate;

    private static final long serialVersionUID = 1L;
}

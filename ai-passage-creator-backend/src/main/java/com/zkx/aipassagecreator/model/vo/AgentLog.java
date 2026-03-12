package com.zkx.aipassagecreator.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 智能体执行日志
 */
@Data
public class AgentLog implements Serializable {

    /** 日志 ID */
    private Long id;

    /** 任务 ID */
    private String taskId;

    /** 智能体名称 */
    private String agentName;

    /** 开始时间 */
    private String startTime;

    /** 结束时间 */
    private String endTime;

    /** 耗时（毫秒） */
    private Long durationMs;

    /** 状态 */
    private String status;

    /** 错误信息 */
    private String errorMessage;

    /** 使用的 Prompt */
    private String prompt;

    /** 输入数据 */
    private String inputData;

    /** 输出数据 */
    private String outputData;

    /** 创建时间 */
    private String createTime;

    /** 更新时间 */
    private String updateTime;

    private static final long serialVersionUID = 1L;
}

package com.zkx.aipassagecreator.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 智能体执行日志实体（对应 agent_log 表）
 * <p>
 * 由 {@code AgentLogAspect} 切面统一写入：先入 Redis 队列，定时批量落库。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "agent_log", camelToUnderline = false)
public class AgentLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /** 任务 ID */
    private String taskId;

    /** 智能体名称 */
    private String agentName;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 耗时（毫秒） */
    private Long durationMs;

    /** 状态：SUCCESS/FAILED */
    private String status;

    /** 错误信息 */
    private String errorMessage;

    /** 使用的 Prompt */
    private String prompt;

    /** 输入数据（JSON） */
    private String inputData;

    /** 输出数据（JSON） */
    private String outputData;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @Column(isLogicDelete = true)
    private Integer isDelete;
}

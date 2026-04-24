package com.zkx.aipassagecreator.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 系统统计分析总览（与前端 API.StatisticsVO 对齐）
 * <p>
 * 基础字段供前端统计页使用；agentStats/trend 为扩展分析数据。
 */
@Data
public class StatisticsVO implements Serializable {

    /** 今日创作数量 */
    private Long todayCount;

    /** 本周创作数量 */
    private Long weekCount;

    /** 本月创作数量 */
    private Long monthCount;

    /** 累计创作数量 */
    private Long totalCount;

    /** 成功率（0-100） */
    private Double successRate;

    /** 智能体平均耗时（毫秒） */
    private Double avgDurationMs;

    /** 今日活跃用户数 */
    private Long activeUserCount;

    /** 注册用户总数 */
    private Long totalUserCount;

    /** VIP 用户数 */
    private Long vipUserCount;

    /** 已消耗配额总量 */
    private Long quotaUsed;

    /** 各智能体执行统计（扩展） */
    private List<AgentStatVO> agentStats;

    /** 近 7 天创作趋势（扩展） */
    private List<TrendPointVO> trend;

    private static final long serialVersionUID = 1L;
}

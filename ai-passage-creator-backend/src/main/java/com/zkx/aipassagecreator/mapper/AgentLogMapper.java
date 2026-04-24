package com.zkx.aipassagecreator.mapper;

import com.mybatisflex.core.BaseMapper;
import com.zkx.aipassagecreator.model.entity.AgentLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 智能体执行日志数据访问层
 */
@Mapper
public interface AgentLogMapper extends BaseMapper<AgentLog> {

    /**
     * 按智能体聚合统计（执行次数/成功/失败/平均耗时）
     */
    @Select("SELECT agentName, COUNT(*) AS execCount, " +
            "COALESCE(SUM(status = 'SUCCESS'), 0) AS successCount, " +
            "COALESCE(SUM(status = 'FAILED'), 0) AS failCount, " +
            "COALESCE(AVG(durationMs), 0) AS avgDurationMs " +
            "FROM agent_log WHERE isDelete = 0 " +
            "GROUP BY agentName ORDER BY execCount DESC")
    List<Map<String, Object>> selectAgentStats();
}

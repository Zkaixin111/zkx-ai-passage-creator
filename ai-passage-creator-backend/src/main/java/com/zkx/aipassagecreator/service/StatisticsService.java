package com.zkx.aipassagecreator.service;

import com.zkx.aipassagecreator.model.vo.StatisticsVO;

/**
 * 系统统计分析服务
 * <p>
 * 聚合文章/用户/智能体日志数据；结果缓存 Redis（5 分钟），降低数据库查询压力。
 */
public interface StatisticsService {

    /**
     * 获取系统统计总览（Redis 缓存）
     */
    StatisticsVO getOverview();
}

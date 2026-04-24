package com.zkx.aipassagecreator.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.zkx.aipassagecreator.constant.UserConstant;
import com.zkx.aipassagecreator.mapper.AgentLogMapper;
import com.zkx.aipassagecreator.mapper.ArticleMapper;
import com.zkx.aipassagecreator.mapper.UserMapper;
import com.zkx.aipassagecreator.model.entity.Article;
import com.zkx.aipassagecreator.model.entity.User;
import com.zkx.aipassagecreator.model.vo.AgentStatVO;
import com.zkx.aipassagecreator.model.vo.StatisticsVO;
import com.zkx.aipassagecreator.model.vo.TrendPointVO;
import com.zkx.aipassagecreator.service.StatisticsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 系统统计分析服务实现
 * <p>
 * Redis 缓存 key：statistics:overview（TTL 5 分钟）
 */
@Slf4j
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final String KEY_OVERVIEW = "statistics:overview";

    private static final long CACHE_TTL_MINUTES = 5;

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private AgentLogMapper agentLogMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public StatisticsVO getOverview() {
        // 1. 读缓存
        try {
            String cached = stringRedisTemplate.opsForValue().get(KEY_OVERVIEW);
            if (StrUtil.isNotBlank(cached)) {
                return OBJECT_MAPPER.readValue(cached, StatisticsVO.class);
            }
        } catch (Exception e) {
            log.warn("统计缓存读取失败: {}", e.getMessage());
        }
        // 2. 计算
        StatisticsVO vo = compute();
        // 3. 写缓存
        try {
            stringRedisTemplate.opsForValue().set(KEY_OVERVIEW,
                    OBJECT_MAPPER.writeValueAsString(vo), CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("统计缓存写入失败: {}", e.getMessage());
        }
        return vo;
    }

    private StatisticsVO compute() {
        StatisticsVO vo = new StatisticsVO();
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        // 文章数量
        vo.setTotalCount(countArticle(null));
        vo.setTodayCount(countArticle(todayStart));
        vo.setWeekCount(countArticle(weekStart));
        vo.setMonthCount(countArticle(monthStart));

        // 成功率（COMPLETED / 总数）
        long completed = countArticleByStatus("COMPLETED");
        vo.setSuccessRate(totalPercent(completed, vo.getTotalCount()));

        // 智能体执行统计 + 平均耗时（agent_log 加权平均）
        List<AgentStatVO> agentStats = buildAgentStats();
        vo.setAgentStats(agentStats);
        vo.setAvgDurationMs(weightedAvgDuration(agentStats));

        // 用户
        try {
            vo.setActiveUserCount(articleMapper.selectActiveUserCount(todayStart));
        } catch (Exception e) {
            log.warn("活跃用户统计失败: {}", e.getMessage());
            vo.setActiveUserCount(0L);
        }
        vo.setTotalUserCount(countUser(null));
        vo.setVipUserCount(countUserByVip());
        try {
            vo.setQuotaUsed(userMapper.selectTotalQuotaUsed());
        } catch (Exception e) {
            vo.setQuotaUsed(0L);
        }

        // 近 7 天趋势（缺日期补 0）
        vo.setTrend(buildTrend(todayStart.minusDays(6)));

        return vo;
    }

    private long countArticle(LocalDateTime start) {
        QueryWrapper qw = new QueryWrapper();
        if (start != null) {
            qw.ge("createTime", start);
        }
        return articleMapper.selectCountByQuery(qw);
    }

    private long countArticleByStatus(String status) {
        QueryWrapper qw = new QueryWrapper();
        qw.eq("status", status);
        return articleMapper.selectCountByQuery(qw);
    }

    private long countUser(LocalDateTime start) {
        QueryWrapper qw = new QueryWrapper();
        if (start != null) {
            qw.ge("createTime", start);
        }
        return userMapper.selectCountByQuery(qw);
    }

    private long countUserByVip() {
        QueryWrapper qw = new QueryWrapper();
        qw.eq("vipType", UserConstant.VIP_TYPE_PERMANENT);
        return userMapper.selectCountByQuery(qw);
    }

    private List<AgentStatVO> buildAgentStats() {
        List<AgentStatVO> result = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = agentLogMapper.selectAgentStats();
            for (Map<String, Object> row : rows) {
                AgentStatVO stat = new AgentStatVO();
                stat.setAgentName(str(row.get("agentName")));
                stat.setExecCount(longVal(row.get("execCount")));
                stat.setSuccessCount(longVal(row.get("successCount")));
                stat.setFailCount(longVal(row.get("failCount")));
                stat.setAvgDurationMs(doubleVal(row.get("avgDurationMs")));
                stat.setSuccessRate(totalPercent(stat.getSuccessCount(), stat.getExecCount()));
                result.add(stat);
            }
        } catch (Exception e) {
            log.warn("智能体统计失败: {}", e.getMessage());
        }
        return result;
    }

    private Double weightedAvgDuration(List<AgentStatVO> stats) {
        if (stats == null || stats.isEmpty()) {
            return 0.0;
        }
        long total = stats.stream().mapToLong(AgentStatVO::getExecCount).sum();
        if (total == 0) {
            return 0.0;
        }
        double sum = stats.stream()
                .mapToDouble(s -> s.getAvgDurationMs() * s.getExecCount())
                .sum();
        return round2(sum / total);
    }

    private List<TrendPointVO> buildTrend(LocalDateTime start) {
        Map<String, Long> map = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = articleMapper.selectDailyTrend(start);
            for (Map<String, Object> row : rows) {
                map.put(str(row.get("date")), longVal(row.get("count")));
            }
        } catch (Exception e) {
            log.warn("趋势统计失败: {}", e.getMessage());
        }
        List<TrendPointVO> trend = new ArrayList<>();
        LocalDate day = start.toLocalDate();
        for (int i = 0; i < 7; i++) {
            String key = day.format(DAY_FORMATTER);
            TrendPointVO point = new TrendPointVO();
            point.setDate(key);
            point.setCount(map.getOrDefault(key, 0L));
            trend.add(point);
            day = day.plusDays(1);
        }
        return trend;
    }

    private Double totalPercent(long part, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return round2(part * 100.0 / total);
    }

    private Double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String str(Object v) {
        return v == null ? null : v.toString();
    }

    private Long longVal(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(v.toString());
    }

    private Double doubleVal(Object v) {
        if (v == null) {
            return 0.0;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(v.toString());
    }
}

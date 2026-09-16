package com.zkx.aipassagecreator.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mybatisflex.core.query.QueryWrapper;
import com.zkx.aipassagecreator.mapper.AgentLogMapper;
import com.zkx.aipassagecreator.model.entity.AgentLog;
import com.zkx.aipassagecreator.service.AgentLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能体执行日志服务实现
 * <p>
 * Redis 队列（agent:log:queue）承载高频写入，@Scheduled 每 10 秒批量落库；
 * 队列限长 5000，防止积压过多占用内存。
 */
@Slf4j
@Service
public class AgentLogServiceImpl implements AgentLogService {

    /** Redis 日志队列 key（List，LPUSH 写入、RPOP 消费） */
    private static final String KEY_LOG_QUEUE = "agent:log:queue";

    /** 队列最大长度 */
    private static final long MAX_QUEUE_SIZE = 5000;

    /** 每次批量落库最大条数 */
    private static final int BATCH_SIZE = 200;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private AgentLogMapper agentLogMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 序列化/反序列化 AgentLog（含 LocalDateTime 字段，必须注册 JavaTimeModule，
     * 否则 enqueue 时 LocalDateTime 序列化失败导致日志被丢弃）
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public void enqueue(AgentLog logEntity) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(logEntity);
            stringRedisTemplate.opsForList().leftPush(KEY_LOG_QUEUE, json);
            // 限长，防止队列无限增长
            stringRedisTemplate.opsForList().trim(KEY_LOG_QUEUE, 0, MAX_QUEUE_SIZE - 1);
        } catch (Exception e) {
            log.warn("智能体日志写入 Redis 队列失败: {}", e.getMessage());
        }
    }

    /**
     * 定时批量落库（每 10 秒执行一次）
     * <p>
     * RPOP 取出 → 反序列化 → insertBatch；若落库失败，将已取出的原始 JSON
     * 回退到队列尾部等待下次重试，避免数据丢失。JSON 反序列化失败的条目
     * 无法恢复，直接丢弃并记录。
     */
    @Scheduled(fixedDelay = 10_000L)
    @Override
    public int flushQueueToDb() {
        List<String> rawJsonList = new ArrayList<>(BATCH_SIZE);
        List<AgentLog> batch = new ArrayList<>(BATCH_SIZE);
        try {
            // RPOP 逐条取出并反序列化（最多 BATCH_SIZE 条）
            for (int i = 0; i < BATCH_SIZE; i++) {
                String json = stringRedisTemplate.opsForList().rightPop(KEY_LOG_QUEUE);
                if (json == null) {
                    break;
                }
                try {
                    AgentLog logEntity = OBJECT_MAPPER.readValue(json, AgentLog.class);
                    if (logEntity.getCreateTime() == null) {
                        logEntity.setCreateTime(LocalDateTime.now());
                    }
                    if (logEntity.getUpdateTime() == null) {
                        logEntity.setUpdateTime(LocalDateTime.now());
                    }
                    if (logEntity.getIsDelete() == null) {
                        logEntity.setIsDelete(0);
                    }
                    batch.add(logEntity);
                    rawJsonList.add(json);
                } catch (Exception parseEx) {
                    // JSON 反序列化失败，该条无法恢复，直接丢弃
                    log.warn("智能体日志反序列化失败，丢弃: {}", json);
                }
            }
            if (batch.isEmpty()) {
                return 0;
            }
            agentLogMapper.insertBatch(batch);
            return batch.size();
        } catch (Exception e) {
            // insertBatch 失败：将已取出的原始 JSON 回退到队列尾部，等待下次重试
            log.warn("智能体日志批量落库失败({} 条)，回退到队列: {}", rawJsonList.size(), e.getMessage());
            if (!rawJsonList.isEmpty()) {
                try {
                    stringRedisTemplate.opsForList().rightPushAll(KEY_LOG_QUEUE, rawJsonList);
                } catch (Exception pushEx) {
                    // 队列回退也失败，记录原始数据作为最后兜底
                    log.error("日志回退队列失败，{} 条日志丢失: {}", rawJsonList.size(), pushEx.getMessage());
                }
            }
            return 0;
        }
    }

    @Override
    public List<com.zkx.aipassagecreator.model.vo.AgentLog> listByTaskId(String taskId) {
        QueryWrapper qw = new QueryWrapper();
        qw.eq("taskId", taskId);
        qw.orderBy("startTime", true);
        List<AgentLog> entities = agentLogMapper.selectListByQuery(qw);
        List<com.zkx.aipassagecreator.model.vo.AgentLog> result = new ArrayList<>();
        for (AgentLog entity : entities) {
            com.zkx.aipassagecreator.model.vo.AgentLog vo = new com.zkx.aipassagecreator.model.vo.AgentLog();
            BeanUtil.copyProperties(entity, vo);
            vo.setStartTime(format(entity.getStartTime()));
            vo.setEndTime(format(entity.getEndTime()));
            vo.setCreateTime(format(entity.getCreateTime()));
            vo.setUpdateTime(format(entity.getUpdateTime()));
            result.add(vo);
        }
        return result;
    }

    private String format(LocalDateTime time) {
        return time == null ? null : time.format(FORMATTER);
    }
}

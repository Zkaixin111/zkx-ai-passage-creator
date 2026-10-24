package com.zkx.aipassagecreator.aspect;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkx.aipassagecreator.annotation.AgentLog;
import com.zkx.aipassagecreator.model.context.ArticleTaskContext;
import com.zkx.aipassagecreator.service.AgentLogService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体执行日志切面
 * <p>
 * 拦截标注 {@link AgentLog} 的执行方法，统一记录执行日志（任务 ID、智能体、耗时、
 * 状态、输入输出摘要），写入 Redis 队列由定时任务批量落库。
 */
@Slf4j
@Aspect
@Component
public class AgentLogAspect {

    /** 输入/输出数据截断长度，防止超大字段拖垮 Redis/DB（分级截断：Prompt 1000 / IO 2000 / 异常 500） */
    private static final int DATA_MAX_LEN = 2000;

    /** Prompt 字段截断长度 */
    private static final int PROMPT_MAX_LEN = 1000;

    /** 异常消息截断长度 */
    private static final int ERROR_MAX_LEN = 500;

    @Resource
    private AgentLogService agentLogService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Around("@annotation(agentLog)")
    public Object around(ProceedingJoinPoint pjp, AgentLog agentLog) throws Throwable {
        long start = System.currentTimeMillis();
        LocalDateTime startTime = LocalDateTime.now();
        String taskId = extractTaskId(pjp.getArgs());
        String agentName = StrUtil.blankToDefault(agentLog.name(), pjp.getTarget().getClass().getSimpleName());

        String status = "SUCCESS";
        String errorMessage = null;
        Object result = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable e) {
            status = "FAILED";
            errorMessage = StrUtil.maxLength(e.getMessage(), ERROR_MAX_LEN);
            throw e;
        } finally {
            try {
                // agent 内部吞掉异常时（仅 setCancelled），按 FAILED 记录
                boolean innerFailed = false;
                for (Object arg : pjp.getArgs()) {
                    if (arg instanceof ArticleTaskContext ctx && ctx.isCancelled()) {
                        innerFailed = true;
                        break;
                    }
                }
                if ("SUCCESS".equals(status) && innerFailed) {
                    status = "FAILED";
                    errorMessage = "智能体内部执行失败（已中断任务）";
                }

                com.zkx.aipassagecreator.model.entity.AgentLog logRecord = new com.zkx.aipassagecreator.model.entity.AgentLog();
                logRecord.setTaskId(taskId);
                logRecord.setAgentName(agentName);
                logRecord.setStartTime(startTime);
                logRecord.setEndTime(LocalDateTime.now());
                logRecord.setDurationMs(System.currentTimeMillis() - start);
                logRecord.setStatus(status);
                logRecord.setErrorMessage(errorMessage);
                logRecord.setPrompt(extractPrompt(pjp.getArgs(), agentName));
                logRecord.setInputData(extractInput(pjp.getArgs()));
                logRecord.setOutputData(extractOutput(pjp.getArgs(), agentName, result));
                logRecord.setCreateTime(LocalDateTime.now());
                logRecord.setUpdateTime(LocalDateTime.now());
                logRecord.setIsDelete(0);
                agentLogService.enqueue(logRecord);
            } catch (Exception e) {
                log.warn("智能体日志记录异常: {}", e.getMessage());
            }
        }
    }

    /**
     * 从方法参数提取任务 ID（智能体方法首参数均为 ArticleTaskContext）
     */
    private String extractTaskId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof ArticleTaskContext ctx) {
                return ctx.getTaskId();
            }
        }
        return null;
    }

    /**
     * 从上下文日志中取该智能体本次执行的 Prompt（agent 内部已有记录时）
     */
    private String extractPrompt(Object[] args, String agentName) {
        for (Object arg : args) {
            if (arg instanceof ArticleTaskContext ctx) {
                List<com.zkx.aipassagecreator.model.vo.AgentLog> logs = ctx.getLogs();
                if (logs != null) {
                    for (int i = logs.size() - 1; i >= 0; i--) {
                        com.zkx.aipassagecreator.model.vo.AgentLog l = logs.get(i);
                        if (agentName.equals(l.getAgentName()) && StrUtil.isNotBlank(l.getPrompt())) {
                            return StrUtil.maxLength(l.getPrompt(), PROMPT_MAX_LEN);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 输入数据摘要：上下文关键字段
     */
    private String extractInput(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof ArticleTaskContext ctx) {
                try {
                    Map<String, Object> input = new HashMap<>();
                    input.put("topic", ctx.getTopic());
                    input.put("style", ctx.getStyle());
                    input.put("mainTitle", ctx.getSelectedMainTitle());
                    input.put("subTitle", ctx.getSelectedSubTitle());
                    input.put("userDescription", ctx.getUserDescription());
                    input.put("enabledImageMethods", ctx.getEnabledImageMethods());
                    input.put("outline", ctx.getOutline());
                    return truncate(OBJECT_MAPPER.writeValueAsString(input));
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * 输出数据摘要：按智能体取上下文对应产出
     */
    private String extractOutput(Object[] args, String agentName, Object result) {
        for (Object arg : args) {
            if (arg instanceof ArticleTaskContext ctx) {
                try {
                    Object output = switch (agentName) {
                        case "TitleAgent" -> ctx.getTitleOptions();
                        case "OutlineAgent" -> ctx.getOutline();
                        case "ContentAgent" -> ctx.getContent() == null ? null : ctx.getContent().toString();
                        case "ImageAnalyzeAgent" -> ctx.getImageRequirements();
                        case "ImageFetchAgent" -> ctx.getImages();
                        case "MergeAgent" -> ctx.getFullContent();
                        default -> result;
                    };
                    if (output == null) {
                        return null;
                    }
                    return truncate(OBJECT_MAPPER.writeValueAsString(output));
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return result == null ? null : truncate(result.toString());
    }

    private String truncate(String data) {
        return data == null ? null : StrUtil.maxLength(data, DATA_MAX_LEN);
    }
}

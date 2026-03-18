package com.zkx.aipassagecreator.model.context;

import com.zkx.aipassagecreator.model.vo.AgentLog;
import com.zkx.aipassagecreator.model.vo.ImageItem;
import com.zkx.aipassagecreator.model.vo.OutlineItem;
import com.zkx.aipassagecreator.model.vo.TitleOption;
import lombok.Data;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文章任务上下文（状态容器）
 * <p>
 * 每个任务一个实例，在 6 个智能体之间传递数据，对应需求文档中的"状态类作为容器"。
 * 任务完成/失败后由编排服务清理。
 */
@Data
public class ArticleTaskContext {

    /** 任务 ID */
    private String taskId;

    /** 用户 ID */
    private Long userId;

    /** 选题 */
    private String topic;

    /** 文章风格 */
    private String style;

    /** 启用的配图方式 */
    private List<String> enabledImageMethods;

    /** 标题方案列表（智能体1产出） */
    private List<TitleOption> titleOptions = new ArrayList<>();

    /** 选中的主标题（用户确认） */
    private String selectedMainTitle;

    /** 选中的副标题（用户确认） */
    private String selectedSubTitle;

    /** 用户补充描述 */
    private String userDescription;

    /** 大纲（智能体2产出，用户可能编辑） */
    private List<OutlineItem> outline = new ArrayList<>();

    /** 正文内容（智能体3流式产出） */
    private StringBuilder content = new StringBuilder();

    /** 配图需求（智能体4产出） */
    private List<ImageItem> imageRequirements = new ArrayList<>();

    /** 配图列表（智能体5产出） */
    private List<ImageItem> images = new ArrayList<>();

    /** 完整图文（智能体6产出） */
    private String fullContent;

    /** SSE 连接（任务全生命周期复用） */
    private SseEmitter sseEmitter;

    /** 任务开始时间 */
    private long startTime;

    /** 各智能体耗时（毫秒） */
    private Map<String, Long> agentDurations = new LinkedHashMap<>();

    /** 智能体执行日志 */
    private List<AgentLog> logs = new ArrayList<>();

    /** 任务是否已终止（失败后置位） */
    private volatile boolean cancelled;

    /** 大纲生成任务是否已启动（CAS 防重，防止重复确认触发重复执行） */
    private final AtomicBoolean outlineTaskStarted = new AtomicBoolean(false);

    /** 正文生成任务是否已启动（CAS 防重，防止重复确认触发重复执行） */
    private final AtomicBoolean contentTaskStarted = new AtomicBoolean(false);

    /** 最后活动时间戳（用于任务过期检测，毫秒） */
    private volatile long lastActiveTime = System.currentTimeMillis();

    /**
     * 标记大纲生成已启动；返回 true 表示首次启动成功，false 表示已启动过（应拒绝重复操作）
     */
    public boolean markOutlineStarted() {
        return outlineTaskStarted.compareAndSet(false, true);
    }

    /**
     * 标记正文生成已启动；返回 true 表示首次启动成功，false 表示已启动过（应拒绝重复操作）
     */
    public boolean markContentStarted() {
        return contentTaskStarted.compareAndSet(false, true);
    }

    /**
     * 刷新最后活动时间（用户每次确认/查询操作时调用）
     */
    public void touch() {
        lastActiveTime = System.currentTimeMillis();
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    /**
     * 记录智能体耗时
     */
    public void recordDuration(String agentName, long durationMs) {
        agentDurations.put(agentName, durationMs);
    }

    /**
     * 添加执行日志
     */
    public void addLog(String agentName, String status, String prompt, String inputData, String outputData,
                       String errorMessage, long durationMs) {
        AgentLog log = new AgentLog();
        log.setTaskId(taskId);
        log.setAgentName(agentName);
        log.setStatus(status);
        log.setStartTime(LocalDateTime.now().minusNanos(durationMs * 1_000_000L)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.setEndTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.setDurationMs(durationMs);
        log.setPrompt(truncate(prompt, 1000));
        log.setInputData(truncate(inputData, 2000));
        log.setOutputData(truncate(outputData, 2000));
        log.setErrorMessage(truncate(errorMessage, 500));
        logs.add(log);
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}

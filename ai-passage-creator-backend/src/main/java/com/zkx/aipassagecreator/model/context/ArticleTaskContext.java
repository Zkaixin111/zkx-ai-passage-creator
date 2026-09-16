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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文章任务上下文 —— 贯穿整个任务生命周期的「共享黑板」
 * <p>
 * 每个任务一个实例，6 个智能体通过它传递数据（不是通过方法参数传，而是通过 Registry 注册表共享引用）。
 * <p>
 * 数据流向（按智能体执行顺序）：
 * <pre>
 * 输入区：topic / style / wordCount / enabledImageMethods（创建任务时设置）
 *   ↓
 * 智能体1 → titleOptions（5 个标题方案）
 *   ↓ 用户选择 → selectedMainTitle / selectedSubTitle
 *   ↓
 * 智能体2 → outline（大纲列表）
 *   ↓ 用户可能编辑 → outline（覆盖）
 *   ↓
 * 智能体3 → content（正文，StringBuilder 逐字追加）
 *   ↓ 用户确认配图
 *   ↓
 * 智能体4 → imageRequirements（每段需要什么图）
 * 智能体5 → images（实际获取到的配图）
 * 智能体6 → fullContent（图文合成后的完整内容）
 * </pre>
 * <p>
 * 任务完成/失败后由 ArticleTaskServiceImpl 从 Registry 中清理。
 */
@Data
public class ArticleTaskContext {

    // ==================== 输入区（创建任务时设置） ====================

    /** 任务 ID（UUID，贯穿全流程的唯一标识） */
    private String taskId;

    /** 用户 ID */
    private Long userId;

    /** 选题，如"AI 对教育的影响" */
    private String topic;

    /** 文章风格，如"科技风格"/"武侠"，直接填入 prompt 的 {style} 占位符 */
    private String style;

    /** 目标字数，默认 1200，填入正文 prompt 约束 AI 输出长度 */
    private int wordCount;

    /** 启用的配图方式，如 ["pexels","ai_image"]，空表示全部启用 */
    private List<String> enabledImageMethods;

    // ==================== 智能体产出区 ====================

    /** 标题方案列表（智能体1 TitleAgent 产出，5 个候选标题） */
    private List<TitleOption> titleOptions = new ArrayList<>();

    /** 选中的主标题（用户从 titleOptions 中选择后设置） */
    private String selectedMainTitle;

    /** 选中的副标题（用户确认） */
    private String selectedSubTitle;

    /** 用户补充的写作要求/描述 */
    private String userDescription;

    /** 大纲（智能体2 OutlineAgent 产出 JSON 列表，用户可能编辑后覆盖） */
    private List<OutlineItem> outline = new ArrayList<>();

    /** 正文内容（智能体3 ContentAgent 流式逐字追加到这里，用 StringBuilder 是因为频繁 append） */
    private StringBuilder content = new StringBuilder();

    /** 配图需求（智能体4 ImageAnalyzeAgent 产出，分析每段需要什么类型的图） */
    private List<ImageItem> imageRequirements = new ArrayList<>();

    /** 配图列表（智能体5 ImageFetchAgent 产出，实际获取/生成后的配图） */
    private List<ImageItem> images = new ArrayList<>();

    /** 完整图文内容（智能体6 MergeAgent 产出，正文+配图合成后的最终 Markdown） */
    private String fullContent;

    // ==================== 运行时状态区 ====================

    /** SSE 连接（任务全生命周期复用，智能体通过它推送进度消息） */
    private SseEmitter sseEmitter;

    /** 任务开始时间（用于计算总耗时） */
    private long startTime;

    /** 各智能体耗时（毫秒），key=智能体名，value=耗时。用 ConcurrentHashMap 是因为多线程（SSE 线程+智能体线程）并发读写 */
    private Map<String, Long> agentDurations = new ConcurrentHashMap<>();

    /** 智能体执行日志列表（@AgentLog AOP 切面写入，供执行日志接口查询） */
    private List<AgentLog> logs = new ArrayList<>();

    /** 任务是否已终止（AI 接口失败时置 true，后续逻辑检查此标志后跳过） */
    private volatile boolean cancelled;

    // ==================== CAS 防重标记 ====================
    // 每个标记对应一次用户确认，用 AtomicBoolean.compareAndSet 保证只执行一次

    /** 大纲生成是否已启动（confirmTitle 时 CAS，防止用户双击导致大纲重复生成） */
    private final AtomicBoolean outlineTaskStarted = new AtomicBoolean(false);

    /** 正文生成是否已启动（confirmOutline 时 CAS，防止用户双击导致正文重复生成） */
    private final AtomicBoolean contentTaskStarted = new AtomicBoolean(false);

    /** 配图/合成是否已启动（confirmContent 时 CAS，防止用户双击导致配图重复执行） */
    private final AtomicBoolean imageTaskStarted = new AtomicBoolean(false);

    // ==================== 过期检测 ====================

    /** 最后活动时间戳（用户每次确认/查询时通过 touch() 刷新，超过 30 分钟未操作则任务过期） */
    private volatile long lastActiveTime = System.currentTimeMillis();

    /**
     * CAS 标记大纲生成已启动
     * @return true=首次调用成功放行；false=已启动过，应拒绝重复操作
     */
    public boolean markOutlineStarted() {
        return outlineTaskStarted.compareAndSet(false, true);
    }

    /**
     * CAS 标记正文生成已启动
     * @return true=首次调用成功放行；false=已启动过，应拒绝重复操作
     */
    public boolean markContentStarted() {
        return contentTaskStarted.compareAndSet(false, true);
    }

    /**
     * CAS 标记配图/合成已启动
     * @return true=首次调用成功放行；false=已启动过，应拒绝重复操作
     */
    public boolean markImageTaskStarted() {
        return imageTaskStarted.compareAndSet(false, true);
    }

    /**
     * 刷新最后活动时间（用户每次确认/查询操作时调用，重置 30 分钟过期倒计时）
     */
    public void touch() {
        lastActiveTime = System.currentTimeMillis();
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    /**
     * 记录智能体执行耗时
     * @param agentName 智能体名称，如 "TitleAgent"
     * @param durationMs 执行耗时（毫秒）
     */
    public void recordDuration(String agentName, long durationMs) {
        agentDurations.put(agentName, durationMs);
    }

    /**
     * 添加智能体执行日志（供前端"执行日志"面板展示）
     * 所有文本字段会做截断（prompt 1000字、input/output 2000字、error 500字）
     */
    public void addLog(String agentName, String status, String prompt, String inputData, String outputData,
                       String errorMessage, long durationMs) {
        AgentLog logItem = new AgentLog();
        logItem.setTaskId(taskId);
        logItem.setAgentName(agentName);
        logItem.setStatus(status);
        // 通过 durationMs 反推开始时间（ endTime - durationMs）
        logItem.setStartTime(LocalDateTime.now().minusNanos(durationMs * 1_000_000L)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logItem.setEndTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logItem.setDurationMs(durationMs);
        logItem.setPrompt(truncate(prompt, 1000));      // prompt 太长会撑爆日志，截断到 1000 字
        logItem.setInputData(truncate(inputData, 2000));
        logItem.setOutputData(truncate(outputData, 2000));
        logItem.setErrorMessage(truncate(errorMessage, 500));
        logs.add(logItem);
    }

    /** 字符串截断工具方法 */
    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}

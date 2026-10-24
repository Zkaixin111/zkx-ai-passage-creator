package com.zkx.aipassagecreator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkx.aipassagecreator.constant.ArticleConstant;
import com.zkx.aipassagecreator.model.enums.SSEMessageTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 连接管理器 —— 实时进度推送的核心
 * <p>
 * 维护 taskId → SseEmitter 的映射（ConcurrentHashMap 保证多线程安全）。
 * 6 个智能体在执行过程中通过它推送进度消息给前端。
 * <p>
 * 推送的消息格式是 JSON 字符串，如：
 * <pre>
 * {"type":"AGENT3_STREAMING","content":"这是正文的一个 chunk"}
 * {"type":"TITLES_GENERATED","titleOptions":[...]}
 * {"type":"ALL_COMPLETE"}
 * </pre>
 * 前端通过 EventSource.onmessage 接收并 JSON.parse 解析。
 * <p>
 * 推送失败（前端已断开）时静默移除并忽略，不抛异常，避免影响智能体线程。
 */
@Slf4j
@Component
public class SseEmitterManager {

    /** taskId → SseEmitter 映射表。用 ConcurrentHashMap 是因为智能体线程和 SSE 连接线程并发访问 */
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 注册 SSE 连接（前端 GET /article/progress/{taskId} 时调用）
     * <p>
     * 同一 taskId 允许多个标签页同时监听：旧连接会被主动 complete 释放，避免泄漏；
     * 之后的消息统一下发到最新连接（前端重连场景天然覆盖旧连接）。
     */
    public void register(String taskId, SseEmitter emitter) {
        SseEmitter old = emitterMap.put(taskId, emitter);
        if (old != null && old != emitter) {
            try {
                old.complete();  // 释放旧连接，否则旧 SseEmitter 永不超时会一直占着资源
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 获取 SSE 连接
     */
    public SseEmitter get(String taskId) {
        return emitterMap.get(taskId);
    }

    /**
     * 移除 SSE 连接（连接结束/超时/异常时调用，防止内存泄漏）
     */
    public void remove(String taskId) {
        emitterMap.remove(taskId);
    }

    /**
     * 仅当映射里登记的仍是该 emitter 时才移除
     * <p>
     * 多标签页场景：A 标签关闭会触发 A 的 onCompletion，
     * 若按 taskId 直接删除，会把 B 标签刚注册的新连接一并删掉，
     * 导致 B 静默收不到任何消息。
     */
    public void remove(String taskId, SseEmitter emitter) {
        emitterMap.remove(taskId, emitter);
    }

    /**
     * 发送 SSE 消息 —— 所有智能体推送进度都走这个方法
     * <p>
     * 组装 JSON：{"type":"消息类型", ...extra}，通过 SseEmitter.send 推送给前端。
     * 推送失败（前端已断开）时移除 Emitter 并忽略，不抛异常。
     *
     * @param taskId 任务 ID，用于找到对应的 SseEmitter
     * @param type   消息类型（13 种，如 AGENT3_STREAMING / TITLES_GENERATED / ALL_COMPLETE）
     * @param extra  附加字段（如 content=正文 chunk, titleOptions=标题列表, outline=大纲）
     */
    public void send(String taskId, SSEMessageTypeEnum type, Map<String, Object> extra) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            // 前端还没建立连接或已断开：这类丢失会直接表现为"界面卡死"，必须留痕
            log.warn("[SSE] 推送失败：无可用连接 taskId={} type={}", taskId, type.getValue());
            return;
        }
        // 组装消息体：type + extra 合并为一个 JSON
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(ArticleConstant.SSE_TYPE, type.getValue());
        if (extra != null) {
            payload.putAll(extra);
        }
        try {
            // 序列化为 JSON 字符串通过 SSE 推送
            emitter.send(SseEmitter.event().data(OBJECT_MAPPER.writeValueAsString(payload)));
        } catch (Exception e) {
            // 连接已断开（用户关浏览器等），移除并忽略
            // 不抛异常，避免影响正在执行的智能体线程
            // 按实例移除：期间前端可能已重连并注册了新 emitter，不能误删
            emitterMap.remove(taskId, emitter);
        }
    }

    /**
     * 完成并关闭 SSE 连接（任务全部完成或过期时调用）
     */
    public void complete(String taskId) {
        SseEmitter emitter = emitterMap.remove(taskId);  // 先移除再 complete，避免重复
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 推送错误消息并关闭连接（智能体执行失败时调用）
     * 先发 ERROR 消息让前端显示错误，再 complete 关闭连接
     */
    public void error(String taskId, String message) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put(ArticleConstant.SSE_MESSAGE, message == null ? "未知错误" : message);
        send(taskId, SSEMessageTypeEnum.ERROR, extra);
        complete(taskId);
    }
}

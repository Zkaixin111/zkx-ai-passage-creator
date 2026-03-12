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
 * SSE 连接管理器
 * <p>
 * 维护 taskId → SseEmitter 的映射，负责组装 JSON 消息并推送。
 * 推送失败（连接断开）时静默忽略，避免影响智能体线程。
 */
@Slf4j
@Component
public class SseEmitterManager {

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 注册连接
     */
    public void register(String taskId, SseEmitter emitter) {
        emitterMap.put(taskId, emitter);
    }

    /**
     * 获取连接
     */
    public SseEmitter get(String taskId) {
        return emitterMap.get(taskId);
    }

    /**
     * 移除连接（连接结束/超时/异常时调用）
     */
    public void remove(String taskId) {
        emitterMap.remove(taskId);
    }

    /**
     * 发送 SSE 消息（JSON 字符串，前端 JSON.parse(event.data) 解析）
     *
     * @param type  消息类型
     * @param extra 附加字段（如 content/titleOptions/outline 等）
     */
    public void send(String taskId, SSEMessageTypeEnum type, Map<String, Object> extra) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(ArticleConstant.SSE_TYPE, type.getValue());
        if (extra != null) {
            payload.putAll(extra);
        }
        try {
            emitter.send(SseEmitter.event().data(OBJECT_MAPPER.writeValueAsString(payload)));
        } catch (Exception e) {
            // 连接已断开，移除并忽略
            emitterMap.remove(taskId);
        }
    }

    /**
     * 完成并关闭连接
     */
    public void complete(String taskId) {
        SseEmitter emitter = emitterMap.remove(taskId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 推送错误并关闭连接
     */
    public void error(String taskId, String message) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put(ArticleConstant.SSE_MESSAGE, message == null ? "未知错误" : message);
        send(taskId, SSEMessageTypeEnum.ERROR, extra);
        complete(taskId);
    }
}

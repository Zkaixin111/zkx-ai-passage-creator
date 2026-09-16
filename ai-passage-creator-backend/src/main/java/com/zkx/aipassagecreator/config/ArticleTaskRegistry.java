package com.zkx.aipassagecreator.config;

import com.zkx.aipassagecreator.model.context.ArticleTaskContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 文章任务上下文注册表
 * <p>
 * 维护 taskId → {@link ArticleTaskContext} 的共享映射。
 * <p>
 * StateGraph 的 OverAllState 会对输入数据做深拷贝（SerializationUtils.deepCopyMap），
 * 复杂对象不能直接放入状态传递（智能体写入的将是副本，原始上下文不受影响）。
 * 因此图节点只通过 taskId 从本注册表获取原始上下文引用。
 * 任务完成/失败后由编排服务清理。
 */
@Component
public class ArticleTaskRegistry {

    private final Map<String, ArticleTaskContext> contextMap = new ConcurrentHashMap<>();

    public void put(String taskId, ArticleTaskContext ctx) {
        contextMap.put(taskId, ctx);
    }

    public ArticleTaskContext get(String taskId) {
        return contextMap.get(taskId);
    }

    public void remove(String taskId) {
        contextMap.remove(taskId);
    }

    public void forEach(BiConsumer<String, ArticleTaskContext> action) {
        contextMap.forEach(action);
    }
}

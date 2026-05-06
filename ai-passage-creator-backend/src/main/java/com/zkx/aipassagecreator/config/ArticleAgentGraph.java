package com.zkx.aipassagecreator.config;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.zkx.aipassagecreator.agent.ContentAgent;
import com.zkx.aipassagecreator.agent.ImageAnalyzeAgent;
import com.zkx.aipassagecreator.agent.ImageFetchAgent;
import com.zkx.aipassagecreator.agent.MergeAgent;
import com.zkx.aipassagecreator.agent.OutlineAgent;
import com.zkx.aipassagecreator.agent.TitleAgent;
import com.zkx.aipassagecreator.model.context.ArticleTaskContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 智能体 StateGraph 编排（Spring AI Alibaba Graph）
 * <p>
 * 将原有 6 个智能体的手工链式调用重构为 DAG 图编排，按用户确认交互拆分为 4 段子图：
 * <pre>
 *   1. titleGraph   : START → TitleAgent   → END   （生成标题方案，等待用户确认）
 *   2. outlineGraph : START → OutlineAgent → END   （流式生成大纲，等待用户确认）
 *   3. contentGraph : START → ContentAgent → END   （流式生成正文，等待用户确认）
 *   4. imageGraph   : START → ImageAnalyze → ImageFetch → Merge → END
 *                    （配图分析 → 配图（并行抓图）→ 图文合成）
 * </pre>
 * 每段图独立 compile 缓存，{@link ArticleTaskContext} 对象作为状态直接放入
 * OverAllState 传递（图内不启用持久化 checkpointer，纯内存运行）。
 */
@Slf4j
@Component
public class ArticleAgentGraph {

    /** OverAllState 中存放任务上下文的键 */
    private static final String KEY_CTX = "ctx";

    private final CompiledGraph titleGraph;
    private final CompiledGraph outlineGraph;
    private final CompiledGraph contentGraph;
    private final CompiledGraph imageGraph;
    private final CompiledGraph mergeGraph;

    public ArticleAgentGraph(TitleAgent titleAgent,
                             OutlineAgent outlineAgent,
                             ContentAgent contentAgent,
                             ImageAnalyzeAgent imageAnalyzeAgent,
                             ImageFetchAgent imageFetchAgent,
                             MergeAgent mergeAgent) throws GraphStateException {
        this.titleGraph = buildTitleGraph(titleAgent);
        this.outlineGraph = buildOutlineGraph(outlineAgent);
        this.contentGraph = buildContentGraph(contentAgent);
        this.imageGraph = buildImageGraph(imageAnalyzeAgent, imageFetchAgent, mergeAgent);
        this.mergeGraph = buildMergeGraph(mergeAgent);
    }

    /** 智能体1：生成标题方案 */
    public void invokeTitle(ArticleTaskContext ctx) {
        invoke(titleGraph, ctx);
    }

    /** 智能体2：流式生成大纲 */
    public void invokeOutline(ArticleTaskContext ctx) {
        invoke(outlineGraph, ctx);
    }

    /** 智能体3：流式生成正文 */
    public void invokeContent(ArticleTaskContext ctx) {
        invoke(contentGraph, ctx);
    }

    /** 智能体4-6：配图分析 → 配图（并行）→ 图文合成 */
    public void invokeImage(ArticleTaskContext ctx) {
        invoke(imageGraph, ctx);
    }

    /** 仅智能体6：图文合成（用户选择不生成配图时直接合成纯文字） */
    public void invokeMergeOnly(ArticleTaskContext ctx) {
        invoke(mergeGraph, ctx);
    }

    // ==================== 图构建 ====================

    private CompiledGraph buildTitleGraph(TitleAgent agent) throws GraphStateException {
        StateGraph graph = new StateGraph();
        graph.addNode("title", node(agent::execute));
        graph.addEdge(StateGraph.START, "title");
        graph.addEdge("title", StateGraph.END);
        return graph.compile();
    }

    private CompiledGraph buildOutlineGraph(OutlineAgent agent) throws GraphStateException {
        StateGraph graph = new StateGraph();
        graph.addNode("outline", node(agent::execute));
        graph.addEdge(StateGraph.START, "outline");
        graph.addEdge("outline", StateGraph.END);
        return graph.compile();
    }

    private CompiledGraph buildContentGraph(ContentAgent agent) throws GraphStateException {
        StateGraph graph = new StateGraph();
        graph.addNode("content", node(agent::execute));
        graph.addEdge(StateGraph.START, "content");
        graph.addEdge("content", StateGraph.END);
        return graph.compile();
    }

    /**
     * 配图与合成图：分析 → 配图 → 合成。
     * <p>
     * 配图分析与配图生成存在数据依赖（先分析才知道要什么图），天然串行；
     * 真正可并行的是多张配图的获取，由 ImageFetchAgent 节点内部并行完成。
     */
    private CompiledGraph buildImageGraph(ImageAnalyzeAgent analyzeAgent,
                                          ImageFetchAgent fetchAgent,
                                          MergeAgent mergeAgent) throws GraphStateException {
        StateGraph graph = new StateGraph();
        graph.addNode("imageAnalyze", node(analyzeAgent::execute));
        graph.addNode("imageFetch", node(fetchAgent::execute));
        graph.addNode("merge", node(mergeAgent::execute));
        graph.addEdge(StateGraph.START, "imageAnalyze");
        graph.addEdge("imageAnalyze", "imageFetch");
        graph.addEdge("imageFetch", "merge");
        graph.addEdge("merge", StateGraph.END);
        return graph.compile();
    }

    /**
     * 仅合成图：START → Merge → END（不生成配图时直接图文合成）
     */
    private CompiledGraph buildMergeGraph(MergeAgent mergeAgent) throws GraphStateException {
        StateGraph graph = new StateGraph();
        graph.addNode("merge", node(mergeAgent::execute));
        graph.addEdge(StateGraph.START, "merge");
        graph.addEdge("merge", StateGraph.END);
        return graph.compile();
    }

    /**
     * 将智能体方法适配为 Graph 节点动作：从状态中取出任务上下文并执行。
     * <p>
     * 智能体 execute 内部自带异常兜底（失败置 cancelled + SSE 推送），
     * 节点返回空状态更新即可。
     */
    private AsyncNodeAction node(Consumer<ArticleTaskContext> executor) {
        return AsyncNodeAction.node_async(state -> {
            ArticleTaskContext ctx = state.<ArticleTaskContext>value(KEY_CTX).orElse(null);
            if (ctx == null) {
                throw new IllegalStateException("StateGraph 状态缺少任务上下文(ctx)");
            }
            executor.accept(ctx);
            return new HashMap<String, Object>();
        });
    }

    private void invoke(CompiledGraph graph, ArticleTaskContext ctx) {
        Map<String, Object> input = new HashMap<>();
        input.put(KEY_CTX, ctx);
        RunnableConfig config = RunnableConfig.builder().threadId(ctx.getTaskId()).build();
        Optional<OverAllState> result = graph.invoke(input, config);
        if (result.isEmpty()) {
            log.warn("[{}] StateGraph 执行未返回状态", ctx.getTaskId());
        }
    }
}

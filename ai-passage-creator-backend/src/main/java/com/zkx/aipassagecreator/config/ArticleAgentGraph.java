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
 * 智能体 StateGraph 编排 —— Spring AI Alibaba Graph 的 DAG 图配置
 * <p>
 * 将 6 个智能体编排为 5 段子图（构造时一次性编译并缓存，后续复用不重复编译）。
 * 按用户确认交互点拆分（每段执行完后暂停，等用户确认再启动下一段）：
 * <pre>
 * 1. titleGraph   : START → TitleAgent    → END   生成 5 个标题方案 → 用户选标题
 * 2. outlineGraph : START → OutlineAgent  → END   流式生成大纲 → 用户编辑大纲
 * 3. contentGraph : START → ContentAgent  → END   流式写正文 → 用户确认配图
 * 4. imageGraph   : START → ImageAnalyze → ImageFetch → Merge → END  配图分析→并行配图→合成
 * 5. mergeGraph   : START → MergeAgent   → END   仅合成（用户选择不配图时走这条路）
 * </pre>
 * <p>
 * 关键设计——为什么要用 Registry 而不直接把 ctx 放入 StateGraph 状态：
 * StateGraph 的 OverAllState 会对输入数据做深拷贝（SerializationUtils.deepCopyMap），
 * 导致智能体写入的是副本而非原始对象，后续读不到更新后的数据。
 * 解决方案：状态中只放 taskId（String 不受深拷贝影响），节点通过 Registry 获取原始 ctx 引用。
 */
@Slf4j
@Component
public class ArticleAgentGraph {

    /** OverAllState 中存放 taskId 的键名 */
    private static final String KEY_TASK_ID = "taskId";

    /** 任务注册表：taskId → ArticleTaskContext，节点通过它获取原始上下文引用 */
    private final ArticleTaskRegistry registry;

    // 5 段子图，构造时编译一次，后续复用（CompiledGraph 是线程安全的）
    private final CompiledGraph titleGraph;
    private final CompiledGraph outlineGraph;
    private final CompiledGraph contentGraph;
    private final CompiledGraph imageGraph;
    private final CompiledGraph mergeGraph;

    /**
     * 构造函数：Spring 注入 Registry + 6 个智能体 Bean，编译 5 段子图
     */
    public ArticleAgentGraph(ArticleTaskRegistry registry,
                             TitleAgent titleAgent,
                             OutlineAgent outlineAgent,
                             ContentAgent contentAgent,
                             ImageAnalyzeAgent imageAnalyzeAgent,
                             ImageFetchAgent imageFetchAgent,
                             MergeAgent mergeAgent) throws GraphStateException {
        this.registry = registry;
        this.titleGraph = buildTitleGraph(titleAgent);
        this.outlineGraph = buildOutlineGraph(outlineAgent);
        this.contentGraph = buildContentGraph(contentAgent);
        this.imageGraph = buildImageGraph(imageAnalyzeAgent, imageFetchAgent, mergeAgent);
        this.mergeGraph = buildMergeGraph(mergeAgent);
    }

    // ==================== 对外暴露的调用方法 ====================

    /** 智能体1：生成标题方案（5 个候选标题） */
    public void invokeTitle(ArticleTaskContext ctx) {
        invoke(titleGraph, ctx);
    }

    /** 智能体2：流式生成大纲（SSE 逐字推送） */
    public void invokeOutline(ArticleTaskContext ctx) {
        invoke(outlineGraph, ctx);
    }

    /** 智能体3：流式写正文（SSE 逐字推送，带字数约束） */
    public void invokeContent(ArticleTaskContext ctx) {
        invoke(contentGraph, ctx);
    }

    /** 智能体4-6：配图分析 → 并行配图 → 图文合成（完整配图流程） */
    public void invokeImage(ArticleTaskContext ctx) {
        invoke(imageGraph, ctx);
    }

    /** 仅智能体6：图文合成（用户选择不配图时，直接把正文拼接为最终内容） */
    public void invokeMergeOnly(ArticleTaskContext ctx) {
        invoke(mergeGraph, ctx);
    }

    // ==================== 子图构建 ====================

    /**
     * 构建标题子图：START → title → END
     */
    private CompiledGraph buildTitleGraph(TitleAgent agent) throws GraphStateException {
        StateGraph graph = new StateGraph();
        graph.addNode("title", node(agent::execute));  // 注册节点
        graph.addEdge(StateGraph.START, "title");      // 连边：起点 → title
        graph.addEdge("title", StateGraph.END);        // 连边：title → 终点
        return graph.compile();                        // 编译为可执行的 CompiledGraph
    }

    /**
     * 构建大纲子图：START → outline → END
     */
    private CompiledGraph buildOutlineGraph(OutlineAgent agent) throws GraphStateException {
        StateGraph graph = new StateGraph();
        graph.addNode("outline", node(agent::execute));
        graph.addEdge(StateGraph.START, "outline");
        graph.addEdge("outline", StateGraph.END);
        return graph.compile();
    }

    /**
     * 构建正文子图：START → content → END
     */
    private CompiledGraph buildContentGraph(ContentAgent agent) throws GraphStateException {
        StateGraph graph = new StateGraph();
        graph.addNode("content", node(agent::execute));
        graph.addEdge(StateGraph.START, "content");
        graph.addEdge("content", StateGraph.END);
        return graph.compile();
    }

    /**
     * 构建配图+合成子图：START → imageAnalyze → imageFetch → merge → END
     * <p>
     * 为什么配图分析和配图获取是串行的：
     * 配图分析产出"每段需要什么图"（ImageItem 列表），配图获取依赖这个列表才能并行抓图。
     * 真正的并行发生在 ImageFetchAgent 内部（用 CompletableFuture 并行获取多张图）。
     */
    private CompiledGraph buildImageGraph(ImageAnalyzeAgent analyzeAgent,
                                          ImageFetchAgent fetchAgent,
                                          MergeAgent mergeAgent) throws GraphStateException {
        StateGraph graph = new StateGraph();
        graph.addNode("imageAnalyze", node(analyzeAgent::execute));  // 智能体4：分析配图需求
        graph.addNode("imageFetch", node(fetchAgent::execute));      // 智能体5：并行获取配图
        graph.addNode("merge", node(mergeAgent::execute));           // 智能体6：合成图文
        graph.addEdge(StateGraph.START, "imageAnalyze");
        graph.addEdge("imageAnalyze", "imageFetch");
        graph.addEdge("imageFetch", "merge");
        graph.addEdge("merge", StateGraph.END);
        return graph.compile();
    }

    /**
     * 构建仅合成子图：START → merge → END
     * 用户选择不配图时走这条路，只执行合成节点（把正文拼接为最终 Markdown）
     */
    private CompiledGraph buildMergeGraph(MergeAgent mergeAgent) throws GraphStateException {
        StateGraph graph = new StateGraph();
        graph.addNode("merge", node(mergeAgent::execute));
        graph.addEdge(StateGraph.START, "merge");
        graph.addEdge("merge", StateGraph.END);
        return graph.compile();
    }

    // ==================== 核心适配器 ====================

    /**
     * 将智能体的 execute 方法适配为 StateGraph 节点动作
     * <p>
     * 核心问题：OverAllState 会深拷贝输入，如果直接把 ctx 放进状态，
     * 智能体修改的是副本，后续读不到更新。
     * <p>
     * 解决方案：状态里只放 taskId（String 不受深拷贝影响），
     * 节点执行时通过 Registry.get(taskId) 拿到原始 ctx 引用，智能体直接写原始对象。
     * <p>
     * 智能体 execute 方法自带异常兜底（失败时设 cancelled=true + SSE 推送错误），
     * 节点只需执行后返回空 Map（不需要往状态里写数据，因为数据都在 ctx 里）。
     *
     * @param executor 智能体的 execute 方法引用，如 titleAgent::execute
     */
    private AsyncNodeAction node(Consumer<ArticleTaskContext> executor) {
        return AsyncNodeAction.node_async(state -> {
            // 从 StateGraph 状态中取出 taskId
            String taskId = state.<String>value(KEY_TASK_ID).orElse(null);
            // 从 Registry 取出原始 ctx 引用（不是副本！）
            ArticleTaskContext ctx = taskId == null ? null : registry.get(taskId);
            if (ctx == null) {
                throw new IllegalStateException("StateGraph 状态缺少任务上下文(taskId=" + taskId + ")");
            }
            // 执行智能体逻辑（智能体内部直接修改 ctx，因为这是原始引用）
            executor.accept(ctx);
            // 返回空 Map：不需要往 StateGraph 状态写数据，所有数据都在 ctx 里
            return new HashMap<String, Object>();
        });
    }

    /**
     * 执行一段子图
     * <p>
     * 1. 把 taskId 放入输入 Map（状态中只传 taskId）
     * 2. 用 taskId 作为线程 ID 构建 RunnableConfig（StateGraph 用 threadId 区分不同任务的执行状态）
     * 3. 调用 graph.invoke 同步执行（虽然是同步调用，但智能体内部的流式输出是异步推送 SSE 的）
     */
    private void invoke(CompiledGraph graph, ArticleTaskContext ctx) {
        Map<String, Object> input = new HashMap<>();
        input.put(KEY_TASK_ID, ctx.getTaskId());
        RunnableConfig config = RunnableConfig.builder().threadId(ctx.getTaskId()).build();
        Optional<OverAllState> result = graph.invoke(input, config);
        if (result.isEmpty()) {
            log.warn("[{}] StateGraph 执行未返回状态", ctx.getTaskId());
        }
    }
}

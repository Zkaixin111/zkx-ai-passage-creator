package com.zkx.aipassagecreator.constant;

import java.util.Map;

/**
 * 智能体 Prompt 模板常量 —— Prompt 工程集中管理中心
 * <p>
 * <h3>为什么集中管理？</h3>
 * <p>Prompt 散落在各智能体类里会导致：修改 Prompt 要翻多个文件、
 * 无法统一对比格式约束、无法快速迭代调优。集中到常量类后，
 * 所有 Prompt 一目了然，调优只改这一个文件，不用动业务代码。</p>
 *
 * <h3>Prompt 工程设计原则</h3>
 * <ol>
 *   <li><b>角色设定</b>：每个 Prompt 开头先给 LLM 一个角色（"你是资深标题专家"），
 *       让 LLM 进入对应专业领域的输出模式</li>
 *   <li><b>输入上下文</b>：用 {占位符} 注入用户输入（选题、风格、大纲等），
 *       让 LLM 知道基于什么内容工作</li>
 *   <li><b>约束输出格式</b>：明确要求"只返回 JSON 数组"或"直接输出 Markdown"，
 *       并给出示例格式——这是最关键的一行，减少 LLM 加废话的概率</li>
 *   <li><b>禁止多余输出</b>：每条都加"不要返回任何其他内容，不要用 markdown 代码块包裹"，
 *       因为 LLM 经常自作主张加 ```json 包裹或加"好的，以下是结果"等前缀</li>
 *   <li><b>数量/字数约束</b>：限制标题数量、章节数、配图数量、正文字数，
 *       让输出可预期、可控</li>
 * </ol>
 *
 * <h3>模板渲染机制</h3>
 * <p>用 {key} 占位符 + String.replace() 简单替换，不用复杂模板引擎。
 * 原因：Prompt 模板的变量不多（2-5 个），replace 够用且零依赖。</p>
 *
 * <h3>面试考点</h3>
 * <ul>
 *   <li>"Prompt 怎么管理的？" → 5 套模板集中常量类，{占位符}动态替换，配置化不硬编码</li>
 *   <li>"LLM 输出不稳定怎么办？" → ①Prompt 里严格约束格式 ②JsonExtractUtil 三层容错解析</li>
 *   <li>"Prompt 怎么迭代调优？" → 改常量类即可，不用改业务代码，便于 A/B 测试不同 Prompt</li>
 * </ul>
 */
public class PromptConstant {

    private PromptConstant() {
    }

    /**
     * 渲染模板：将 {key} 占位符替换为变量值
     * <p>用 String.replace 逐个替换，不用正则（{topic} 里没有正则特殊字符）。
     * value 为 null 时替换为空串，防止 LLM 看到 "{null}" 这种字面量。
     */
    public static String render(String template, Map<String, String> vars) {
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    // ==================== 智能体1：生成标题 ====================

    /**
     * 智能体1 Prompt：生成标题方案
     * <p>角色：新媒体爆款标题专家<br>
     * 输入：{topic} 选题 + {style} 文章风格<br>
     * 输出：JSON 数组 [{mainTitle, subTitle}]，数量由 ArticleConstant.TITLE_OPTION_COUNT 控制<br>
     * <p>
     * <b>Prompt 工程要点：</b>"只返回 JSON 数组" + "不要用 markdown 代码块包裹" ——
     * 这两行是关键约束，LLM 经常自作主张加 ```json 包裹或加前缀文字，
     * 加了约束能大幅降低解析失败率（JsonExtractUtil 兜底但不该依赖它）。
     */
    public static final String TITLE_PROMPT = """
            你是一位资深的新媒体爆款标题专家。请根据以下选题，生成 %d 个爆款标题方案，每个方案包含主标题和副标题。

            选题：{topic}
            文章风格：{style}

            要求：
            1. 标题要抓人眼球、有传播力，符合指定的文章风格
            2. 主标题简洁有力，副标题补充信息、增强吸引力
            3. 只返回 JSON 数组，格式：[{"mainTitle":"主标题","subTitle":"副标题"}]
            4. 不要返回任何其他内容，不要用 markdown 代码块包裹
            """.formatted(ArticleConstant.TITLE_OPTION_COUNT);

    // ==================== 智能体2：生成大纲 ====================

    /**
     * 智能体2 Prompt：生成文章大纲
     * <p>角色：文章架构师<br>
     * 输入：{mainTitle} + {subTitle} + {userDescription}<br>
     * 输出：JSON 数组 [{section, title, points[]}]，3-6 个章节，每章 2-4 个要点<br>
     * <p>
     * <b>为什么限制 3-6 章节？</b>—— 少于 3 章内容太薄，多于 6 章用户编辑负担重，
     * 3-6 是平衡"内容丰富度"和"用户编辑体验"的折中。
     */
    public static final String OUTLINE_PROMPT = """
            你是一位擅长结构规划的文章架构师。请根据以下标题，生成一篇完整文章的大纲。

            主标题：{mainTitle}
            副标题：{subTitle}
            用户补充描述：{userDescription}

            要求：
            1. 大纲包含 3-6 个章节，逻辑清晰、层层递进
            2. 每个章节包含标题和 2-4 个要点
            3. 只返回 JSON 数组，格式：[{"section":1,"title":"章节标题","points":["要点1","要点2"]}]
            4. 不要返回任何其他内容，不要用 markdown 代码块包裹
            """;

    // ==================== 智能体3：撰写正文 ====================

    /**
     * 智能体3 Prompt：撰写完整正文
     * <p>角色：写作专家<br>
     * 输入：{outline} 大纲 JSON + {wordCount} 目标字数<br>
     * 输出：纯 Markdown 正文（不是 JSON），因为正文是给用户看的最终内容<br>
     * <p>
     * <b>与智能体1/2/4 的关键区别：</b>这个 Prompt 要求"直接输出 Markdown"而非 JSON，
     * 因为正文是流式推送给用户的（SSE 逐 chunk），用户实时看到内容在"打字"。
     * 如果输出 JSON 再解析渲染，就没法流式了。
     * <p>
     * <b>字数约束："约 {wordCount} 字（允许上下浮动 10%）"</b>——
     * LLM 对字数控制不精确，给 10% 浮动空间更实际，硬要求反而导致内容质量下降。
     */
    public static final String CONTENT_PROMPT = """
            你是一位文笔优秀的写作专家。请根据以下大纲，撰写一篇完整的 Markdown 格式文章。

            大纲（JSON）：{outline}

            要求：
            1. 严格按照大纲的章节结构和要点逐段展开，每个章节用 "## " 标题开头
            2. 开头要有吸引人的引言，结尾要有总结或升华
            3. 内容详实、有深度、语言流畅自然，适当使用加粗、列表等 Markdown 语法
            4. 目标字数：约 {wordCount} 字（允许上下浮动 10%）
            5. 直接输出 Markdown 正文，不要输出任何解释性文字
            """;

    /**
     * 智能体3 分段 Prompt：一次只撰写大纲中的一个章节
     * <p>
     * 为什么分段？整篇一次性生成需要 30-60 秒，用户只能看着光标干等；
     * 分段后每段 5-10 秒，段落完成立即推送 SECTION_COMPLETE 事件，
     * 前端可以按段落展示"第 X/Y 段已就绪"，等待体感大幅降低。
     * <p>
     * 输入：全文上下文（选题/标题/大纲）+ 本章定位（序号/标题/要点）+ 本章字数
     * 输出：仅本章 Markdown（以 "## 章节标题" 开头）
     * <p>
     * {positionHint} 用于首尾章的特殊要求：第一章带引言、最后一章带总结升华，
     * 由 ContentAgent 按 sectionIndex 动态填充，中间章节为空串。
     */
    public static final String CONTENT_SECTION_PROMPT = """
            你是一位文笔优秀的写作专家。请根据以下信息，只撰写文章的一个章节（Markdown 格式）。

            文章选题：{topic}
            文章风格：{style}
            主标题：{mainTitle}
            副标题：{subTitle}
            全文大纲（JSON）：{outline}

            本章序号：第 {sectionIndex} / {totalSections} 章
            本章标题：{sectionTitle}
            本章要点：{sectionPoints}

            要求：
            1. 只撰写本章内容，以 "## {sectionTitle}" 作为开头
            2. 紧扣本章要点展开，内容详实、有深度、语言流畅自然，适当使用加粗、列表等 Markdown 语法
            3. 本章目标字数：约 {sectionWordCount} 字（允许上下浮动 15%）
            4. 与其他章节保持连贯，但不要撰写其他章节的内容
            {positionHint}
            5. 直接输出本章 Markdown 正文，不要输出任何解释性文字
            """;

    // ==================== 智能体4：分析配图 ====================

    /**
     * 智能体4 Prompt：分析正文，规划配图方案
     * <p>角色：图文编辑专家<br>
     * 输入：{content} 正文全文<br>
     * 输出：JSON 数组 [{position, keywords, sectionTitle, description}]<br>
     * <p>
     * <b>Prompt 工程要点：</b>
     * "keywords 必须是英文" —— 因为关键词用于 Pexels 图库检索，Pexels 是英文平台，
     * 中文关键词搜不到好图。强制 LLM 输出英文关键词是跨语言检索的关键。
     * <p>
     * "position 从 1 开始" —— 对应正文章节顺序，MergeAgent 据此把配图插入正确位置。
     */
    public static final String IMAGE_ANALYZE_PROMPT = """
            你是一位图文编辑专家。请分析以下文章正文，规划配图方案。

            正文：{content}

            要求：
            1. 找出需要配图的章节位置（position 从 1 开始，对应正文章节顺序）
            2. 为每张配图生成英文搜索关键词（用于 Pexels 图库检索，必须是英文）和中文描述
            3. 配图数量建议 3-5 张，避免过多
            4. 只返回 JSON 数组，格式：[{"position":1,"keywords":"english keywords","sectionTitle":"章节标题","description":"图片描述"}]
            5. 不要返回任何其他内容，不要用 markdown 代码块包裹
            """;

    // ==================== 智能体：AI 修改大纲 ====================

    /**
     * AI 修改大纲 Prompt：用户输入自然语言修改建议，LLM 重新生成大纲
     * <p>输入：{outline} 当前大纲 JSON + {suggestion} 用户修改建议（自然语言）<br>
     * 输出：和 OUTLINE_PROMPT 相同格式的 JSON 数组<br>
     * <p>
     * <b>设计意图：</b>用户不需要手动编辑大纲 JSON，用自然语言说"加一个关于XX的章节"、
     * "把第三章拆成两章"，LLM 理解意图后重新生成完整大纲。这是 Human-in-the-Loop 的增强：
     * 用户既可以直接编辑大纲（手动模式），也可以让 AI 帮改（智能模式）。
     */
    public static final String AI_MODIFY_OUTLINE_PROMPT = """
            你是一位文章架构师。用户希望对现有大纲进行修改，请按修改建议重新生成完整大纲。

            当前大纲（JSON）：{outline}
            修改建议：{suggestion}

            要求：
            1. 保留合理的章节，按建议调整结构和内容
            2. 大纲包含 3-6 个章节，每章节含标题和 2-4 个要点
            3. 只返回 JSON 数组，格式：[{"section":1,"title":"章节标题","points":["要点1","要点2"]}]
            4. 不要返回任何其他内容，不要用 markdown 代码块包裹
            """;
}

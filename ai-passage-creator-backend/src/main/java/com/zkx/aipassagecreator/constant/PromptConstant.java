package com.zkx.aipassagecreator.constant;

import java.util.Map;

/**
 * 智能体 Prompt 模板常量
 * <p>
 * 集中管理所有智能体的 Prompt 模板，采用 {占位符} 动态替换。
 * 每个模板都严格约束输出格式（纯 JSON 或 Markdown），减少解析失败。
 */
public class PromptConstant {

    private PromptConstant() {
    }

    /**
     * 渲染模板：将 {key} 占位符替换为变量值
     */
    public static String render(String template, Map<String, String> vars) {
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    // ==================== 智能体1：生成标题 ====================

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

    public static final String CONTENT_PROMPT = """
            你是一位文笔优秀的写作专家。请根据以下大纲，撰写一篇完整的 Markdown 格式文章。

            大纲（JSON）：{outline}

            要求：
            1. 严格按照大纲的章节结构和要点逐段展开，每个章节用 "## " 标题开头
            2. 开头要有吸引人的引言，结尾要有总结或升华
            3. 内容详实、有深度、语言流畅自然，适当使用加粗、列表等 Markdown 语法
            4. 直接输出 Markdown 正文，不要输出任何解释性文字
            """;

    // ==================== 智能体4：分析配图 ====================

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

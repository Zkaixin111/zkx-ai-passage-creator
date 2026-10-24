package com.zkx.aipassagecreator.constant;

/**
 * 文章生成模块常量
 * <p>
 * 统一管理任务超时、图片搜索配置、SSE 推送字段名等。
 */
public class ArticleConstant {

    private ArticleConstant() {
    }

    /** 任务总超时时间（毫秒），30 分钟 */
    public static final long TASK_TIMEOUT_MS = 30 * 60 * 1000L;

    /** Pexels API 搜索地址 */
    public static final String PEXELS_API_URL = "https://api.pexels.com/v1/search";

    /** Picsum 随机图兜底地址（{seed} 占位符保证同关键词可复现） */
    public static final String PICSUM_API_URL = "https://picsum.photos/seed/{seed}/800/600";

    /** Iconify 图标搜索 API */
    public static final String ICONIFY_SEARCH_URL = "https://api.iconify.design/search?query={query}&limit=1";

    /** Iconify 图标文件地址（{icon} 为图标名，如 mdi:rocket） */
    public static final String ICONIFY_ICON_URL = "https://api.iconify.design/{icon}.svg?width=800&height=500";

    /** Twemoji 表情包地址（{codepoint} 为 emoji 的 unicode 码点，如 1f604；强制横图尺寸） */
    public static final String TWEMOJI_URL = "https://api.iconify.design/twemoji/{codepoint}.svg?width=800&height=500";

    /** Mermaid 在线渲染地址（{code} 为 base64url 编码的 mermaid 代码；强制横图尺寸） */
    public static final String MERMAID_RENDER_URL = "https://mermaid.ink/img/{code}?type=png&width=800&height=500";

    /** DashScope 文生图接口（异步任务） */
    public static final String DASHSCOPE_T2I_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis";

    /** DashScope 异步任务查询接口 */
    public static final String DASHSCOPE_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/{taskId}";

    /** DashScope 文生图接口（OpenAI 兼容，同步，qwen-image-3.0-pro） */
    public static final String DASHSCOPE_IMAGE_GEN_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/images/generations";

    /** 标题方案生成数量 */
    public static final int TITLE_OPTION_COUNT = 3;

    /** 默认目标字数 */
    public static final int DEFAULT_WORD_COUNT = 1200;

    /** 目标字数上限：防止前端传入超大字数导致 token 成本与生成耗时失控 */
    public static final int MAX_WORD_COUNT = 5000;

    /** 默认配图数量上限 */
    public static final int DEFAULT_IMAGE_COUNT = 5;

    // ==================== SSE 推送消息字段名 ====================

    /** 消息类型 */
    public static final String SSE_TYPE = "type";

    /** 流式内容片段 */
    public static final String SSE_CONTENT = "content";

    /** 标题方案列表 */
    public static final String SSE_TITLE_OPTIONS = "titleOptions";

    /** 大纲列表 */
    public static final String SSE_OUTLINE = "outline";

    /** 配图需求列表 */
    public static final String SSE_IMAGE_REQUIREMENTS = "imageRequirements";

    /** 配图列表 */
    public static final String SSE_IMAGES = "images";

    /** 完整图文内容 */
    public static final String SSE_FULL_CONTENT = "fullContent";

    /** 错误信息 */
    public static final String SSE_MESSAGE = "message";

    /** 当前段落序号（从 0 开始） */
    public static final String SSE_SECTION_INDEX = "sectionIndex";

    /** 段落总数 */
    public static final String SSE_TOTAL_SECTIONS = "totalSections";

    /** 当前段落标题 */
    public static final String SSE_SECTION_TITLE = "sectionTitle";
}

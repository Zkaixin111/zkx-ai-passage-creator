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

    /** 标题方案生成数量 */
    public static final int TITLE_OPTION_COUNT = 3;

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
}

package com.zkx.aipassagecreator.model.enums;

import lombok.Getter;

/**
 * SSE 消息类型枚举
 * <p>
 * 与前端 {@code ArticleCreatePage.vue} 的 handleSSEMessage 分发逻辑一一对应，
 * 类型名必须与前端完全一致，否则前端无法识别。
 */
@Getter
public enum SSEMessageTypeEnum {

    /** 智能体1（标题）完成 */
    AGENT1_COMPLETE("AGENT1_COMPLETE", "标题方案生成完成"),
    /** 标题方案就绪（携带 titleOptions） */
    TITLES_GENERATED("TITLES_GENERATED", "标题方案生成完成"),
    /** 大纲流式输出（携带 content） */
    AGENT2_STREAMING("AGENT2_STREAMING", "大纲流式输出"),
    /** 大纲生成完成（携带 outline） */
    OUTLINE_GENERATED("OUTLINE_GENERATED", "大纲生成完成"),
    /** 智能体2（大纲）内部完成 */
    AGENT2_COMPLETE("AGENT2_COMPLETE", "大纲完成"),
    /** 正文流式输出（携带 content） */
    AGENT3_STREAMING("AGENT3_STREAMING", "正文流式输出"),
    /** 智能体3（正文）完成 */
    AGENT3_COMPLETE("AGENT3_COMPLETE", "正文完成"),
    /** 智能体4（配图分析）完成（携带 imageRequirements） */
    AGENT4_COMPLETE("AGENT4_COMPLETE", "配图分析完成"),
    /** 单张配图完成 */
    IMAGE_COMPLETE("IMAGE_COMPLETE", "单张配图完成"),
    /** 智能体5（配图）完成（携带 images） */
    AGENT5_COMPLETE("AGENT5_COMPLETE", "配图生成完成"),
    /** 图文合成完成（携带 fullContent） */
    MERGE_COMPLETE("MERGE_COMPLETE", "图文合成完成"),
    /** 全部完成 */
    ALL_COMPLETE("ALL_COMPLETE", "全部完成"),
    /** 错误（携带 message） */
    ERROR("ERROR", "错误");

    private final String value;

    private final String text;

    SSEMessageTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}

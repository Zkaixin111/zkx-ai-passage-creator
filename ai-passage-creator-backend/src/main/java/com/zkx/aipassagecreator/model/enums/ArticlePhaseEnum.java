package com.zkx.aipassagecreator.model.enums;

import lombok.Getter;

/**
 * 文章生成阶段枚举（对应前端 currentPhase，存入 article.phase）
 */
@Getter
public enum ArticlePhaseEnum {

    TITLE_GENERATING("TITLE_GENERATING", "标题生成中"),
    TITLE_SELECTING("TITLE_SELECTING", "标题选择"),
    OUTLINE_GENERATING("OUTLINE_GENERATING", "大纲生成中"),
    OUTLINE_EDITING("OUTLINE_EDITING", "大纲编辑"),
    CONTENT_GENERATING("CONTENT_GENERATING", "正文生成中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败");

    private final String value;

    private final String text;

    ArticlePhaseEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}

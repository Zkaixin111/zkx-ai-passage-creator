package com.zkx.aipassagecreator.model.enums;

import lombok.Getter;

/**
 * 文章状态枚举（对应 article.status）
 */
@Getter
public enum ArticleStatusEnum {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败");

    private final String value;

    private final String text;

    ArticleStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     */
    public static ArticleStatusEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ArticleStatusEnum anEnum : ArticleStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

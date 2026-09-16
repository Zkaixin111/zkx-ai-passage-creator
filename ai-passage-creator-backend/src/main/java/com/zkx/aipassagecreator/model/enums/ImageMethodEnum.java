package com.zkx.aipassagecreator.model.enums;

import lombok.Getter;

/**
 * 配图方式枚举
 * <p>
 * MVP 阶段仅实现 PEXELS（含 Picsum 兜底），其余方式预留，
 * 与前端 {@code ArticleCreatePage.vue} 的配图方式选项保持一致。
 */
@Getter
public enum ImageMethodEnum {

    PEXELS("PEXELS", "Pexels 图库"),
    PICSUM("PICSUM", "Picsum 随机图(兜底)"),
    NANO_BANANA("NANO_BANANA", "AI 生图"),
    MERMAID("MERMAID", "Mermaid 图表"),
    ICONIFY("ICONIFY", "Iconify 图标"),
    EMOJI_PACK("EMOJI_PACK", "表情包"),
    SVG_DIAGRAM("SVG_DIAGRAM", "SVG 图表");

    private final String value;

    private final String text;

    ImageMethodEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     */
    public static ImageMethodEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ImageMethodEnum anEnum : ImageMethodEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

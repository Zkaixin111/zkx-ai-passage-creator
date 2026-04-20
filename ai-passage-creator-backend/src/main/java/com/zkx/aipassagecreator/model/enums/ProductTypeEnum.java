package com.zkx.aipassagecreator.model.enums;

import lombok.Getter;

/**
 * 商品类型枚举（当前仅支持 VIP 永久会员）
 */
@Getter
public enum ProductTypeEnum {

    VIP_PERMANENT("VIP_PERMANENT", "VIP 永久会员");

    private final String value;

    private final String text;

    ProductTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}

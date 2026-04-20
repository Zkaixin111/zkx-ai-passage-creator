package com.zkx.aipassagecreator.model.enums;

import lombok.Getter;

/**
 * 支付状态枚举（状态机：PENDING -> PAID 单向流转，幂等入账）
 */
@Getter
public enum PaymentStatusEnum {

    PENDING("PENDING", "待支付"),
    PAID("PAID", "已支付"),
    FAILED("FAILED", "失败"),
    CLOSED("CLOSED", "已关闭"),
    REFUNDED("REFUNDED", "已退款");

    private final String value;

    private final String text;

    PaymentStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}

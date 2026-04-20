package com.zkx.aipassagecreator.model.enums;

import lombok.Getter;

/**
 * 支付渠道枚举
 */
@Getter
public enum PaymentChannelEnum {

    WECHAT("WECHAT", "微信支付"),
    ALIPAY("ALIPAY", "支付宝");

    private final String value;

    private final String text;

    PaymentChannelEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static PaymentChannelEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (PaymentChannelEnum anEnum : PaymentChannelEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

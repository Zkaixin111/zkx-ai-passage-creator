package com.zkx.aipassagecreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 支付配置（微信/支付宝）
 * <p>
 * 对应 application.yaml 的 pay 段。敏感信息建议通过环境变量注入：
 * PAY_WECHAT_APPID / PAY_WECHAT_MCHID / PAY_WECHAT_APIV3KEY 等。
 */
@Data
@Component
@ConfigurationProperties(prefix = "pay")
public class PaymentProperties {

    /** VIP 永久会员价格（元） */
    private BigDecimal vipPrice = new BigDecimal("29.90");

    /** 回调基础地址（公网可访问，如 https://api.example.com/api），用于拼接完整回调 URL */
    private String notifyBaseUrl = "http://localhost:8123/api";

    private Wechat wechat = new Wechat();

    private Alipay alipay = new Alipay();

    @Data
    public static class Wechat {
        /** 微信开放平台 AppID（小程序/公众号） */
        private String appid;
        /** 商户号 */
        private String mchid;
        /** APIv3 密钥 */
        private String apiV3Key;
        /** 商户 API 证书序列号 */
        private String merchantSerialNo;
        /** 商户 API 私钥文件路径（如 classpath:cert/apiclient_key.pem 或绝对路径） */
        private String privateKeyPath;
    }

    @Data
    public static class Alipay {
        /** 支付宝开放平台 AppID */
        private String appId;
        /** 应用私钥（PKCS8，含换行用 \n 或改环境变量） */
        private String privateKey;
        /** 支付宝公钥 */
        private String alipayPublicKey;
        /** 网关（沙箱可改为 https://openapi-sandbox.dl.alipaydev.com/gateway.do） */
        private String gateway = "https://openapi.alipay.com/gateway.do";
    }
}

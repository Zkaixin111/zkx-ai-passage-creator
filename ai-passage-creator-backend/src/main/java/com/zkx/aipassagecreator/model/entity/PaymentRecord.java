package com.zkx.aipassagecreator.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单实体（支付宝）
 * <p>
 * 对应 payment_record 表。orderNo/channelOrderNo 双唯一索引保证下单与回调幂等。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "payment_record", camelToUnderline = false)
public class PaymentRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /** 业务订单号（幂等唯一） */
    private String orderNo;

    /** 用户 ID */
    private Long userId;

    /** 支付渠道：ALIPAY */
    private String channel;

    /** 商品类型：VIP_PERMANENT */
    private String productType;

    /** 商品标题 */
    private String subject;

    /** 实付金额（元） */
    private BigDecimal amount;

    /** 货币 */
    private String currency;

    /** 状态：PENDING/PAID/FAILED/CLOSED/REFUNDED */
    private String status;

    /** 渠道单号（支付宝 trade_no，幂等唯一） */
    private String channelOrderNo;

    /** 支付回调原始报文（对账排查用） */
    private String notifyData;

    /** 支付成功时间 */
    private LocalDateTime payTime;

    /** 退款时间 */
    private LocalDateTime refundTime;

    /** 退款原因 */
    private String refundReason;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @Column(isLogicDelete = true)
    private Integer isDelete;
}

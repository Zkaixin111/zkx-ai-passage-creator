package com.zkx.aipassagecreator.mapper;

import com.mybatisflex.core.BaseMapper;
import com.zkx.aipassagecreator.model.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付订单数据访问层
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}

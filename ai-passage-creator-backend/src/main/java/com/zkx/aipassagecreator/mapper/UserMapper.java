package com.zkx.aipassagecreator.mapper;

import com.mybatisflex.core.BaseMapper;
import com.zkx.aipassagecreator.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户数据访问层
 * <p>
 * 继承 MyBatis-Flex BaseMapper，自动获得基本 CRUD 能力。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 原子扣减创作配额（仅当仍有剩余额度时扣减成功）
     * <p>
     * 并发安全：靠 WHERE quotaUsed &lt; quotaTotal 条件保证不会超扣，
     * 返回影响行数：1=扣减成功，0=配额已用完。
     */
    @Update("UPDATE user SET quotaUsed = quotaUsed + 1, updateTime = NOW() " +
            "WHERE id = #{userId} AND quotaUsed < quotaTotal")
    int deductQuota(@Param("userId") Long userId);

    /**
     * 退还一次创作配额（任务创建失败时补偿，避免配额白扣）
     * <p>
     * quotaUsed &gt; 0 条件保证不会把已用配额退成负数。
     */
    @Update("UPDATE user SET quotaUsed = quotaUsed - 1, updateTime = NOW() " +
            "WHERE id = #{userId} AND quotaUsed > 0")
    int refundQuota(@Param("userId") Long userId);

    /**
     * 已消耗配额总量
     */
    @Select("SELECT COALESCE(SUM(quotaUsed), 0) FROM user WHERE isDelete = 0")
    Long selectTotalQuotaUsed();
}

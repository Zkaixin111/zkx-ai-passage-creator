package com.zkx.aipassagecreator.mapper;

import com.mybatisflex.core.BaseMapper;
import com.zkx.aipassagecreator.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层
 * <p>
 * 继承 MyBatis-Flex BaseMapper，自动获得基本 CRUD 能力。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}

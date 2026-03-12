package com.zkx.aipassagecreator.mapper;

import com.mybatisflex.core.BaseMapper;
import com.zkx.aipassagecreator.model.entity.Article;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章数据访问层
 * <p>
 * 继承 MyBatis-Flex BaseMapper，自动获得基本 CRUD 能力。
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}

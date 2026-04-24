package com.zkx.aipassagecreator.mapper;

import com.mybatisflex.core.BaseMapper;
import com.zkx.aipassagecreator.model.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文章数据访问层
 * <p>
 * 继承 MyBatis-Flex BaseMapper，自动获得基本 CRUD 能力。
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 指定时间之后的活跃用户数（去重）
     */
    @Select("SELECT COUNT(DISTINCT userId) FROM article WHERE createTime >= #{start} AND isDelete = 0")
    Long selectActiveUserCount(@Param("start") LocalDateTime start);

    /**
     * 指定时间之后的每日创作趋势
     */
    @Select("SELECT DATE_FORMAT(createTime, '%Y-%m-%d') AS date, COUNT(*) AS count " +
            "FROM article WHERE createTime >= #{start} AND isDelete = 0 " +
            "GROUP BY DATE(createTime) ORDER BY date")
    List<Map<String, Object>> selectDailyTrend(@Param("start") LocalDateTime start);
}

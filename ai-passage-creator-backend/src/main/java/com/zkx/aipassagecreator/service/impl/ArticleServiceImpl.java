package com.zkx.aipassagecreator.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.logicdelete.LogicDeleteManager;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zkx.aipassagecreator.exception.BusinessException;
import com.zkx.aipassagecreator.exception.ErrorCode;
import com.zkx.aipassagecreator.mapper.ArticleMapper;
import com.zkx.aipassagecreator.model.dto.ArticleQueryRequest;
import com.zkx.aipassagecreator.model.entity.Article;
import com.zkx.aipassagecreator.model.vo.AgentExecutionStats;
import com.zkx.aipassagecreator.model.vo.ArticleVO;
import com.zkx.aipassagecreator.model.vo.ImageItem;
import com.zkx.aipassagecreator.model.vo.OutlineItem;
import com.zkx.aipassagecreator.model.vo.TitleOption;
import com.zkx.aipassagecreator.service.ArticleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 文章服务实现
 */
@Service
public class ArticleServiceImpl implements ArticleService {

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private ArticleTaskServiceImpl articleTaskService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public ArticleVO getArticle(String taskId, Long userId) {
        Article article = getByTaskId(taskId);
        if (!article.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该文章");
        }
        return toVO(article);
    }

    @Override
    public Page<ArticleVO> listArticle(ArticleQueryRequest request, Long userId, boolean isAdmin) {
        QueryWrapper queryWrapper = new QueryWrapper();
        // 排除逻辑删除的文章（防御性过滤，确保已删除文章不出现在列表）
        queryWrapper.eq("isDelete", 0);
        // 非管理员只能看自己的文章
        if (!isAdmin) {
            queryWrapper.eq("userId", userId);
        } else if (request.getUserId() != null) {
            queryWrapper.eq("userId", request.getUserId());
        }
        // 按状态筛选
        if (StrUtil.isNotBlank(request.getStatus())) {
            queryWrapper.eq("status", request.getStatus());
        }
        // 排序（白名单防注入）
        String sortField = request.getSortField();
        if (StrUtil.isNotBlank(sortField) && isSafeField(sortField)) {
            boolean isAsc = "ascend".equals(request.getSortOrder());
            queryWrapper.orderBy(sortField, isAsc);
        } else {
            queryWrapper.orderBy("createTime", false);
        }

        Page<Article> page = articleMapper.paginate(request.getPageNum(), request.getPageSize(), queryWrapper);
        Page<ArticleVO> voPage = new Page<>();
        voPage.setTotalRow(page.getTotalRow());
        voPage.setPageNumber(page.getPageNumber());
        voPage.setPageSize(page.getPageSize());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public boolean deleteArticle(Long id, Long userId, boolean isAdmin) {
        // 绕过逻辑删除过滤查询，兼容已逻辑删除的文章（否则已删除文章再次删除会误报"文章不存在"）
        Article article = LogicDeleteManager.execWithoutLogicDelete(() -> articleMapper.selectOneById(id));
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文章不存在");
        }
        if (!isAdmin && !article.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除该文章");
        }
        // 已逻辑删除：幂等返回成功（列表已过滤 isDelete=0，前端刷新后即消失）
        if (article.getIsDelete() != null && article.getIsDelete() == 1) {
            return true;
        }
        // 逻辑删除（@Column(isLogicDelete = true) 自动处理）
        boolean result = articleMapper.deleteById(id) > 0;
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除失败");
        }
        return true;
    }

    @Override
    public AgentExecutionStats getExecutionLogs(String taskId, Long userId) {
        return articleTaskService.getExecutionStats(taskId, userId);
    }

    // ==================== 私有辅助 ====================

    private Article getByTaskId(String taskId) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("taskId", taskId);
        Article article = articleMapper.selectOneByQuery(queryWrapper);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文章不存在");
        }
        return article;
    }

    /**
     * 实体转 VO（解析 JSON 字段，格式化时间）
     */
    private ArticleVO toVO(Article article) {
        ArticleVO vo = new ArticleVO();
        // 排除 JSON 字段与时间字段（类型不同需手动处理）
        BeanUtil.copyProperties(article, vo, "createTime", "completedTime", "titleOptions", "outline", "images");
        vo.setTitleOptions(parseList(article.getTitleOptions(), TitleOption.class));
        vo.setOutline(parseList(article.getOutline(), OutlineItem.class));
        vo.setImages(parseList(article.getImages(), ImageItem.class));
        vo.setCreateTime(format(article.getCreateTime()));
        vo.setCompletedTime(format(article.getCompletedTime()));
        return vo;
    }

    private <T> List<T> parseList(String json, Class<T> clazz) {
        if (StrUtil.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(json,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String format(LocalDateTime time) {
        return time == null ? null : time.format(FORMATTER);
    }

    private boolean isSafeField(String field) {
        return "id".equals(field)
                || "taskId".equals(field)
                || "status".equals(field)
                || "createTime".equals(field)
                || "completedTime".equals(field)
                || "updateTime".equals(field);
    }
}

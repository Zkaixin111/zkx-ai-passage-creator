package com.zkx.aipassagecreator.common;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkx.aipassagecreator.exception.BusinessException;
import com.zkx.aipassagecreator.exception.ErrorCode;

import java.util.List;

/**
 * AI 返回 JSON 解析工具
 * <p>
 * 处理 AI 输出中可能出现的 markdown 代码块包裹、多余说明文字等情况，
 * 提取 JSON 数组并解析为目标类型。
 */
public class JsonExtractUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonExtractUtil() {
    }

    /**
     * 从 AI 返回文本中提取 JSON 数组并解析为 List
     */
    public static <T> List<T> parseList(String text, Class<T> clazz) {
        if (StrUtil.isBlank(text)) {
            return List.of();
        }
        String json = text.trim();
        // 剥离 markdown 代码块包裹
        if (json.startsWith("```")) {
            json = json.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```\\s*$", "").trim();
        }
        // 提取第一个 [ 到最后一个 ] 之间的内容
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        try {
            return MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 返回格式解析失败，请重试");
        }
    }
}

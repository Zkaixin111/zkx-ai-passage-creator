package com.zkx.aipassagecreator.common;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkx.aipassagecreator.exception.BusinessException;
import com.zkx.aipassagecreator.exception.ErrorCode;

import java.util.List;

/**
 * AI 返回 JSON 解析工具 —— 三层容错解析器
 * <p>
 * <h3>为什么需要这个工具？</h3>
 * <p>LLM 输出 JSON 时非常不稳定，经常出现以下情况：</p>
 * <ul>
 *   <li><b>markdown 包裹</b>：```json\n[{...}]\n```（大模型喜欢加代码块）</li>
 *   <li><b>多余说明文字</b>："好的，以下是结果：[{...}]"（大模型喜欢加前缀）</li>
 *   <li><b>格式错误</b>：缺逗号、多逗号、字段名没引号（概率较低但有）</li>
 *   <li><b>完全无法解析</b>：大模型理解错误返回纯文本（需要降级处理）</li>
 * </ul>
 *
 * <h3>三层容错策略</h3>
 * <ol>
 *   <li><b>第一层：剥离 markdown 代码块</b>—— 正则去除 ```json 和 ``` 包裹</li>
 *   <li><b>第二层：正则截取 JSON 片段</b>—— 找到第一个 [ 到最后一个 ] 之间的内容，
 *       去除前后多余的说明文字</li>
 *   <li><b>第三层：Jackson 解析</b>—— 用 ObjectMapper 解析为目标类型，
 *       解析失败抛 BusinessException 提示用户重试（而不是让程序 NPE 崩掉）</li>
 * </ol>
 *
 * <h3>设计决策</h3>
 * <ul>
 *   <li>为什么只处理 JSON 数组不处理 JSON 对象？—— 智能体的输出都是列表
 *       （标题列表、大纲列表、配图需求列表），所以 parseList 专攻数组</li>
 *   <li>为什么解析失败抛业务异常而不是返回空列表？—— 返回空列表会导致
 *       后续流程拿到"空结果"继续跑（比如配图需求为空就不配图），
 *       用户不知道是 AI 出错了还是真的没有内容。抛异常让上层决定是重试还是报错</li>
 *   <li>为什么不用正则直接匹配完整 JSON？—— 正则匹配嵌套 JSON 不可靠，
 *       用"第一个 [ 到最后一个 ]"更简单也更健壮</li>
 * </ul>
 */
public class JsonExtractUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonExtractUtil() {
    }

    /**
     * 从 AI 返回文本中提取 JSON 数组并解析为 List
     * <p>
     * 三层容错流程：
     * 1. 剥离 markdown 代码块包裹（```json ... ```）
     * 2. 正则截取第一个 [ 到最后一个 ] 之间的内容（去除多余说明文字）
     * 3. Jackson 反序列化为 List&lt;T&gt;，失败抛 BusinessException
     *
     * @param text  AI 返回的原始文本
     * @param clazz 目标元素类型（如 TitleOption.class）
     * @return 解析后的列表，输入为空返回空列表
     * @throws BusinessException AI 返回格式无法解析时抛出
     */
    public static <T> List<T> parseList(String text, Class<T> clazz) {
        if (StrUtil.isBlank(text)) {
            return List.of();
        }
        String json = text.trim();
        // 第一层容错：剥离 markdown 代码块包裹（LLM 经常自作主张加 ```json ... ```）
        if (json.startsWith("```")) {
            json = json.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```\\s*$", "").trim();
        }
        // 第二层容错：提取第一个 [ 到最后一个 ] 之间的内容（去除"好的，以下是结果："等前缀文字）
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        try {
            // 第三层容错：Jackson 反序列化，失败抛业务异常（而非 NPE 让程序崩掉）
            return MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            // 解析失败不返回空列表，而是抛异常让上层决定重试还是报错
            // 返回空列表会导致后续流程拿到"空结果"继续跑（如配图需求为空就不配图），用户会困惑
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 返回格式解析失败，请重试");
        }
    }
}

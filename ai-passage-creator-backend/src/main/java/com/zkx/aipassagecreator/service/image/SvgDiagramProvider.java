package com.zkx.aipassagecreator.service.image;

import cn.hutool.core.util.StrUtil;
import com.zkx.aipassagecreator.model.enums.ImageMethodEnum;
import com.zkx.aipassagecreator.model.vo.ImageItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;

/**
 * SVG 示意图配图策略
 * <p>
 * 用 AI 根据配图描述生成内联 SVG 代码（尺寸固定 800x500），返回生成的字节（由调用方上传 COS 后展示）。
 */
@Slf4j
@Component
public class SvgDiagramProvider implements ImageProvider {

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Override
    public ImageMethodEnum method() {
        return ImageMethodEnum.SVG_DIAGRAM;
    }

    @Override
    public ImageFetchResult generate(ImageItem requirement, String taskId, int index) {
        String description = StrUtil.blankToDefault(requirement.getDescription(), requirement.getKeywords());
        try {
            String svg = generateSvg(description);
            if (StrUtil.isBlank(svg)) {
                return null;
            }
            return new ImageFetchResult(null, svg.getBytes(StandardCharsets.UTF_8),
                    ImageMethodEnum.SVG_DIAGRAM.getValue(), "svg");
        } catch (Exception e) {
            log.warn("SVG 生成失败: description={}, error={}", description, e.getMessage());
            return null;
        }
    }

    /**
     * 调用 AI 生成内联 SVG 代码
     */
    private String generateSvg(String description) {
        String prompt = """
                你是一位信息图表设计师。请根据下面的描述，生成一张简洁的内联 SVG 示意图。

                描述：%s

                要求：
                1. 只返回 SVG 代码本身，不要用 markdown 代码块包裹，不要任何解释
                2. 根元素为 <svg xmlns="http://www.w3.org/2000/svg" width="800" height="500">，白色/浅色背景
                3. 使用简单几何形状（矩形/圆形/线条/文字）表达内容，配色协调，避免依赖外部字体
                4. 可包含标题文字（用 <text> 元素），中英文均可
                """.formatted(StrUtil.maxLength(description, 200));
        String content = chatClientBuilder.build().prompt().user(prompt).call().content();
        return content == null ? null : content.trim();
    }
}

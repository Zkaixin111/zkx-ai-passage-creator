package com.zkx.aipassagecreator.service.image;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.zkx.aipassagecreator.constant.ArticleConstant;
import com.zkx.aipassagecreator.model.enums.ImageMethodEnum;
import com.zkx.aipassagecreator.model.vo.ImageItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Mermaid 图表配图策略
 * <p>
 * 用 AI 根据配图描述生成 Mermaid 代码（flowchart），再经 mermaid.ink 在线渲染为 PNG，
 * 返回生成的图片字节（由调用方上传 COS 后展示）。
 */
@Slf4j
@Component
public class MermaidImageProvider implements ImageProvider {

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Override
    public ImageMethodEnum method() {
        return ImageMethodEnum.MERMAID;
    }

    @Override
    public ImageFetchResult generate(ImageItem requirement, String taskId, int index) {
        String description = StrUtil.blankToDefault(requirement.getDescription(), requirement.getKeywords());
        try {
            String mermaidCode = generateMermaidCode(description);
            if (StrUtil.isBlank(mermaidCode)) {
                return null;
            }
            String encoded = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mermaidCode.getBytes(StandardCharsets.UTF_8));
            String renderUrl = ArticleConstant.MERMAID_RENDER_URL.replace("{code}", encoded);
            byte[] bytes = HttpUtil.downloadBytes(renderUrl);
            if (bytes == null || bytes.length == 0) {
                log.warn("Mermaid 渲染结果为空: description={}", description);
                return null;
            }
            return new ImageFetchResult(null, bytes, ImageMethodEnum.MERMAID.getValue(), "png");
        } catch (Exception e) {
            log.warn("Mermaid 生成失败: description={}, error={}", description, e.getMessage());
            return null;
        }
    }

    /**
     * 调用 AI 生成 Mermaid flowchart 代码（限定简单语法，提高渲染成功率）
     */
    private String generateMermaidCode(String description) {
        String prompt = """
                你是一位图表专家。请根据下面的配图描述，生成一段 Mermaid flowchart 代码（用于流程图示意图）。

                配图描述：%s

                要求：
                1. 只返回 Mermaid 代码本身，不要用 ```mermaid 代码块包裹，不要任何解释
                2. 使用简单的 flowchart LR/TD 语法，节点文字用中文，避免复杂语法（如 subgraph 嵌套、样式定制）
                3. 节点数量 4-6 个，内容与描述相关
                示例：flowchart LR
                    A[开始] --> B[步骤1] --> C[步骤2] --> D[完成]
                """.formatted(StrUtil.maxLength(description, 200));
        String content = chatClientBuilder.build().prompt().user(prompt).call().content();
        return content == null ? null : content.trim();
    }
}

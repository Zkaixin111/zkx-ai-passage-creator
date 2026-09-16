package com.zkx.aipassagecreator.service.image.provider;

import cn.hutool.core.util.StrUtil;
import com.zkx.aipassagecreator.model.enums.ImageMethodEnum;
import com.zkx.aipassagecreator.model.vo.ImageItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Mermaid 图表配图策略（本地渲染）
 * <p>
 * 用 AI 根据配图描述生成 Mermaid 代码（flowchart），再调用本地安装的
 * mermaid-cli（mmdc）渲染为 PNG（不依赖任何在线渲染服务）。
 * 渲染失败返回 null，由调用方降级。
 */
@Slf4j
@Component
public class MermaidImageProvider implements ImageProvider {

    /** mmdc 可执行文件路径（Windows 下为 .cmd，配置项 article.image.mermaid.mmdc-path） */
    @Value("${article.image.mermaid.mmdc-path:mmdc}")
    private String mmdcPath;

    @Resource
    private ChatClient.Builder chatClientBuilder;

    /** 渲染超时（秒） */
    private static final long RENDER_TIMEOUT_SECONDS = 60;

    @Override
    public ImageMethodEnum method() {
        return ImageMethodEnum.MERMAID;
    }

    @Override
    public ImageFetchResult generate(ImageItem requirement, String taskId, int index) {
        String description = StrUtil.blankToDefault(requirement.getDescription(), requirement.getKeywords());
        File input = null;
        File output = null;
        try {
            String mermaidCode = generateMermaidCode(description);
            if (StrUtil.isBlank(mermaidCode)) {
                return null;
            }
            // 写临时 .mmd 输入文件与输出路径
            input = File.createTempFile("mermaid-", ".mmd");
            output = File.createTempFile("mermaid-", ".png");
            Files.write(input.toPath(), mermaidCode.getBytes(StandardCharsets.UTF_8));
            Files.deleteIfExists(output.toPath());

            byte[] bytes = renderLocally(input, output);
            if (bytes == null || bytes.length == 0) {
                log.warn("Mermaid 本地渲染结果为空: description={}", description);
                return null;
            }
            return new ImageFetchResult(null, bytes, ImageMethodEnum.MERMAID.getValue(), "png");
        } catch (Exception e) {
            log.warn("Mermaid 本地渲染失败: description={}, error={}", description, e.getMessage());
            return null;
        } finally {
            deleteQuietly(input);
            deleteQuietly(output);
        }
    }

    /**
     * 调用 mmdc 本地渲染：mmdc -i input.mmd -o output.png -w 800 -H 500
     */
    private byte[] renderLocally(File input, File output) throws Exception {
        List<String> cmd = new ArrayList<>();
        // Windows 下 .cmd 需经 cmd.exe /c 执行
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            cmd.add("cmd.exe");
            cmd.add("/c");
        }
        cmd.add(mmdcPath);
        cmd.add("-i");
        cmd.add(input.getAbsolutePath());
        cmd.add("-o");
        cmd.add(output.getAbsolutePath());
        cmd.add("-w");
        cmd.add("800");
        cmd.add("-H");
        cmd.add("500");
        cmd.add("--scale");
        cmd.add("1");

        Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        boolean finished = process.waitFor(RENDER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            log.warn("Mermaid 渲染超时: {}", mmdcPath);
            return null;
        }
        if (process.exitValue() != 0) {
            String err = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            log.warn("mmdc 渲染退出码 {}: {}", process.exitValue(), StrUtil.maxLength(err, 500));
            return null;
        }
        return Files.readAllBytes(output.toPath());
    }

    private void deleteQuietly(File file) {
        if (file != null) {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (Exception ignored) {
                // 忽略清理失败
            }
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

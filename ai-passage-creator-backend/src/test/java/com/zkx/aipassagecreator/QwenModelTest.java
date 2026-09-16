package com.zkx.aipassagecreator;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 验证 application.yaml 中配置的模型（当前为 qwen3.7-plus，经 DashScope OpenAI 兼容模式接入）是否可用。
 * 运行：在 IDEA 中右键 Run 'QwenModelTest'，或 ./mvnw -Dtest=QwenModelTest test
 */
@SpringBootTest
public class QwenModelTest {

    @Resource
    private ChatModel chatModel;

    @Test
    public void testQwenModelAvailable() {
        String expectedModel = "qwen3.7-plus";
        long start = System.currentTimeMillis();
        try {
            String res = chatModel.call("你好，请只回复两个字：可用");
            long cost = System.currentTimeMillis() - start;
            System.out.println("===== 模型可用性测试 =====");
            System.out.println("期望模型: " + expectedModel);
            System.out.println("耗时    : " + cost + " ms");
            System.out.println("模型回复: " + res);
            System.out.println("结论    : ✅ 模型可用");
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            System.out.println("===== 模型可用性测试 =====");
            System.out.println("期望模型: " + expectedModel);
            System.out.println("耗时    : " + cost + " ms");
            System.out.println("结论    : ❌ 调用失败");
            System.out.println("异常信息: " + e.getMessage());
            throw e;
        }
    }
}

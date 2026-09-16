package com.zkx.aipassagecreator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 应用上下文加载测试
 * <p>
 * 仅验证 Spring 容器能正常启动。AI 模型调用测试请单独编写，避免依赖外网和 API 额度。
 */
@SpringBootTest
class AiPassageCreatorApplicationTests {

    @Test
    void contextLoads() {
        // Spring 容器启动成功即通过
    }

}

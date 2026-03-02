package com.zkx.aipassagecreator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 项目启动类
 * <p>
 * 启用 AOP 代理暴露，供 AuthInterceptor 等切面使用。
 */
@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
public class AiPassageCreatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiPassageCreatorApplication.class, args);
    }

}

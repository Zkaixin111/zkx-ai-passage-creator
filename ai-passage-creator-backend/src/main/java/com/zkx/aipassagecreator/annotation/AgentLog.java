package com.zkx.aipassagecreator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 智能体执行日志注解
 * <p>
 * 标注在智能体执行方法上，由 {@code AgentLogAspect} 切面统一记录执行日志
 * （任务 ID、智能体名称、耗时、状态、输入输出等），先写入 Redis 队列再异步落库。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentLog {

    /**
     * 智能体名称；为空时取被标注方法所在类的简单类名
     */
    String name() default "";
}

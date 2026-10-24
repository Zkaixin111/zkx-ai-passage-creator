package com.zkx.aipassagecreator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 智能体执行日志注解 —— AI 全链路可观测的入口标记
 * <p>
 * 标注在智能体执行方法上，由 {@code AgentLogAspect} 切面统一记录执行日志
 * （任务 ID、智能体名称、耗时、状态、Prompt、输入输出摘要、异常信息），
 * 先写入 Redis 队列削峰，再定时批量落库。
 * <p>
 * <b>使用方式：</b>在智能体方法上加 <code>@AgentLog</code> 或 <code>@AgentLog(name = "标题智能体")</code>，
 * 切面自动拦截并采集日志，业务代码零侵入。
 * <p>
 * <b>设计意图：</b>6 个智能体的日志采集逻辑完全一致（采集什么、怎么截断、怎么入队），
 * 如果每个智能体自己写日志代码 → 6 份重复代码、格式不统一、维护成本高。
 * 用注解+切面把横切关注点抽离，智能体只关注业务逻辑。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentLog {

    /**
     * 智能体名称；为空时取被标注方法所在类的简单类名
     */
    String name() default "";
}

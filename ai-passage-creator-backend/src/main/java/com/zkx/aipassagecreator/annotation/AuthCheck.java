package com.zkx.aipassagecreator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 * <p>
 * 标注在 Controller 方法上，在执行前校验当前用户是否拥有指定角色权限。
 * mustRole 为空字符串（默认值）时，仅校验用户已登录。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 必须拥有的角色
     */
    String mustRole() default "";
}

package com.zkx.aipassagecreator.aop;

import com.zkx.aipassagecreator.annotation.AuthCheck;
import com.zkx.aipassagecreator.constant.UserConstant;
import com.zkx.aipassagecreator.exception.BusinessException;
import com.zkx.aipassagecreator.exception.ErrorCode;
import com.zkx.aipassagecreator.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 权限校验 AOP 切面
 * <p>
 * 拦截所有标注了 {@link AuthCheck} 的方法，在执行前校验当前用户的登录状态和角色权限。
 */
@Aspect
@Component
public class AuthInterceptor {

    /**
     * 执行拦截：校验登录状态和角色权限
     *
     * @param joinPoint 连接点
     * @param authCheck 注解实例
     * @return 方法执行结果
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 1. 获取当前 HttpServletRequest
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 2. 从 Session 获取当前登录用户
        User currentUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 3. 获取注解要求的角色
        String mustRole = authCheck.mustRole();

        // 4. mustRole 为空，仅需登录即可 —— 放行
        if (mustRole.isEmpty()) {
            return joinPoint.proceed();
        }

        // 5. 管理员拥有全部权限 —— 放行
        if (UserConstant.ADMIN_ROLE.equals(currentUser.getUserRole())) {
            return joinPoint.proceed();
        }

        // 6. 校验用户角色是否匹配要求的角色
        if (!mustRole.equals(currentUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
        }

        // 7. 角色匹配 —— 放行
        return joinPoint.proceed();
    }
}

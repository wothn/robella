package org.elmo.robella.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.elmo.robella.annotation.RequiredRole;
import org.elmo.robella.model.common.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class RoleAspect {

    @Before("@within(org.elmo.robella.annotation.RequiredRole) || @annotation(org.elmo.robella.annotation.RequiredRole)")
    public Mono<Void> checkRole(JoinPoint joinPoint) {
        // 获取当前方法或类的注解
        RequiredRole requiredRole = getRequiredRoleAnnotation(joinPoint);
        if (requiredRole == null) {
            return Mono.empty();
        }

        // 获取所需的最小角色
        Role required = requiredRole.value();
        if (required == null) {
            return Mono.empty();
        }

        // 从Reactor上下文中获取当前用户角色
        return Mono.deferContextual(contextView -> {
            String roleValue = contextView.getOrDefault("role", "");
            if (roleValue.isEmpty()) {
                log.warn("No role found in context for method: {}", joinPoint.getSignature().getName());
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
            }

            Role currentRole = Role.fromValue(roleValue);

            // 检查当前角色是否具有所需最小角色的权限
            if (currentRole.hasPermission(required)) {
                log.debug("Role check passed for user with role {} accessing method {} (required: {})",
                         currentRole, joinPoint.getSignature().getName(), required);
                return Mono.empty();
            }

            log.warn("Role check failed for user with role {} accessing method {}. Required minimum role: {}",
                     currentRole, joinPoint.getSignature().getName(), required);
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions"));
        });
    }

    private RequiredRole getRequiredRoleAnnotation(JoinPoint joinPoint) {
        // 优先获取方法级别的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiredRole methodAnnotation = method.getAnnotation(RequiredRole.class);

        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        // 如果方法级别没有注解，则获取类级别的注解
        Class<?> targetClass = joinPoint.getTarget().getClass();
        return targetClass.getAnnotation(RequiredRole.class);
    }
}
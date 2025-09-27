package org.elmo.robella.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;

import java.util.UUID;

import org.elmo.robella.context.RequestContextHolder;
import org.elmo.robella.context.RequestContextHolder.RequestContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置类
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册 Sa-Token 拦截器
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器
        registry.addInterceptor(new SaInterceptor(handler -> {
            StpUtil.checkLogin();
            RequestContext requestContext = RequestContextHolder.getContext();
            requestContext.setRequestId(UUID.randomUUID().toString());
            requestContext.setUserId(StpUtil.getLoginIdAsLong());
            requestContext.setRole(StpUtil.getRoleList().getFirst());
        }).isAnnotation(false)  // 指定关闭掉注解鉴权能力，这样框架就只会做路由拦截校验了 
        ).addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/users/login",
                        "/api/users/register",
                        "/api/users/refresh",
                        "/api/health",
                        "/api/oauth/github/**",
                        "/actuator/**",
                        "/webjars/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/v1/**",
                        "/anthropic/**");
    }
}
package org.elmo.robella.config;

import org.elmo.robella.interceptor.AuthenticationInterceptor;
import org.elmo.robella.interceptor.ApiKeyInterceptor;
import org.elmo.robella.interceptor.ContextCleanupInterceptor;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;
    private final ApiKeyInterceptor apiKeyInterceptor;
    private final ContextCleanupInterceptor contextCleanupInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
                .order(1)
                .addPathPatterns("/**");

        registry.addInterceptor(apiKeyInterceptor)
                .order(2)
                .addPathPatterns("/**");

        registry.addInterceptor(contextCleanupInterceptor)
                .order(3)
                .addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}
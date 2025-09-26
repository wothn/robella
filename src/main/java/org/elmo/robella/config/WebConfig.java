package org.elmo.robella.config;

import org.elmo.robella.interceptor.ApiKeyInterceptor;
import org.elmo.robella.interceptor.ContextCleanupInterceptor;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ApiKeyInterceptor apiKeyInterceptor;
    private final ContextCleanupInterceptor contextCleanupInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyInterceptor)
                .order(1)
                .addPathPatterns("/**");

        registry.addInterceptor(contextCleanupInterceptor)
                .order(2)
                .addPathPatterns("/**");
    }

}
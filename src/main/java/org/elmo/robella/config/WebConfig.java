package org.elmo.robella.config;

import org.elmo.robella.interceptor.AuthenticationInterceptor;
import org.elmo.robella.interceptor.ApiKeyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;

@Configuration
@RequiredArgsConstructor
public class WebConfig {

    private final AuthenticationInterceptor anthenticationInterceptor;
    private final ApiKeyInterceptor apiKeyInterceptor;

    @Bean
    public WebFilter jwtFilter() {
        return anthenticationInterceptor;
    }

    @Bean
    public WebFilter apiKeyFilter() {
        return apiKeyInterceptor;
    }

  
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
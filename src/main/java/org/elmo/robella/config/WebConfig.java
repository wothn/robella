package org.elmo.robella.config;

import org.elmo.robella.interceptor.AuthenticationFilter;
import org.elmo.robella.service.ApiKeyService;
import org.elmo.robella.util.JwtUtil;
import org.elmo.robella.interceptor.ApiKeyFilter;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;

@Configuration
@RequiredArgsConstructor
public class WebConfig {

    private final JwtUtil jwtUtil;
    private final ApiKeyService apiKeyService;

    @Bean
    @Order(1)
    public WebFilter authenticationFilter() {
        return new AuthenticationFilter(jwtUtil);
    }


    @Bean
    @Order(2)
    public WebFilter apiKeyFilter() {
        return new ApiKeyFilter(apiKeyService);
    }

  
    @Bean
    @Order(0)
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
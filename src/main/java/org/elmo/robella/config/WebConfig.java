package org.elmo.robella.config;

import org.elmo.robella.interceptor.AuthenticationFilter;
import org.elmo.robella.interceptor.ApiKeyFilter;
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

    private final AuthenticationFilter authenticationFilter;
    private final ApiKeyFilter apiKeyFilter;

    @Bean
    public WebFilter authenticationFilter() {
        return authenticationFilter;
    }


    @Bean
    public WebFilter apiKeyFilter() {
        return apiKeyFilter;
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
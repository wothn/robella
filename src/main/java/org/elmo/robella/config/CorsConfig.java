package org.elmo.robella.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // 允许的域名
        corsConfig.addAllowedOriginPattern("*");
        
        // 允许的HTTP方法
        corsConfig.addAllowedMethod("*");
        
        // 允许的请求头
        corsConfig.addAllowedHeader("*");
        
        // 是否允许发送Cookie
        corsConfig.setAllowCredentials(true);
        
        // 预检请求的有效期，单位为秒
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}

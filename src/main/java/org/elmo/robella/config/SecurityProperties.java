package org.elmo.robella.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "spring.security")
public class SecurityProperties {
    
    private boolean enabled = true;
    
    private JwtProperties jwt = new JwtProperties();
    
    private CorsProperties cors = new CorsProperties();
    
    private List<String> publicEndpoints = List.of(
            "/api/auth/**",
            "/actuator/**",
            "/error",
            "/favicon.ico"
    );
    
    private List<String> adminEndpoints = List.of(
            "/api/admin/**",
            "/api/users/**"
    );
    
    @Data
    public static class JwtProperties {
        private String secret;
        private int expiration = 86400;
        private String issuer = "robella";
        private String audience = "robella-client";
    }
    
    @Data
    public static class CorsProperties {
        private List<String> allowedOrigins = List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:8080"
        );
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        private boolean allowCredentials = true;
    }
}
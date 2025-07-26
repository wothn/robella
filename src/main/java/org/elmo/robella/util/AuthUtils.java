package org.elmo.robella.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

public class AuthUtils {
    
    public static String extractApiKey(ServerHttpRequest request) {
        // 从Authorization头提取API密钥
        HttpHeaders headers = request.getHeaders();
        String authHeader = headers.getFirst("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // 从自定义头提取API密钥
        String apiKey = headers.getFirst("X-API-Key");
        if (apiKey != null) {
            return apiKey;
        }
        
        return null;
    }
}
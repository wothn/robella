package org.elmo.robella.util;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtUtil {
    
    @Value("${jwt.expiration:86400}")
    private int jwtExpiration;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public String generateToken(String username, String role) {
        // 使用Sa-Token创建token
        StpUtil.login(1001L);
        String token = StpUtil.getTokenValue();
        // 存储角色信息
        SaManager.getSaTokenDao().set("role:" + token, role, jwtExpiration);
        return token;
    }
    
    public String generateToken(String username, String role, Long userId) {
        // 使用Sa-Token创建token
        StpUtil.login(userId);
        String token = StpUtil.getTokenValue();
        // 存储额外信息
        Map<String, Object> data = new HashMap<>();
        data.put("role", role);
        data.put("username", username);
        data.put("userId", userId);
        try {
            SaManager.getSaTokenDao().set("data:" + token, objectMapper.writeValueAsString(data), jwtExpiration);
        } catch (Exception e) {
            log.error("Failed to serialize user data", e);
        }
        return token;
    }
    
    public String extractUsername(String token) {
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId != null ? loginId.toString() : null;
        } catch (Exception e) {
            log.warn("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }
    
    public String extractRole(String token) {
        try {
            Object role = SaManager.getSaTokenDao().get("role:" + token);
            if (role == null) {
                Object dataStr = SaManager.getSaTokenDao().get("data:" + token);
                if (dataStr != null) {
                    try {
                        Map<String, Object> data = objectMapper.readValue(dataStr.toString(), Map.class);
                        role = data != null ? data.get("role") : null;
                    } catch (Exception e) {
                        log.warn("Failed to deserialize user data", e);
                    }
                }
            }
            return role != null ? role.toString() : null;
        } catch (Exception e) {
            log.warn("Failed to extract role from token: {}", e.getMessage());
            return null;
        }
    }
    
    public Long extractUserId(String token) {
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId != null ? Long.parseLong(loginId.toString()) : null;
        } catch (Exception e) {
            log.warn("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }
    
    public Boolean validateToken(String token) {
        try {
            return StpUtil.getLoginIdByToken(token) != null;
        } catch (Exception e) {
            log.warn("Invalid token: {}", e.getMessage());
            return false;
        }
    }
    
    public Boolean validateToken(String token, String username) {
        try {
            String tokenUsername = extractUsername(token);
            return username.equals(tokenUsername) && validateToken(token);
        } catch (Exception e) {
            log.warn("Token validation failed for user {}: {}", username, e.getMessage());
            return false;
        }
    }
    
    public LocalDateTime getExpirationTime(String token) {
        try {
            long timeout = StpUtil.getTokenTimeout(token);
            return LocalDateTime.now().plus(timeout, ChronoUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to get expiration time: {}", e.getMessage());
            return LocalDateTime.now().plusHours(24);
        }
    }
}
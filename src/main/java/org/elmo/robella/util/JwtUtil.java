package org.elmo.robella.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400}")
    private int jwtExpiration;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    /**
     * 生成JWT Token
     * @param username 用户名
     * @param role 角色
     * @param userId 用户ID
     * @return JWT Token
     */
    public String generateToken(String username, String role, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("角色不能为空");
        }
        
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("username", username);
            claims.put("role", role);
            claims.put("userId", userId);
            
            Date now = new Date();
            Date expiration = new Date(now.getTime() + jwtExpiration * 1000L);
            
            String token = Jwts.builder()
                    .claims(claims)
                    .subject(username)
                    .issuedAt(now)
                    .expiration(expiration)
                    .signWith(getSigningKey())
                    .compact();
                    
            log.info("JWT Token生成成功 - 用户: {}, ID: {}, 角色: {}", username, userId, role);
            return token;
            
        } catch (Exception e) {
            log.error("JWT Token生成失败 - 用户: {}, ID: {}, 错误: {}", username, userId, e.getMessage());
            throw new RuntimeException("Token生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从Token中提取用户名
     * @param token JWT Token
     * @return 用户名
     */
    public String extractUsername(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Token为空，无法提取用户名");
            return null;
        }
        
        try {
            Claims claims = extractAllClaims(token);
            String username = claims.get("username", String.class);
            log.debug("成功提取用户名: {}", username);
            return username;
        } catch (Exception e) {
            log.warn("提取用户名失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从Token中提取角色
     * @param token JWT Token
     * @return 角色
     */
    public String extractRole(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Token为空，无法提取角色");
            return null;
        }
        
        try {
            Claims claims = extractAllClaims(token);
            String role = claims.get("role", String.class);
            log.debug("成功提取角色: {}", role);
            return role;
        } catch (Exception e) {
            log.warn("提取角色失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从Token中提取用户ID
     * @param token JWT Token
     * @return 用户ID
     */
    public Long extractUserId(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Token为空，无法提取用户ID");
            return null;
        }
        
        try {
            Claims claims = extractAllClaims(token);
            Object userIdObj = claims.get("userId");
            if (userIdObj != null) {
                return Long.parseLong(userIdObj.toString());
            }
            log.warn("Token中未找到用户ID信息");
            return null;
        } catch (Exception e) {
            log.warn("提取用户ID失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 验证Token是否有效
     * @param token JWT Token
     * @return 是否有效
     */
    public Boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.debug("Token为空，验证失败");
            return false;
        }
        
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();
            boolean isValid = expiration.after(new Date());
            
            if (isValid) {
                log.debug("Token验证成功");
            } else {
                log.debug("Token已过期，验证失败");
            }
            
            return isValid;
        } catch (ExpiredJwtException e) {
            log.debug("Token已过期: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 验证Token是否属于指定用户
     * @param token JWT Token
     * @param username 用户名
     * @return 是否属于指定用户
     */
    public Boolean validateToken(String token, String username) {
        if (username == null || username.trim().isEmpty()) {
            log.warn("用户名为空，无法验证Token");
            return false;
        }
        
        try {
            if (!validateToken(token)) {
                return false;
            }
            
            String tokenUsername = extractUsername(token);
            boolean matches = username.equals(tokenUsername);
            
            if (matches) {
                log.debug("Token用户验证成功: {}", username);
            } else {
                log.warn("Token用户验证失败: 期望用户{}, Token中的用户{}", username, tokenUsername);
            }
            
            return matches;
        } catch (Exception e) {
            log.warn("Token用户验证异常 - 用户: {}, 错误: {}", username, e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取Token过期时间
     * @param token JWT Token
     * @return 过期时间
     */
    public LocalDateTime getExpirationTime(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Token为空，返回默认过期时间");
            return LocalDateTime.now().plusSeconds(jwtExpiration);
        }
        
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();
            return expiration.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (Exception e) {
            log.warn("获取Token过期时间失败: {}", e.getMessage());
            return LocalDateTime.now().plusSeconds(jwtExpiration);
        }
    }
    
    /**
     * 刷新Token（生成新的Token）
     * @param token 原Token
     * @return 新Token
     */
    public String refreshToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token不能为空");
        }
        
        try {
            if (!validateToken(token)) {
                throw new RuntimeException("Token无效或已过期，无法刷新");
            }
            
            Claims claims = extractAllClaims(token);
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);
            Long userId = Long.parseLong(claims.get("userId").toString());
            
            String newToken = generateToken(username, role, userId);
            log.info("Token刷新成功 - 用户: {}", username);
            
            return newToken;
        } catch (Exception e) {
            log.error("Token刷新失败: {}", e.getMessage());
            throw new RuntimeException("Token刷新失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 提取Token中的所有Claims
     * @param token JWT Token
     * @return Claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
package org.elmo.robella.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    /**
     * 用户信息
     */
    private UserResponse user;
    
    /**
     * 访问令牌（如果使用JWT）
     */
    private String accessToken;
    
    /**
     * 刷新令牌（如果使用JWT）
     */
    private String refreshToken;
    
    /**
     * 令牌过期时间
     */
    private LocalDateTime expiresAt;
    
    /**
     * 登录时间
     */
    private LocalDateTime loginTime;
    
    /**
     * 登录状态消息
     */
    private String message;
    
    /**
     * 会话ID（如果使用session）
     */
    private String sessionId;
}

package org.elmo.robella.exception;

/**
 * 刷新Token异常
 * 当刷新Token无效或过期时抛出
 */
public class RefreshTokenException extends AuthenticationException {
    
    public RefreshTokenException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public RefreshTokenException(ErrorCode errorCode, String details) {
        super(errorCode);
        addDetail("details", details);
    }
}
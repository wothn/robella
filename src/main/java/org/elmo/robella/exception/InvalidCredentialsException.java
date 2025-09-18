package org.elmo.robella.exception;

/**
 * 无效凭证异常
 * 当用户名/密码错误时抛出
 */
public class InvalidCredentialsException extends AuthenticationException {
    
    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS);
    }
    
    public InvalidCredentialsException(String username) {
        super(ErrorCode.INVALID_CREDENTIALS);
        addDetail("username", username);
    }
}
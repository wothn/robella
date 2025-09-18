package org.elmo.robella.exception;

/**
 * 用户账户被禁用异常
 */
public class UserDisabledException extends BusinessLogicException {
    
    public UserDisabledException() {
        super(ErrorCode.USER_DISABLED);
    }
    
    public UserDisabledException(String username) {
        super(ErrorCode.USER_DISABLED);
        addDetail("username", username);
    }
    
    @Override
    public org.springframework.http.HttpStatus getHttpStatus() {
        return org.springframework.http.HttpStatus.FORBIDDEN;
    }
}
package org.elmo.robella.exception;

import org.elmo.robella.common.ErrorCodeConstants;
import org.springframework.http.HttpStatus;

/**
 * 余额不足异常
 * 当用户账户余额不足以支付请求的费用时抛出
 */
public class InsufficientCreditsException extends BusinessException {
    
    public InsufficientCreditsException(String message) {
        super(HttpStatus.PAYMENT_REQUIRED, ErrorCodeConstants.INSUFFICIENT_BALANCE, message);
    }
    
    public InsufficientCreditsException(String message, Throwable cause) {
        super(HttpStatus.PAYMENT_REQUIRED, ErrorCodeConstants.INSUFFICIENT_BALANCE, message, cause);
    }
}
package org.elmo.robella.model.dto;

import lombok.Data;

/**
 * 错误响应DTO
 * 用于统一返回错误信息格式
 */
@Data
public class ErrorResponse {
    
    private String errorCode;
    private String message;
    private long timestamp;
    
    public ErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
}
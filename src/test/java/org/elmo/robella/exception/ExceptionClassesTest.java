package org.elmo.robella.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 异常类单元测试
 */
class ExceptionClassesTest {

    @Test
    void testResourceNotFoundException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("User", 123L);
        
        assertEquals("User不存在", exception.getMessage());
        assertEquals("B4001", exception.getErrorCode().getCode());
        assertEquals(ErrorCategory.BUSINESS_LOGIC, exception.getCategory());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.getHttpStatus());
        assertEquals("User", exception.getDetails().get("resourceType"));
        assertEquals(123L, exception.getDetails().get("resourceId"));
    }

    @Test
    void testResourceConflictException() {
        ResourceConflictException exception = new ResourceConflictException("User", "username", "testuser");
        
        assertEquals("User已存在", exception.getMessage());
        assertEquals("B4002", exception.getErrorCode().getCode());
        assertEquals(ErrorCategory.BUSINESS_LOGIC, exception.getCategory());
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, exception.getHttpStatus());
        assertEquals("User", exception.getDetails().get("resourceType"));
        assertEquals("username", exception.getDetails().get("conflictField"));
        assertEquals("testuser", exception.getDetails().get("conflictValue"));
    }

    @Test
    void testInvalidCredentialsException() {
        InvalidCredentialsException exception = new InvalidCredentialsException("testuser");
        
        assertEquals("用户名或密码错误", exception.getMessage());
        assertEquals("A2001", exception.getErrorCode().getCode());
        assertEquals(ErrorCategory.AUTHENTICATION, exception.getCategory());
        assertEquals(org.springframework.http.HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
        assertEquals("testuser", exception.getDetails().get("username"));
    }

    @Test
    void testUserDisabledException() {
        UserDisabledException exception = new UserDisabledException("testuser");
        
        assertEquals("用户账户已禁用", exception.getMessage());
        assertEquals("B4004", exception.getErrorCode().getCode());
        assertEquals(ErrorCategory.BUSINESS_LOGIC, exception.getCategory());
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, exception.getHttpStatus());
        assertEquals("testuser", exception.getDetails().get("username"));
    }

    @Test
    void testProviderException() {
        ProviderException exception = new ProviderException("openai", 429, "Rate limit exceeded");
        
        assertEquals("服务提供商错误: Rate limit exceeded", exception.getMessage());
        assertEquals("E5001", exception.getErrorCode().getCode());
        assertEquals(ErrorCategory.EXTERNAL_SERVICE, exception.getCategory());
        assertEquals(org.springframework.http.HttpStatus.BAD_GATEWAY, exception.getHttpStatus());
        assertEquals("openai", exception.getDetails().get("provider"));
        assertEquals(429, exception.getDetails().get("httpStatus"));
    }

    @Test
    void testTransformException() {
        TransformException exception = new TransformException("openai", "anthropic", "Invalid format");
        
        assertEquals("数据转换错误: Invalid format", exception.getMessage());
        assertEquals("E5002", exception.getErrorCode().getCode());
        assertEquals(ErrorCategory.EXTERNAL_SERVICE, exception.getCategory());
        assertEquals(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
        assertEquals("openai", exception.getDetails().get("fromFormat"));
        assertEquals("anthropic", exception.getDetails().get("toFormat"));
    }

    @Test
    void testQuotaExceededException() {
        QuotaExceededException exception = new QuotaExceededException("API", 1000, 500);
        
        assertEquals("配额已超限", exception.getMessage());
        assertEquals("B4007", exception.getErrorCode().getCode());
        assertEquals(ErrorCategory.BUSINESS_LOGIC, exception.getCategory());
        assertEquals(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, exception.getHttpStatus());
        assertEquals("API", exception.getDetails().get("quotaType"));
        assertEquals(1000L, exception.getDetails().get("current"));
        assertEquals(500L, exception.getDetails().get("limit"));
    }

    @Test
    void testRateLimitException() {
        RateLimitException exception = new RateLimitException(60L);
        
        assertEquals("请求频率超限", exception.getMessage());
        assertEquals("B4008", exception.getErrorCode().getCode());
        assertEquals(ErrorCategory.BUSINESS_LOGIC, exception.getCategory());
        assertEquals(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, exception.getHttpStatus());
        assertEquals(60L, exception.getDetails().get("retryAfterSeconds"));
    }

    @Test
    void testErrorCodeFormatting() {
        // 测试带参数的错误码格式化
        ErrorCode code = ErrorCode.RESOURCE_NOT_FOUND;
        String formatted = code.formatMessage("User");
        assertEquals("User不存在", formatted);
        
        // 测试多参数格式化
        ErrorCode roleCode = ErrorCode.ROLE_REQUIRED;
        String roleFormatted = roleCode.formatMessage("ADMIN");
        assertEquals("需要ADMIN角色权限", roleFormatted);
    }

    @Test
    void testAddDetails() {
        ResourceNotFoundException exception = new ResourceNotFoundException("User", 123L);
        
        // 测试添加额外详情
        exception.addDetail("source", "database")
                 .addDetail("query", "SELECT * FROM users WHERE id = 123");
        
        assertEquals(4, exception.getDetails().size()); // resourceType, resourceId, source, query
        assertEquals("database", exception.getDetails().get("source"));
        assertEquals("SELECT * FROM users WHERE id = 123", exception.getDetails().get("query"));
    }
}
package org.elmo.robella.exception;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 错误响应格式测试
 */
class ErrorResponseTest {

    @Test
    void testStandardErrorResponse() {
        Map<String, Object> details = Map.of("field", "username", "value", "test");
        
        ErrorResponse response = ErrorResponse.create(
            "V1001", 
            "参数无效", 
            "VALIDATION", 
            "/api/users", 
            details
        );
        
        assertNotNull(response.getError());
        assertEquals("V1001", response.getError().getCode());
        assertEquals("参数无效", response.getError().getMessage());
        assertEquals("VALIDATION", response.getError().getCategory());
        assertEquals("/api/users", response.getError().getPath());
        assertEquals(details, response.getError().getDetails());
        assertNotNull(response.getError().getTimestamp());
    }

    @Test
    void testFromException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("User", 123L);
        String path = "/api/users/123";
        
        ErrorResponse response = ErrorResponse.fromException(exception, path);
        
        assertNotNull(response.getError());
        assertEquals("B4001", response.getError().getCode());
        assertEquals("User不存在", response.getError().getMessage());
        assertEquals("BUSINESS_LOGIC", response.getError().getCategory());
        assertEquals(path, response.getError().getPath());
        assertEquals("User", response.getError().getDetails().get("resourceType"));
        assertEquals(123L, response.getError().getDetails().get("resourceId"));
    }

    @Test
    void testOpenAIErrorResponse() {
        InvalidCredentialsException exception = new InvalidCredentialsException();
        
        OpenAIErrorResponse response = OpenAIErrorResponse.fromException(exception);
        
        assertNotNull(response.getError());
        assertEquals("用户名或密码错误", response.getError().getMessage());
        assertEquals("authentication_error", response.getError().getType());
        assertEquals("invalid_authentication", response.getError().getCode());
    }

    @Test
    void testAnthropicErrorResponse() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Model", "gpt-4");
        
        AnthropicErrorResponse response = AnthropicErrorResponse.fromException(exception);
        
        assertEquals("error", response.getType());
        assertNotNull(response.getError());
        assertEquals("invalid_request_error", response.getError().getType());
        assertEquals("Model不存在", response.getError().getMessage());
    }

    @Test
    void testOpenAIErrorTypeMapping() {
        // 测试验证错误映射
        InvalidParameterException validationEx = new InvalidParameterException("username");
        OpenAIErrorResponse validationResp = OpenAIErrorResponse.fromException(validationEx);
        assertEquals("invalid_request_error", validationResp.getError().getType());
        assertEquals("invalid_parameter", validationResp.getError().getCode());
        
        // 测试认证错误映射
        InvalidCredentialsException authEx = new InvalidCredentialsException();
        OpenAIErrorResponse authResp = OpenAIErrorResponse.fromException(authEx);
        assertEquals("authentication_error", authResp.getError().getType());
        assertEquals("invalid_authentication", authResp.getError().getCode());
        
        // 测试频率限制错误映射
        RateLimitException rateLimitEx = new RateLimitException();
        OpenAIErrorResponse rateLimitResp = OpenAIErrorResponse.fromException(rateLimitEx);
        assertEquals("invalid_request_error", rateLimitResp.getError().getType());
        assertEquals("rate_limit_exceeded", rateLimitResp.getError().getCode());
        
        // 测试提供商错误映射
        ProviderException providerEx = new ProviderException("API error");
        OpenAIErrorResponse providerResp = OpenAIErrorResponse.fromException(providerEx);
        assertEquals("api_error", providerResp.getError().getType());
        assertEquals("api_error", providerResp.getError().getCode());
    }

    @Test
    void testAnthropicErrorTypeMapping() {
        // 测试验证错误映射
        InvalidParameterException validationEx = new InvalidParameterException("model");
        AnthropicErrorResponse validationResp = AnthropicErrorResponse.fromException(validationEx);
        assertEquals("invalid_request_error", validationResp.getError().getType());
        
        // 测试认证错误映射
        InvalidCredentialsException authEx = new InvalidCredentialsException();
        AnthropicErrorResponse authResp = AnthropicErrorResponse.fromException(authEx);
        assertEquals("authentication_error", authResp.getError().getType());
        
        // 测试频率限制错误映射
        RateLimitException rateLimitEx = new RateLimitException();
        AnthropicErrorResponse rateLimitResp = AnthropicErrorResponse.fromException(rateLimitEx);
        assertEquals("rate_limit_error", rateLimitResp.getError().getType());
        
        // 测试配额超限错误映射
        QuotaExceededException quotaEx = new QuotaExceededException();
        AnthropicErrorResponse quotaResp = AnthropicErrorResponse.fromException(quotaEx);
        assertEquals("overloaded_error", quotaResp.getError().getType());
        
        // 测试外部服务错误映射
        ProviderException providerEx = new ProviderException("API error");
        AnthropicErrorResponse providerResp = AnthropicErrorResponse.fromException(providerEx);
        assertEquals("api_error", providerResp.getError().getType());
    }

    @Test
    void testErrorResponseWithNullDetails() {
        ResourceNotFoundException exception = new ResourceNotFoundException("User");
        // 清空details
        exception.getDetails().clear();
        
        ErrorResponse response = ErrorResponse.fromException(exception, "/api/users");
        
        // 当details为空时，应该返回null而不是空Map
        assertNull(response.getError().getDetails());
    }
}
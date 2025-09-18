package org.elmo.robella.exception;

import org.elmo.robella.model.response.UserResponse;
import org.elmo.robella.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全局异常处理器测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testResourceNotFoundException() {
        webTestClient.get()
                .uri("/api/users/999999")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("B4001")
                .jsonPath("$.error.message").isEqualTo("User不存在")
                .jsonPath("$.error.category").isEqualTo("BUSINESS_LOGIC")
                .jsonPath("$.error.details.resourceType").isEqualTo("User")
                .jsonPath("$.error.details.resourceId").isEqualTo(999999);
    }

    @Test
    void testResourceConflictException() {
        // 首先创建一个用户
        String existingUsername = "testuser" + System.currentTimeMillis();
        
        webTestClient.post()
                .uri("/api/users")
                .bodyValue(createTestUser(existingUsername, "test@example.com"))
                .header("Authorization", "Bearer admin-token") // 需要管理员权限
                .exchange()
                .expectStatus().isOk();

        // 尝试创建同名用户，应该返回冲突错误
        webTestClient.post()
                .uri("/api/users")
                .bodyValue(createTestUser(existingUsername, "another@example.com"))
                .header("Authorization", "Bearer admin-token")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("B4002")
                .jsonPath("$.error.message").isEqualTo("User已存在")
                .jsonPath("$.error.category").isEqualTo("BUSINESS_LOGIC")
                .jsonPath("$.error.details.conflictField").isEqualTo("username")
                .jsonPath("$.error.details.conflictValue").isEqualTo(existingUsername);
    }

    @Test
    void testInvalidCredentialsException() {
        webTestClient.post()
                .uri("/api/users/login")
                .bodyValue(createLoginRequest("nonexistent", "wrongpassword"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("A2001")
                .jsonPath("$.error.message").isEqualTo("用户名或密码错误")
                .jsonPath("$.error.category").isEqualTo("AUTHENTICATION");
    }

    @Test
    void testValidationError() {
        // 发送无效的用户数据（空用户名）
        webTestClient.post()
                .uri("/api/users")
                .bodyValue("{\"username\":\"\",\"email\":\"test@example.com\",\"password\":\"password\"}")
                .header("Authorization", "Bearer admin-token")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("V1005")
                .jsonPath("$.error.message").isEqualTo("数据验证失败")
                .jsonPath("$.error.category").isEqualTo("VALIDATION");
    }

    @Test
    void testOpenAIFormatError() {
        // 测试OpenAI格式的错误响应
        webTestClient.post()
                .uri("/v1/chat/completions")
                .bodyValue("{\"model\":\"nonexistent\",\"messages\":[]}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.type").isEqualTo("invalid_request_error")
                .jsonPath("$.error.message").exists();
    }

    @Test
    void testAnthropicFormatError() {
        // 测试Anthropic格式的错误响应
        webTestClient.post()
                .uri("/anthropic/v1/messages")
                .bodyValue("{\"model\":\"nonexistent\",\"messages\":[]}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo("error")
                .jsonPath("$.error.type").isEqualTo("invalid_request_error")
                .jsonPath("$.error.message").exists();
    }

    private Map<String, Object> createTestUser(String usernameParam, String emailParam) {
        return Map.of(
            "username", usernameParam,
            "email", emailParam,
            "password", "testpassword",
            "displayName", "Test User"
        );
    }

    private Map<String, Object> createLoginRequest(String usernameParam, String passwordParam) {
        return Map.of(
            "username", usernameParam,
            "password", passwordParam
        );
    }
}
package org.elmo.robella.interceptor;

import org.elmo.robella.service.ApiKeyService;
import org.elmo.robella.model.entity.ApiKey;
import org.elmo.robella.context.RequestContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyInterceptor implements HandlerInterceptor {

    private final ApiKeyService apiKeyService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String path = request.getRequestURI();

        // 只处理需要API key的端点
        if (!isApiKeyEndpoint(path)) {
            return true;
        }

        // 提取API key - 支持Authorization Bearer和X-API-Key两种方式
        String apiKey = null;
        String authHeader = request.getHeader("Authorization");
        String xApiKey = request.getHeader("X-API-Key");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            apiKey = authHeader.substring(7);
        } else if (xApiKey != null && !xApiKey.isBlank()) {
            // Anthropic API使用X-API-Key头部，直接包含密钥值
            apiKey = xApiKey;
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Missing or invalid API key for path: {}", path);
            handleUnauthorized(response, "Missing or invalid API key");
            return false;
        }

        // 验证API key
        try {
            ApiKey validKey = apiKeyService.validateApiKey(apiKey);
            if (validKey != null) {
                log.debug("API key validation successful for user: {} on path: {}", validKey.getUserId(), path);

                // 检查是否已有RequestId，如果没有则生成新的
                String requestId = RequestContextHolder.getContext() != null ?
                    RequestContextHolder.getContext().getRequestId() : null;
                if (requestId == null) {
                    requestId = UUID.randomUUID().toString();
                }

                // 将用户信息设置到ThreadLocal上下文
                RequestContextHolder.RequestContext context = RequestContextHolder.RequestContext.builder()
                    .requestId(requestId)
                    .userId(validKey.getUserId())
                    .apiKeyId(validKey.getId())
                    .build();
                RequestContextHolder.setContext(context);

                return true;
            } else {
                log.warn("Invalid API key for path: {}", path);
                handleUnauthorized(response, "Invalid API key");
                return false;
            }
        } catch (Exception e) {
            log.error("API key validation error: {}", e.getMessage());
            handleUnauthorized(response, "API key validation error");
            return false;
        }
    }

    private void handleUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String body = String.format("{\"error\":{\"type\":\"authentication_error\",\"message\":\"%s\"}}", message);
        response.getWriter().write(body);
    }

    private boolean isApiKeyEndpoint(String path) {
        // 排除查看模型的端点，这些端点可以公开访问
        if (path.equals("/v1/models") || path.equals("/anthropic/v1/models")) {
            return false;
        }

        return path.startsWith("/v1") || path.startsWith("/anthropic/v1");
    }
}
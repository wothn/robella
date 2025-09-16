package org.elmo.robella.interceptor;

import org.elmo.robella.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.lang.NonNull;
import reactor.util.context.Context;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyInterceptor implements WebFilter {

    private final ApiKeyService apiKeyService;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 只处理需要API key的端点
        if (!isApiKeyEndpoint(path)) {
            return chain.filter(exchange);
        }

        // 提取API key
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return handleUnauthorized(exchange, "Missing or invalid API key");
        }

        String apiKey = authHeader.substring(7);
        if (apiKey.isBlank()) {
            log.warn("Empty API key for path: {}", path);
            return handleUnauthorized(exchange, "Empty API key");
        }

        // 验证API key
        return apiKeyService.validateApiKey(apiKey)
                .flatMap(validKey -> {
                    if (validKey != null) {
                        // 将用户信息添加到上下文
                        Context userContext = Context.of(
                            "userId", validKey.getUserId(),
                            "apiKeyId", validKey.getId(),
                            "apiKeyPrefix", validKey.getKeyPrefix()
                        );

                        log.debug("API key validation successful for user: {} on path: {}", validKey.getUserId(), path);

                        return chain.filter(exchange)
                            .contextWrite(userContext);
                    } else {
                        log.warn("Invalid API key for path: {}", path);
                        return handleUnauthorized(exchange, "Invalid API key");
                    }
                })
                .onErrorResume(e -> {
                    log.error("API key validation error: {}", e.getMessage());
                    return handleUnauthorized(exchange, "API key validation error");
                });
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");

        String body = String.format("{\"error\":{\"type\":\"authentication_error\",\"message\":\"%s\"}}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private boolean isApiKeyEndpoint(String path) {
        // 排除查看模型的端点，这些端点可以公开访问
        if (path.equals("/v1/models") || path.equals("/anthropic/v1/models")) {
            return false;
        }
        
        return path.startsWith("/v1") || path.startsWith("/anthropic/v1");
    }
}
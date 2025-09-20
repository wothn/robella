package org.elmo.robella.filter;

import org.elmo.robella.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.lang.NonNull;
import reactor.util.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class ApiKeyFilter implements WebFilter {

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

        // 提取API key - 支持Authorization Bearer和X-API-Key两种方式
        String apiKey = null;
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String xApiKey = request.getHeaders().getFirst("X-API-Key");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            apiKey = authHeader.substring(7);
        } else if (xApiKey != null && !xApiKey.isBlank()) {
            // Anthropic API使用X-API-Key头部，直接包含密钥值
            apiKey = xApiKey;
        }
        
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Missing or invalid API key for path: {}", path);
            return handleUnauthorized(exchange, "Missing or invalid API key");
        }

        // 验证API key
        return apiKeyService.validateApiKey(apiKey)
                .flatMap(validKey -> {
                    if (validKey != null) {
                        log.debug("API key validation successful for user: {} on path: {}", validKey.getUserId(), path);

                        // 合并Context而不是覆盖，保留现有的requestId等信息
                        return chain.filter(exchange)
                            .contextWrite(ctx -> {
                                // 保留现有Context中的requestId
                                String existingRequestId = ctx.getOrDefault("requestId", UUID.randomUUID().toString());
                                // 添加用户信息到Context
                                return Context.of(
                                    "userId", validKey.getUserId(),
                                    "apiKeyId", validKey.getId(),
                                    "requestId", existingRequestId
                                );
                            });
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
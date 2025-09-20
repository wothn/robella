package org.elmo.robella.filter;

import org.elmo.robella.util.JwtUtil;
import org.elmo.robella.model.common.Role;
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

@Slf4j
@RequiredArgsConstructor
public class AuthenticationFilter implements WebFilter {

    private final JwtUtil jwtUtil;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 检查是否已有RequestId，如果没有则生成新的
        String requestId = exchange.getAttribute("requestId");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
            exchange.getAttributes().put("requestId", requestId);
        }

        // 公开端点跳过JWT验证
        if (isPublicEndpoint(path)) {
            log.debug("Public endpoint accessed: {}, requestId: {}", path, requestId);
            Context publicContext = Context.of("requestId", requestId);
            return chain.filter(exchange)
                .contextWrite(publicContext);
        }

        // 提取JWT令牌
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}, requestId: {}", path, requestId);
            return handleUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        if (token.isBlank()) {
            log.warn("Empty JWT token for path: {}, requestId: {}", path, requestId);
            return handleUnauthorized(exchange, "Empty JWT token");
        }

        // 验证JWT令牌
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid JWT token for path: {}, requestId: {}", path, requestId);
            return handleUnauthorized(exchange, "Invalid JWT token");
        }

        // 提取用户信息并添加到Reactor上下文
        try {
            String username = jwtUtil.extractUsername(token);
            Role role = jwtUtil.extractRole(token);
            
            // 创建Reactor上下文包含用户信息
            Context userContext = Context.of(
                "username", username,
                "role", role.getValue(),
                "token", token,
                "userId", jwtUtil.extractClaim(token, claims -> claims.get("userId", Long.class)),
                "requestId", requestId
            );
            
            log.debug("JWT validation successful for user: {} on path: {}, requestId: {}", username, path, requestId);
            
            // 将上下文传递给下一个过滤器
            return chain.filter(exchange)
                .contextWrite(userContext);
        } catch (Exception e) {
            log.error("Error extracting claims from JWT token: {}", e.getMessage());
            return handleUnauthorized(exchange, "Error processing JWT token");
        }
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        // 未认证401
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = String.format("{\"error\":{\"type\":\"authentication_error\",\"message\":\"%s\"}}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/users/login") ||
               path.startsWith("/api/users/register") ||
               path.startsWith("/api/users/refresh") ||
               path.startsWith("/api/health") ||
               path.startsWith("/api/oauth/github") ||
               path.startsWith("/actuator") ||
               path.startsWith("/webjars") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs")||
               path.startsWith("/v1") ||
               path.startsWith("/anthropic") ||
               path.equals("/favicon.ico");
    }
}
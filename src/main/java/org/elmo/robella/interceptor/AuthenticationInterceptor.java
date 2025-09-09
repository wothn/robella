package org.elmo.robella.interceptor;

import org.elmo.robella.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationInterceptor implements WebFilter {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 公开端点跳过JWT验证
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        // 提取JWT令牌
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return Mono.error(new RuntimeException("Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7);
        if (token.isBlank()) {
            log.warn("Empty JWT token for path: {}", path);
            return Mono.error(new RuntimeException("Empty JWT token"));
        }

        // 验证JWT令牌
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            return Mono.error(new RuntimeException("Invalid JWT token"));
        }

        // 提取用户信息并添加到请求属性
        try {
            String username = jwtUtil.extractUsername(token);
            Integer role = jwtUtil.extractRole(token);
            
            exchange.getAttributes().put("username", username);
            exchange.getAttributes().put("role", role);
            exchange.getAttributes().put("token", token);
            
            log.debug("JWT validation successful for user: {} on path: {}", username, path);
        } catch (Exception e) {
            log.error("Error extracting claims from JWT token: {}", e.getMessage());
            return Mono.error(new RuntimeException("Error processing JWT token"));
        }

        return chain.filter(exchange);
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/users/login") ||
               path.startsWith("/api/users/register") ||
               path.startsWith("/api/users/refresh") ||
               path.startsWith("/api/health") ||
               path.startsWith("/actuator") ||
               path.startsWith("/webjars") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs")||
               path.startsWith("/api/v1");
    }
}
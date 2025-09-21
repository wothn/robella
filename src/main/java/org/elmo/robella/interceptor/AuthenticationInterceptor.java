package org.elmo.robella.interceptor;

import org.elmo.robella.util.JwtUtil;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.context.RequestContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String path = request.getRequestURI();

        // 检查是否已有RequestId，如果没有则生成新的
        String requestId = RequestContextHolder.getContext() != null ?
            RequestContextHolder.getContext().getRequestId() : null;
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        // 公开端点跳过JWT验证
        if (isPublicEndpoint(path)) {
            log.debug("Public endpoint accessed: {}, requestId: {}", path, requestId);
            return true;
        }

        // 提取JWT令牌
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}, requestId: {}", path, requestId);
            handleUnauthorized(response, "Missing or invalid Authorization header");
            return false;
        }

        String token = authHeader.substring(7);
        if (token.isBlank()) {
            log.warn("Empty JWT token for path: {}, requestId: {}", path, requestId);
            handleUnauthorized(response, "Empty JWT token");
            return false;
        }

        // 验证JWT令牌
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid JWT token for path: {}, requestId: {}", path, requestId);
            handleUnauthorized(response, "Invalid JWT token");
            return false;
        }

        // 提取用户信息并设置到ThreadLocal
        try {
            String username = jwtUtil.extractUsername(token);
            Role role = jwtUtil.extractRole(token);
            Long userId = jwtUtil.extractClaim(token, claims -> claims.get("userId", Long.class));

            // 将用户信息设置到ThreadLocal上下文
            RequestContextHolder.RequestContext context = RequestContextHolder.RequestContext.builder()
                .requestId(requestId)
                .username(username)
                .role(role.getValue())
                .token(token)
                .userId(userId)
                .build();
            RequestContextHolder.setContext(context);

            log.debug("JWT validation successful for user: {} on path: {}, requestId: {}", username, path, requestId);

            return true;
        } catch (Exception e) {
            log.error("Error extracting claims from JWT token: {}", e.getMessage());
            handleUnauthorized(response, "Error processing JWT token");
            return false;
        }
    }

    private void handleUnauthorized(HttpServletResponse response, String message) throws Exception {
        // 未认证401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String body = String.format("{\"error\":{\"type\":\"authentication_error\",\"message\":\"%s\"}}", message);
        response.getWriter().write(body);
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
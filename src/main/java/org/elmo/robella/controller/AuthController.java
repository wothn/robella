package org.elmo.robella.controller;

import org.elmo.robella.model.LoginResponse;
import org.elmo.robella.service.GitHubOAuthService;
import org.elmo.robella.service.UserService;
import org.elmo.robella.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final GitHubOAuthService gitHubOAuthService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/github/login")
    public Mono<ResponseEntity<String>> githubLogin(
            @RequestParam(required = false) String redirectUri,
            @RequestParam(required = false, name = "frontRedirect") String frontRedirect,
            ServerWebExchange exchange) {

        String finalRedirectUri = redirectUri != null ? redirectUri
                : exchange.getRequest().getURI().getScheme() + "://" +
                        exchange.getRequest().getURI().getHost() + ":" +
                        exchange.getRequest().getURI().getPort() + "/api/auth/github/callback";

    // 使用 state 透传前端回跳地址（如 http://localhost:5173）
    String authUrl = gitHubOAuthService.getAuthorizationUrl(finalRedirectUri, frontRedirect);

        return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authUrl))
                .build());
    }

    @GetMapping("/github/callback")
    public Mono<ResponseEntity<Object>> githubCallback(
            @RequestParam String code,
            @RequestParam(required = false) String redirectUri,
            @RequestParam(required = false) String state,
            ServerWebExchange exchange) {

        String finalRedirectUri = redirectUri != null ? redirectUri
                : exchange.getRequest().getURI().getScheme() + "://" +
                        exchange.getRequest().getURI().getHost() + ":" +
                        exchange.getRequest().getURI().getPort() + "/api/auth/github/callback";

    return gitHubOAuthService.exchangeCodeForUser(code, finalRedirectUri)
                .flatMap(user -> {
                    String jwtToken = jwtTokenProvider.generateToken(user.getUsername(), user.getRole(), user.getId());
                    return userService.createOAuthLoginResponse(user)
                            .map(loginResponse -> {
                                loginResponse.setAccessToken(jwtToken);
                                loginResponse.setExpiresAt(jwtTokenProvider.getExpirationTime(jwtToken));
                                loginResponse.setLoginTime(LocalDateTime.now());
                                return loginResponse;
                            });
                })
                .map(loginResponse -> {
                    // 确保token不为null
                    String token = loginResponse.getAccessToken();
                    if (token == null) {
                        throw new RuntimeException("JWT token generation failed");
                    }
                    // 优先从 state 还原前端站点，例如 http://localhost:5173
            String frontBase = (state != null && !state.isBlank()) ? URLDecoder.decode(state, StandardCharsets.UTF_8) : "";
                    String frontendRedirectUrl = (frontBase.isEmpty() ? "" : frontBase) + "/auth/success?" +
                            "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8) +
                            "&user=" + URLEncoder.encode(loginResponse.getUser().getUsername(), StandardCharsets.UTF_8);

                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(frontendRedirectUrl))
                            .build();
                })
                .onErrorResume(error -> {
                    log.error("GitHub登录失败: {}", error.getMessage());
                    String errorMessage = error.getMessage() != null ? error.getMessage() : "GitHub登录失败";
                    String frontBase = (state != null && !state.isBlank()) ? URLDecoder.decode(state, StandardCharsets.UTF_8) : "";
                    String frontendErrorUrl = (frontBase.isEmpty() ? "" : frontBase) + "/auth/error?" +
                            "message=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);

                    return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(frontendErrorUrl))
                            .build());
                });
    }

    @GetMapping("/github/user")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<LoginResponse>> getGitHubUser() {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(context -> {
                    String username = context.getAuthentication().getName();
                    return userService.getUserByUsername(username);
                })
                .map(user -> {
                    LoginResponse response = LoginResponse.builder()
                            .user(user)
                            .accessToken(null)
                            .message("用户信息获取成功")
                            .build();
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    log.error("获取GitHub用户信息失败: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }
}
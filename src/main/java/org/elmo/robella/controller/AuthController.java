package org.elmo.robella.controller;

import org.elmo.robella.model.LoginResponse;
import org.elmo.robella.service.GitHubOAuthService;
import org.elmo.robella.service.UserService;
import org.elmo.robella.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
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
    private final JwtUtil jwtUtil;
    
    @GetMapping("/github/login")
    public Mono<ResponseEntity<String>> githubLogin(
            @RequestParam(required = false) String redirectUri,
            ServerWebExchange exchange) {
        
        String finalRedirectUri = redirectUri != null ? redirectUri : 
            exchange.getRequest().getURI().getScheme() + "://" + 
            exchange.getRequest().getURI().getHost() + ":" + 
            exchange.getRequest().getURI().getPort() + "/api/auth/github/callback";
        
        String authUrl = gitHubOAuthService.getAuthorizationUrl(finalRedirectUri);
        
        return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authUrl))
                .build());
    }
    
    @GetMapping("/github/callback")
    public Mono<ResponseEntity<Object>> githubCallback(
            @RequestParam String code,
            @RequestParam(required = false) String redirectUri,
            ServerWebExchange exchange) {
        
        String finalRedirectUri = redirectUri != null ? redirectUri : 
            exchange.getRequest().getURI().getScheme() + "://" + 
            exchange.getRequest().getURI().getHost() + ":" + 
            exchange.getRequest().getURI().getPort() + "/api/auth/github/callback";
        
        return gitHubOAuthService.exchangeCodeForUser(code, finalRedirectUri)
                .flatMap(user -> {
                    String jwtToken = jwtUtil.generateToken(user.getUsername(), user.getRole(), user.getId());
                    return userService.createOAuthLoginResponse(user)
                            .map(loginResponse -> {
                                loginResponse.setAccessToken(jwtToken);
                                loginResponse.setExpiresAt(jwtUtil.getExpirationTime(jwtToken));
                                loginResponse.setLoginTime(LocalDateTime.now());
                                return loginResponse;
                            });
                })
                .map(loginResponse -> {
                    String frontendRedirectUrl = "/auth/success?" +
                            "token=" + URLEncoder.encode(loginResponse.getAccessToken(), StandardCharsets.UTF_8) +
                            "&user=" + URLEncoder.encode(loginResponse.getUser().getUsername(), StandardCharsets.UTF_8) +
                            "&message=" + URLEncoder.encode("GitHub登录成功", StandardCharsets.UTF_8);
                    
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(frontendRedirectUrl))
                            .build();
                })
                .onErrorResume(error -> {
                    log.error("GitHub登录失败: {}", error.getMessage());
                    String errorMessage = error.getMessage() != null ? error.getMessage() : "GitHub登录失败";
                    String frontendErrorUrl = "/auth/error?message=" + 
                            URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
                    
                    return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(frontendErrorUrl))
                            .build());
                });
    }
    
    @GetMapping("/github/user")
    public Mono<ResponseEntity<LoginResponse>> getGitHubUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        
        String token = authHeader.substring(7);
        
        if (!jwtUtil.validateToken(token)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        
        String username = jwtUtil.extractUsername(token);
        return userService.getUserByUsername(username)
                .map(user -> {
                    LoginResponse response = LoginResponse.builder()
                            .user(user)
                            .accessToken(token)
                            .expiresAt(jwtUtil.getExpirationTime(token))
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
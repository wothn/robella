package org.elmo.robella.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.service.GitHubOAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequestMapping("/api/oauth/github")
@RequiredArgsConstructor
@Slf4j
public class GitHubOAuthController {

    private final GitHubOAuthService gitHubOAuthService;

    @GetMapping("/login")
    public Mono<Void> login(ServerWebExchange exchange) {
        String authUrl = gitHubOAuthService.generateAuthorizationUrl();
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(URI.create(authUrl));
        return response.setComplete();
    }

    @GetMapping("/callback")
    public Mono<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            ServerWebExchange exchange) {
        
        log.info("GitHub OAuth callback received - code: {}, state: {}", 
                code != null ? "present" : "missing", 
                state != null ? "present" : "missing");
        
        if (code == null || state == null) {
            log.warn("GitHub OAuth callback called without required parameters");
            return redirectToError(exchange);
        }
        
        return gitHubOAuthService.handleOAuthCallback(code, state)
                .flatMap(loginResponse -> {
                    log.info("GitHub OAuth service returned login response - accessToken: {}, refreshToken: {}", 
                            loginResponse.getAccessToken() != null ? "present" : "missing",
                            loginResponse.getRefreshToken() != null ? "present" : "missing");
                    
                    // 生成JWT token并添加到重定向URL的参数中，确保URL编码
                    try {
                        String encodedToken = java.net.URLEncoder.encode(loginResponse.getAccessToken(), "UTF-8");
                        String encodedRefreshToken = java.net.URLEncoder.encode(loginResponse.getRefreshToken(), "UTF-8");
                        String redirectUrl = "http://localhost:5173/auth/success?token=" + encodedToken + "&refreshToken=" + encodedRefreshToken;
                        
                        log.info("Redirecting to frontend success page: {}", redirectUrl);
                        
                        ServerHttpResponse response = exchange.getResponse();
                        response.setStatusCode(HttpStatus.FOUND);
                        response.getHeaders().setLocation(URI.create(redirectUrl));
                        return response.setComplete();
                    } catch (java.io.UnsupportedEncodingException e) {
                        log.error("URL encoding failed: {}", e.getMessage());
                        return redirectToError(exchange);
                    }
                })
                .onErrorResume(e -> {
                    log.error("GitHub OAuth callback error: {}", e.getMessage(), e);
                    return redirectToError(exchange);
                });
    }
    
    private Mono<Void> redirectToError(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(URI.create("http://localhost:5173/auth/error"));
        return response.setComplete();
    }

}
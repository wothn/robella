package org.elmo.robella.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.model.response.LoginResponse;
import org.elmo.robella.service.GitHubOAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public Mono<ResponseEntity<LoginResponse>> callback(
            @RequestParam String code,
            @RequestParam String state,
            ServerWebExchange exchange) {
        
        return gitHubOAuthService.handleOAuthCallback(code, state)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("GitHub OAuth callback error: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
        
    }

}
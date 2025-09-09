package org.elmo.robella.controller;

import org.elmo.robella.model.entity.User;
import org.elmo.robella.model.request.LoginRequest;
import org.elmo.robella.model.response.LoginResponse;
import org.elmo.robella.model.response.UserResponse;
import org.elmo.robella.service.UserService;
import org.elmo.robella.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public Mono<ResponseEntity<UserResponse>> createUser(@Valid @RequestBody User user) {
        return userService.createUser(user)
                .map(createdUser -> ResponseEntity.status(HttpStatus.CREATED).body(createdUser))
                .onErrorResume(e -> {
                    log.error("创建用户失败: {}", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<UserResponse>> getUserById(@PathVariable @NotNull Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("获取用户失败: {}", e.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @GetMapping("/username/{username}")
    public Mono<ResponseEntity<UserResponse>> getUserByUsername(@PathVariable @NotBlank String username) {
        return userService.getUserByUsername(username)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("获取用户失败: {}", e.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @GetMapping("/email/{email}")
    public Mono<ResponseEntity<UserResponse>> getUserByEmail(@PathVariable @NotBlank @Email String email) {
        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("获取用户失败: {}", e.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @GetMapping
    public Flux<UserResponse> getAllUsers() {
        return userService.getAllUsers()
                .onErrorResume(e -> {
                    log.error("获取用户列表失败: {}", e.getMessage());
                    return Flux.empty();
                });
    }

    @GetMapping("/active")
    public Flux<UserResponse> getActiveUsers() {
        return userService.getActiveUsers()
                .onErrorResume(e -> {
                    log.error("获取活跃用户失败: {}", e.getMessage());
                    return Flux.empty();
                });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<UserResponse>> updateUser(
            @PathVariable @NotNull Long id,
            @Valid @RequestBody User user) {
        return userService.updateUser(id, user)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("更新用户失败: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable @NotNull Long id) {
        return userService.deleteUser(id)
                .thenReturn(ResponseEntity.noContent().<Void>build())
                .onErrorResume(e -> {
                    log.error("删除用户失败: {}", e.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @PutMapping("/{id}/activate")
    public Mono<ResponseEntity<UserResponse>> activateUser(@PathVariable @NotNull Long id) {
        return userService.activateUser(id)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("激活用户失败: {}", e.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @PutMapping("/{id}/deactivate")
    public Mono<ResponseEntity<UserResponse>> deactivateUser(@PathVariable @NotNull Long id) {
        return userService.deactivateUser(id)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("停用用户失败: {}", e.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        return userService.login(loginRequest)
                .flatMap(tokens -> {
                    ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
                            .httpOnly(true)
                            .secure(true)
                            .sameSite("Lax")
                            .path("/api/user/refresh")
                            .maxAge(jwtUtil.getRefreshTokenExpiration())
                            .build();
                    LoginResponse loginResponse = new LoginResponse(tokens.getAccessToken());
                    return Mono.just(ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, cookie.toString())
                            .body(loginResponse));
                })
                .onErrorResume(e -> {
                    log.error("登录失败: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(ServerWebExchange exchange) {

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/user/refresh")
                .maxAge(0) // 设置 Max-Age=0 指示浏览器立即删除此cookie
                .build();

        return Mono.just(
                ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, cookie.toString())
                        .build() // 这里构建的是 ResponseEntity<Void>
        );
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<LoginResponse>> refreshToken(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getCookies().getFirst("refreshToken"))
                .map(cookie -> cookie.getValue())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("刷新令牌不存在")))
                .filter(token -> !token.isBlank())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("刷新令牌为空")))
                .flatMap(userService::refreshToken)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("客户端错误: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("服务器内部错误: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<UserResponse>> getCurrentUser(ServerWebExchange exchange) {
        String username = exchange.getAttribute("username");

        if (username == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        return userService.getUserByUsername(username).map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build())).onErrorResume(e -> {
                    log.error("获取当前用户失败: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

}
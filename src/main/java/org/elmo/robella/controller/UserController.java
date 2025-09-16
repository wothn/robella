package org.elmo.robella.controller;

import org.elmo.robella.model.entity.User;
import org.elmo.robella.model.request.LoginRequest;
import org.elmo.robella.model.request.RefreshTokenRequest;
import org.elmo.robella.model.response.LoginResponse;
import org.elmo.robella.model.response.UserResponse;
import org.elmo.robella.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController {

    private final UserService userService;

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
                .map(tokens -> {
                    LoginResponse loginResponse = new LoginResponse(tokens.getAccessToken(), tokens.getRefreshToken());
                    return ResponseEntity.ok(loginResponse);
                })
                .onErrorResume(e -> {
                    log.error("登录失败: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<LoginResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        return userService.refreshToken(request.getRefreshToken())
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("刷新令牌失败: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
                });
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<UserResponse>> getCurrentUser() {
        return Mono.deferContextual(contextView -> {
            String username = contextView.get("username");
            
            if (username == null) {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).<UserResponse>build());
            }
            log.info("当前用户: {}", username);

            return userService.getUserByUsername(username).map(ResponseEntity::ok)
                    .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).<UserResponse>build()));
        }).onErrorResume(e -> {
            log.error("获取当前用户失败: {}", e.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<UserResponse>build());
        });
    }

}
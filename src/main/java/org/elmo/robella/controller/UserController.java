package org.elmo.robella.controller;

import org.elmo.robella.model.LoginRequest;
import org.elmo.robella.model.LoginResponse;
import org.elmo.robella.model.UserDTO;
import org.elmo.robella.model.UserResponse;
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
import jakarta.validation.constraints.Email;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    public Mono<ResponseEntity<UserResponse>> createUser(@Valid @RequestBody UserDTO userDTO) {
        return userService.createUser(userDTO)
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
    
    @GetMapping("/role/{role}")
    public Flux<UserResponse> getUsersByRole(@PathVariable @NotBlank String role) {
        return userService.getUsersByRole(role)
            .onErrorResume(e -> {
                log.error("根据角色获取用户失败: {}", e.getMessage());
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
            @Valid @RequestBody UserDTO userDTO) {
        return userService.updateUser(id, userDTO)
            .map(ResponseEntity::ok)
            .onErrorResume(e -> {
                log.error("更新用户失败: {}", e.getMessage());
                if (e instanceof IllegalArgumentException) {
                    return Mono.just(ResponseEntity.badRequest().build());
                }
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
    public Mono<ResponseEntity<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        return userService.login(loginRequest)
            .map(ResponseEntity::ok)
            .onErrorResume(e -> {
                log.error("登录失败: {}", e.getMessage());
                if (e instanceof IllegalArgumentException) {
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                }
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            });
    }
    
    @PostMapping("/{id}/login")
    public Mono<ResponseEntity<UserResponse>> recordLogin(@PathVariable @NotNull Long id) {
        return userService.updateLastLogin(id)
            .map(ResponseEntity::ok)
            .onErrorResume(e -> {
                log.error("记录登录时间失败: {}", e.getMessage());
                return Mono.just(ResponseEntity.notFound().build());
            });
    }
}
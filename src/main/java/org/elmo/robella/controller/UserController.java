package org.elmo.robella.controller;

import org.elmo.robella.annotation.RequiredRole;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.entity.User;
import org.elmo.robella.model.request.LoginRequest;
import org.elmo.robella.model.request.RefreshTokenRequest;
import org.elmo.robella.model.request.UserProfileUpdateRequest;
import org.elmo.robella.model.response.LoginResponse;
import org.elmo.robella.model.response.UserResponse;
import org.elmo.robella.service.UserService;
import org.elmo.robella.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping
    @RequiredRole(Role.ADMIN)
    public Mono<UserResponse> createUser(@Valid @RequestBody User user) {
        return userService.createUser(user);
    }

    @GetMapping("/{id}")
    public Mono<UserResponse> getUserById(@PathVariable @NotNull Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/username/{username}")
    public Mono<UserResponse> getUserByUsername(@PathVariable @NotBlank String username) {
        return userService.getUserByUsername(username);
    }

    @GetMapping
    @RequiredRole(Role.ADMIN)
    public Flux<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/active")
    @RequiredRole(Role.ADMIN)
    public Flux<UserResponse> getActiveUsers() {
        return userService.getActiveUsers();
    }

    @PutMapping("/{id}")
    @RequiredRole(Role.ADMIN)
    public Mono<UserResponse> updateUser(
            @PathVariable @NotNull Long id,
            @Valid @RequestBody User user) {
        return userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    @RequiredRole(Role.ADMIN)
    public Mono<Void> deleteUser(@PathVariable @NotNull Long id) {
        return userService.deleteUser(id);
    }

    @PutMapping("/{id}/activate")
    @RequiredRole(Role.ADMIN)
    public Mono<UserResponse> activateUser(@PathVariable @NotNull Long id) {
        return userService.activateUser(id);
    }

    @PutMapping("/{id}/deactivate")
    @RequiredRole(Role.ADMIN)
    public Mono<UserResponse> deactivateUser(@PathVariable @NotNull Long id) {
        return userService.deactivateUser(id);
    }

    @PostMapping("/login")
    public Mono<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return userService.login(loginRequest)
                .map(tokens -> new LoginResponse(tokens.getAccessToken(), tokens.getRefreshToken()));
    }

    @PostMapping("/refresh")
    public Mono<LoginResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return userService.refreshToken(request.getRefreshToken());
    }

    @GetMapping("/me")
    public Mono<UserResponse> getCurrentUser() {
        return Mono.deferContextual(contextView -> {
            String username = contextView.get("username");

            if (username == null) {
                return Mono.error(new InvalidCredentialsException());
            }
            log.info("当前用户: {}", username);

            return userService.getUserByUsername(username);
        });
    }

    @PutMapping("/me")
    public Mono<UserResponse> updateCurrentUser(
            @Valid @RequestBody UserProfileUpdateRequest updateRequest) {
        return Mono.deferContextual(contextView -> {
            String username = contextView.get("username");

            if (username == null) {
                return Mono.error(new InvalidCredentialsException());
            }

            log.info("更新用户资料: {}", username);

            return userService.updateUserProfile(username, updateRequest);
        });
    }
}
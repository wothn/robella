package org.elmo.robella.controller;

import org.elmo.robella.annotation.RequiredRole;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.entity.User;
import org.elmo.robella.model.request.LoginRequest;
import org.elmo.robella.model.request.RefreshTokenRequest;
import org.elmo.robella.model.request.UserProfileUpdateRequest;
import org.elmo.robella.model.request.UserCreateRequest;
import org.elmo.robella.model.request.UserUpdateRequest;
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
    public Mono<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return userService.createUser(request);
    }

    @GetMapping
    @RequiredRole(Role.ADMIN)
    public Flux<UserResponse> getUsers(@RequestParam(required = false) Boolean active) {
        return userService.getUsers(active);
    }

    @GetMapping("/{id}")
    @RequiredRole(Role.ADMIN)
    public Mono<UserResponse> getUserById(@PathVariable @NotNull Long id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    @RequiredRole(Role.ADMIN)
    public Mono<UserResponse> updateUser(
            @PathVariable @NotNull Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateUser(id, request);
    }

    @PutMapping("/{id}/active")
    @RequiredRole(Role.ADMIN)
    public Mono<UserResponse> setUserActive(
            @PathVariable @NotNull Long id,
            @RequestParam Boolean active) {
        return userService.setUserActive(id, active);
    }

    @DeleteMapping("/{id}")
    @RequiredRole(Role.ADMIN)
    public Mono<Void> deleteUser(@PathVariable @NotNull Long id) {
        return userService.deleteUser(id);
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
}
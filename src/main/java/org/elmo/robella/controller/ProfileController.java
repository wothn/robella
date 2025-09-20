package org.elmo.robella.controller;

import org.elmo.robella.model.request.UserProfileUpdateRequest;
import org.elmo.robella.model.response.UserResponse;
import org.elmo.robella.service.UserService;
import org.elmo.robella.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public Mono<UserResponse> getCurrentUser() {
        return Mono.deferContextual(contextView -> {
            String username = contextView.get("username");

            if (username == null) {
                return Mono.error(new InvalidCredentialsException());
            }
            log.info("获取当前用户信息: {}", username);

            return userService.getUserByUsername(username);
        });
    }

    @PutMapping
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

    @DeleteMapping
    public Mono<Void> deleteCurrentUser() {
        return Mono.deferContextual(contextView -> {
            String username = contextView.get("username");

            if (username == null) {
                return Mono.error(new InvalidCredentialsException());
            }

            log.info("删除当前用户: {}", username);

            return userService.deleteUserByUsername(username);
        });
    }

    @PutMapping("/password")
    public Mono<Void> changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        return Mono.deferContextual(contextView -> {
            String username = contextView.get("username");

            if (username == null) {
                return Mono.error(new InvalidCredentialsException());
            }

            log.info("修改用户密码: {}", username);

            return userService.changePassword(username, currentPassword, newPassword);
        });
    }
}
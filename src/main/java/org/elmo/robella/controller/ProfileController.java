package org.elmo.robella.controller;

import org.elmo.robella.model.request.UserProfileUpdateRequest;
import org.elmo.robella.model.response.UserResponse;
import org.elmo.robella.service.UserService;
import org.elmo.robella.exception.InvalidCredentialsException;
import org.elmo.robella.context.RequestContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public UserResponse getCurrentUser() {
        String username = RequestContextHolder.getContext() != null ?
            RequestContextHolder.getContext().getUsername() : null;

        if (username == null) {
            throw new InvalidCredentialsException();
        }
        log.info("获取当前用户信息: {}", username);

        return userService.getUserByUsername(username);
    }

    @PutMapping
    public UserResponse updateCurrentUser(
            @Valid @RequestBody UserProfileUpdateRequest updateRequest) {
        String username = RequestContextHolder.getContext() != null ?
            RequestContextHolder.getContext().getUsername() : null;

        if (username == null) {
            throw new InvalidCredentialsException();
        }

        log.info("更新用户资料: {}", username);

        return userService.updateUserProfile(username, updateRequest);
    }

    @DeleteMapping
    public void deleteCurrentUser() {
        String username = RequestContextHolder.getContext() != null ?
            RequestContextHolder.getContext().getUsername() : null;

        if (username == null) {
            throw new InvalidCredentialsException();
        }

        log.info("删除当前用户: {}", username);

        userService.deleteUserByUsername(username);
    }

    @PutMapping("/password")
    public void changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        String username = RequestContextHolder.getContext() != null ?
            RequestContextHolder.getContext().getUsername() : null;

        if (username == null) {
            throw new InvalidCredentialsException();
        }

        log.info("修改用户密码: {}", username);

        userService.changePassword(username, currentPassword, newPassword);
    }
}
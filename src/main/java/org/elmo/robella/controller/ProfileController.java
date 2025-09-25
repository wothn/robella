package org.elmo.robella.controller;

import org.elmo.robella.model.request.UserProfileUpdateRequest;
import org.elmo.robella.model.response.UserResponse;
import org.elmo.robella.service.UserService;
import org.elmo.robella.common.ErrorCodeConstants;
import org.elmo.robella.context.RequestContextHolder;
import org.elmo.robella.exception.BusinessException;

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
        Long userId = RequestContextHolder.getContext() != null ?
            RequestContextHolder.getContext().getUserId() : null;

        if (userId == null) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "User not authenticated");
        }
        log.info("获取当前用户信息: {}", userId);

        return userService.getUserById(userId);
    }

    @PutMapping
    public UserResponse updateCurrentUser(
            @Valid @RequestBody UserProfileUpdateRequest updateRequest) {
        Long userId = RequestContextHolder.getContext() != null ?
            RequestContextHolder.getContext().getUserId() : null;

        if (userId == null) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "User not authenticated");
        }

        log.info("更新用户资料: {}", userId);

        return userService.updateUserProfile(userId, updateRequest);
    }

    @DeleteMapping
    public void deleteCurrentUser() {
        Long userId = RequestContextHolder.getContext() != null ?
            RequestContextHolder.getContext().getUserId() : null;

        if (userId == null) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "User not authenticated");
        }

        log.info("删除当前用户: {}", userId);

        userService.removeById(userId);
    }

    

    @PutMapping("/password")
    public void changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        String username = RequestContextHolder.getContext() != null ?
            RequestContextHolder.getContext().getUsername() : null;

        if (username == null) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "User not authenticated");
        }

        log.info("修改用户密码: {}", username);

        userService.changePassword(username, currentPassword, newPassword);
    }
}
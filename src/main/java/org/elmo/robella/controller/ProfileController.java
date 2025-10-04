package org.elmo.robella.controller;

import org.elmo.robella.model.request.UserProfileUpdateRequest;
import org.elmo.robella.model.response.UserResponse;
import org.elmo.robella.service.UserService;
import org.elmo.robella.common.ErrorCodeConstants;
import org.elmo.robella.context.RequestContextHolder;
import org.elmo.robella.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<UserResponse> getCurrentUser() {
        Long userId = RequestContextHolder.getContext() != null ?
            RequestContextHolder.getContext().getUserId() : null;

        if (userId == null) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "User not authenticated");
        }
        log.info("获取当前用户信息: {}", userId);

        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PutMapping
    public ResponseEntity<Boolean> updateCurrentUser(
            @Valid @RequestBody UserProfileUpdateRequest updateRequest) {
        Long userId = RequestContextHolder.getContext() != null ?
            RequestContextHolder.getContext().getUserId() : null;

        if (userId == null) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "User not authenticated");
        }

        log.info("更新用户资料: {}", userId);

        return ResponseEntity.ok(userService.updateUserProfile(userId, updateRequest));
    }

    @DeleteMapping
    public ResponseEntity<Boolean> deleteCurrentUser() {
        Long userId = RequestContextHolder.getContext() != null ?
            RequestContextHolder.getContext().getUserId() : null;

        if (userId == null) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "User not authenticated");
        }

        log.info("删除当前用户: {}", userId);

        return ResponseEntity.ok(userService.removeById(userId));
    }

    

    @PutMapping("/password")
    public ResponseEntity<Boolean> changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        Long userId = RequestContextHolder.getContext() != null ?
            RequestContextHolder.getContext().getUserId() : null;

        if (userId == null) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "User not authenticated");
        }

        log.info("修改用户密码: {}", userId);

        return ResponseEntity.ok(userService.changePassword(userId, currentPassword, newPassword));
    }

    @DeleteMapping("/github")
    public ResponseEntity<Boolean> unbindGitHubAccount() {
        Long userId = RequestContextHolder.getContext() != null ?
            RequestContextHolder.getContext().getUserId() : null;

        if (userId == null) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "User not authenticated");
        }

        log.info("取消 GitHub 绑定: {}", userId);
        return ResponseEntity.ok(userService.unlinkGitHubAccount(userId));
    }
}
package org.elmo.robella.controller;

import org.elmo.robella.annotation.RequiredRole;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.request.LoginRequest;
import org.elmo.robella.model.request.UserCreateRequest;
import org.elmo.robella.model.request.UserUpdateRequest;
import org.elmo.robella.model.response.UserResponse;
import org.elmo.robella.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping
    @RequiredRole(Role.ADMIN)
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @GetMapping
    @RequiredRole(Role.ADMIN)
    public ResponseEntity<List<UserResponse>> getUsers(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(userService.getUsers(active));
    }

    @GetMapping("/{id}")
    @RequiredRole(Role.ADMIN)
    public ResponseEntity<UserResponse> getUserById(@PathVariable @NotNull Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @RequiredRole(Role.ADMIN)
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable @NotNull Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PutMapping("/{id}/active")
    @RequiredRole(Role.ADMIN)
    public ResponseEntity<UserResponse> setUserActive(
            @PathVariable @NotNull Long id,
            @RequestParam Boolean active) {
        return ResponseEntity.ok(userService.setUserActive(id, active));
    }

    @DeleteMapping("/{id}")
    @RequiredRole(Role.ADMIN)
    public ResponseEntity<Void> deleteUser(@PathVariable @NotNull Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<SaResult> login(
            @Valid @RequestBody LoginRequest loginRequest) {
        userService.login(loginRequest);

        return ResponseEntity.ok(SaResult.ok("登录成功"));
    }

    @PostMapping("/logout")
    public ResponseEntity<SaResult> logout() {
        StpUtil.logout();
        return ResponseEntity.ok(SaResult.ok("退出成功"));
    }
}
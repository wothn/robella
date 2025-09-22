package org.elmo.robella.controller;

import org.elmo.robella.annotation.RequiredRole;
import org.elmo.robella.exception.ErrorCode;
import org.elmo.robella.exception.RefreshTokenException;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.request.LoginRequest;
import org.elmo.robella.model.request.RefreshTokenRequest;
import org.elmo.robella.model.request.UserCreateRequest;
import org.elmo.robella.model.request.UserUpdateRequest;
import org.elmo.robella.model.response.LoginResponse;
import org.elmo.robella.model.response.UserResponse;
import org.elmo.robella.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import jakarta.servlet.http.Cookie;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping
    @RequiredRole(Role.ADMIN)
    public UserResponse createUser(@Valid @RequestBody UserCreateRequest request) {
        return userService.createUser(request);
    }

    @GetMapping
    @RequiredRole(Role.ADMIN)
    public List<UserResponse> getUsers(@RequestParam(required = false) Boolean active) {
        return userService.getUsers(active);
    }

    @GetMapping("/{id}")
    @RequiredRole(Role.ADMIN)
    public UserResponse getUserById(@PathVariable @NotNull Long id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    @RequiredRole(Role.ADMIN)
    public UserResponse updateUser(
            @PathVariable @NotNull Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateUser(id, request);
    }

    @PutMapping("/{id}/active")
    @RequiredRole(Role.ADMIN)
    public UserResponse setUserActive(
            @PathVariable @NotNull Long id,
            @RequestParam Boolean active) {
        return userService.setUserActive(id, active);
    }

    @DeleteMapping("/{id}")
    @RequiredRole(Role.ADMIN)
    public void deleteUser(@PathVariable @NotNull Long id) {
        userService.deleteUser(id);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {
        var tokens = userService.login(loginRequest);

        // Set refreshToken in HttpOnly cookie
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("refreshToken", tokens.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
        response.addCookie(cookie);

        // Return response with accessToken only
        return new LoginResponse(tokens.getAccessToken());
    }

    @PostMapping("/refresh")
    public LoginResponse refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null) {
            throw new RefreshTokenException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        LoginResponse loginResponse = userService.refreshToken(refreshToken);

        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
        response.addCookie(cookie);

        // Return response with accessToken only
        return new LoginResponse(loginResponse.getAccessToken());
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        // Clear refreshToken cookie by setting maxAge to -1
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(-1);
        response.addCookie(cookie);
    }
}
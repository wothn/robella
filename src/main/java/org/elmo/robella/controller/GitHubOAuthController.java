package org.elmo.robella.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.elmo.robella.service.GitHubOAuthService;

import jakarta.servlet.http.Cookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/oauth/github")
@RequiredArgsConstructor
@Slf4j
public class GitHubOAuthController {

    private final GitHubOAuthService gitHubOAuthService;

    @GetMapping("/login")
    public RedirectView login() {
        String authUrl = gitHubOAuthService.generateAuthorizationUrl();
        return new RedirectView(authUrl);
    }

    @GetMapping("/callback")
    public RedirectView callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            HttpServletResponse response) {


        if (code == null || state == null) {
            log.warn("GitHub OAuth callback called without required parameters");
            return new RedirectView("http://localhost:5173/auth/error");
        }

        try {
            String refreshToken = gitHubOAuthService.handleOAuthCallback(code, state);
            log.info("GitHub OAuth service returned login response - accessToken: {}, refreshToken: {}",
                    refreshToken != null ? "present" : "missing");

            // Set refreshToken in HttpOnly cookie
            Cookie cookie = new Cookie("refreshToken", refreshToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // Set to true in production with HTTPS
            cookie.setPath("/");
            cookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
            response.addCookie(cookie);

            // 重定向到前端成功页面，token由前端通过refreshToken刷新获取
            String redirectUrl = "http://localhost:5173/auth/success";

            log.info("Redirecting to frontend success page: {}", redirectUrl);
            return new RedirectView(redirectUrl);
        } catch (Exception e) {
            log.error("GitHub OAuth callback error: {}", e.getMessage(), e);
            return new RedirectView("http://localhost:5173/auth/error");
        }
    }

}
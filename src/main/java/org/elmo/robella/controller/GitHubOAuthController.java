package org.elmo.robella.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.service.GitHubOAuthService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

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
            @RequestParam(required = false) String state) {

        log.info("GitHub OAuth callback received - code: {}, state: {}",
                code != null ? "present" : "missing",
                state != null ? "present" : "missing");

        if (code == null || state == null) {
            log.warn("GitHub OAuth callback called without required parameters");
            return new RedirectView("http://localhost:5173/auth/error");
        }

        try {
            var loginResponse = gitHubOAuthService.handleOAuthCallback(code, state);
            log.info("GitHub OAuth service returned login response - accessToken: {}, refreshToken: {}",
                    loginResponse.getAccessToken() != null ? "present" : "missing",
                    loginResponse.getRefreshToken() != null ? "present" : "missing");

            // 生成JWT token并添加到重定向URL的参数中，确保URL编码
            String encodedToken = java.net.URLEncoder.encode(loginResponse.getAccessToken(), "UTF-8");
            String encodedRefreshToken = java.net.URLEncoder.encode(loginResponse.getRefreshToken(), "UTF-8");
            String redirectUrl = "http://localhost:5173/auth/success?token=" + encodedToken + "&refreshToken=" + encodedRefreshToken;

            log.info("Redirecting to frontend success page: {}", redirectUrl);
            return new RedirectView(redirectUrl);
        } catch (Exception e) {
            log.error("GitHub OAuth callback error: {}", e.getMessage(), e);
            return new RedirectView("http://localhost:5173/auth/error");
        }
    }

}
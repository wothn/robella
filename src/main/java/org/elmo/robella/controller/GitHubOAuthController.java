package org.elmo.robella.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.exception.ApiException;
import org.elmo.robella.exception.BusinessException;
import org.elmo.robella.service.GitHubOAuthService;
import org.elmo.robella.service.GitHubOAuthService.OAuthCallbackResult;
import org.elmo.robella.service.GitHubOAuthService.OAuthFlowType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import cn.dev33.satoken.stp.StpUtil;

@RestController
@RequestMapping("/api/oauth/github")
@RequiredArgsConstructor
@Slf4j
public class GitHubOAuthController {

    private final GitHubOAuthService gitHubOAuthService;

    @Value("${github.oauth.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${github.oauth.login-success-path:/auth/success}")
    private String loginSuccessPath;

    @Value("${github.oauth.login-error-path:/auth/error}")
    private String loginErrorPath;

    @Value("${github.oauth.bind-success-path:/profile?github=success}")
    private String bindSuccessPath;

    @Value("${github.oauth.bind-error-path:/profile?github=error}")
    private String bindErrorPath;

    @GetMapping("/login")
    public RedirectView login() {
        String authUrl = gitHubOAuthService.generateAuthorizationUrl();
        return new RedirectView(authUrl);
    }

    @GetMapping("/bind")
    public RedirectView bind() {
        if (!StpUtil.isLogin()) {
            log.warn("GitHub bind requested without active session");
            return new RedirectView(buildRedirectUrl(OAuthFlowType.BIND, false));
        }

        Long userId = StpUtil.getLoginIdAsLong();
        log.info("Initiating GitHub bind flow for user {}", userId);
        String authUrl = gitHubOAuthService.generateAuthorizationUrlForBinding(userId);
        return new RedirectView(authUrl);
    }

    @GetMapping("/callback")
    public RedirectView callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            HttpServletResponse response) {


        if (code == null || state == null) {
            log.warn("GitHub OAuth callback called without required parameters");
            return new RedirectView(buildRedirectUrl(OAuthFlowType.LOGIN, false));
        }

        OAuthFlowType flowType = gitHubOAuthService.peekFlowType(state);
        try {
            OAuthCallbackResult result = gitHubOAuthService.handleOAuthCallback(code, state);

            String redirectUrl = buildRedirectUrl(result.flowType(), true);
            log.info("Redirecting to frontend success page: {}", redirectUrl);
            return new RedirectView(redirectUrl);
        } catch (BusinessException | ApiException e) {
            log.error("GitHub OAuth callback error: {}", e.getMessage(), e);
            return new RedirectView(buildRedirectUrl(flowType, false));
        } catch (Exception e) {
            log.error("GitHub OAuth callback unexpected error", e);
            return new RedirectView(buildRedirectUrl(flowType, false));
        }
    }

    private String buildRedirectUrl(OAuthFlowType flowType, boolean success) {
        String path = switch (flowType) {
            case BIND -> success ? bindSuccessPath : bindErrorPath;
            case LOGIN -> success ? loginSuccessPath : loginErrorPath;
        };

        if (path.startsWith("http")) {
            return path;
        }

        boolean baseEndsWithSlash = frontendBaseUrl.endsWith("/");
        if (path.startsWith("/")) {
            return baseEndsWithSlash ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1) + path : frontendBaseUrl + path;
        }

        return baseEndsWithSlash ? frontendBaseUrl + path : frontendBaseUrl + "/" + path;
    }
}
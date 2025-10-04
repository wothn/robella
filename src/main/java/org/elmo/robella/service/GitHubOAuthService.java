package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.model.dto.GitHubUserInfo;
import org.elmo.robella.model.dto.GithubAccessTokenResponse;
import org.elmo.robella.model.entity.User;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.common.ErrorCodeConstants;
import org.elmo.robella.exception.ApiException;
import org.elmo.robella.exception.BusinessException;
import org.elmo.robella.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import cn.dev33.satoken.stp.StpUtil;

import org.elmo.robella.util.JsonUtils;

import org.elmo.robella.util.OkHttpUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubOAuthService {
    
    private final Map<String, OAuthState> stateStore = new ConcurrentHashMap<>();
    private final UserMapper userMapper;
    
    private final OkHttpUtils okHttpUtils;
    private final JsonUtils jsonUtils;

    @Value("${github.oauth.client-id}")
    private String clientId;

    @Value("${github.oauth.client-secret}")
    private String clientSecret;

    @Value("${github.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${github.oauth.scope}")
    private String scope;

    @Value("${github.oauth.auth-url}")
    private String authUrl;

    @Value("${github.oauth.token-url}")
    private String tokenUrl;

    @Value("${github.oauth.user-url}")
    private String userUrl;

    public String generateAuthorizationUrl() {
        return generateAuthorizationUrlInternal(OAuthFlowType.LOGIN, null);
    }

    public String generateAuthorizationUrlForBinding(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "User not authenticated");
        }
        return generateAuthorizationUrlInternal(OAuthFlowType.BIND, userId);
    }

    private String generateAuthorizationUrlInternal(OAuthFlowType flowType, Long userId) {
        String state = generateState();
        stateStore.put(state, new OAuthState(flowType, userId, Instant.now()));

        return String.format("%s?client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                authUrl, clientId, redirectUri, scope, state);
    }

    public OAuthCallbackResult handleOAuthCallback(String code, String state) {
        OAuthState oauthState = getValidState(state);
        try {
            // 获取access token
            String accessToken = getAccessToken(code, state);
            GitHubUserInfo userInfo = getUserInfo(accessToken);
            String githubId = String.valueOf(userInfo.getId());

            if (oauthState.flowType() == OAuthFlowType.LOGIN) {
                User user = findOrCreateGitHubUser(userInfo);
                StpUtil.login(user.getId());
                log.info("GitHub OAuth login successful for user {}", user.getUsername());
                return new OAuthCallbackResult(OAuthFlowType.LOGIN, user.getId(), githubId);
            }

            bindGitHubUserToExistingAccount(oauthState.userId(), userInfo);
            log.info("GitHub account {} bound to user {}", githubId, oauthState.userId());
            return new OAuthCallbackResult(OAuthFlowType.BIND, oauthState.userId(), githubId);
        } catch (BusinessException e) {
            log.error("GitHub OAuth processing failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("GitHub OAuth processing unexpected error: {}", e.getMessage(), e);
            throw new ApiException(ErrorCodeConstants.INTERNAL_ERROR, "GitHub OAuth processing failed");
        } finally {
            stateStore.remove(state);
        }
    }

    private User findOrCreateGitHubUser(GitHubUserInfo userInfo) throws IOException {
        String githubId = String.valueOf(userInfo.getId());
        String username = userInfo.getLogin();
        String email = userInfo.getEmail();
        String name = userInfo.getName();
        String avatar = userInfo.getAvatarUrl();

        final String finalEmail = (email == null || email.trim().isEmpty()) ? username + "@github.local" : email;
        final String finalName = (name == null || name.trim().isEmpty()) ? username : name;

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getGithubId, githubId);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            User newUser = User.builder()
                    .username(username)
                    .email(finalEmail)
                    .displayName(finalName)
                    .avatar(avatar)
                    .githubId(githubId)
                    .active(true)
                    .role(Role.USER)
                    .password(null) // OAuth用户没有密码
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            userMapper.insert(newUser);
            user = newUser;
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userMapper.updateById(user);
        return user;
    }

    private void bindGitHubUserToExistingAccount(Long userId, GitHubUserInfo userInfo) {
        if (userId == null) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "User not authenticated");
        }

        String githubId = String.valueOf(userInfo.getId());

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getGithubId, githubId);
        User existingBinding = userMapper.selectOne(queryWrapper);
        if (existingBinding != null && !existingBinding.getId().equals(userId)) {
            throw new BusinessException(ErrorCodeConstants.RESOURCE_CONFLICT, "该 GitHub 账号已绑定到其他用户");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeConstants.RESOURCE_NOT_FOUND, "User not found");
        }

        user.setGithubId(githubId);
        if (!StringUtils.hasText(user.getAvatar()) && StringUtils.hasText(userInfo.getAvatarUrl())) {
            user.setAvatar(userInfo.getAvatarUrl());
        }
        if (!StringUtils.hasText(user.getDisplayName()) && StringUtils.hasText(userInfo.getName())) {
            user.setDisplayName(userInfo.getName());
        }
        user.setUpdatedAt(OffsetDateTime.now());
        userMapper.updateById(user);
    }

    private OAuthState getValidState(String state) {
        OAuthState oauthState = stateStore.get(state);
        if (oauthState == null) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "Invalid or expired state parameter");
        }

        Duration duration = Duration.between(oauthState.createdAt(), Instant.now());
        if (duration.toMinutes() >= 10) {
            stateStore.remove(state);
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "Invalid or expired state parameter");
        }
        return oauthState;
    }

    public OAuthFlowType peekFlowType(String state) {
        OAuthState oauthState = stateStore.get(state);
        return oauthState != null ? oauthState.flowType() : OAuthFlowType.LOGIN;
    }

    /**
     * 生成随机的state参数
     */
    private String generateState() {
        return UUID.randomUUID().toString();
    }

    public enum OAuthFlowType {
        LOGIN,
        BIND
    }

    private record OAuthState(OAuthFlowType flowType, Long userId, Instant createdAt) {
    }

    public record OAuthCallbackResult(OAuthFlowType flowType, Long userId, String githubId) {
    }

    public String getAccessToken(String code, String state) throws IOException {
        Map<String, String> headers = Map.of(
                "Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                "Accept", MediaType.APPLICATION_JSON_VALUE
        );

        Map<String, String> formData = Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "redirect_uri", redirectUri,
                "grant_type", "authorization_code"
        );

        String responseBody = okHttpUtils.postForm(tokenUrl, formData, headers);
        try {
            GithubAccessTokenResponse response = jsonUtils.fromJson(responseBody, GithubAccessTokenResponse.class);
            return response.getAccessToken();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse access token response", e);
        }
    }

    public GitHubUserInfo getUserInfo(String accessToken) throws IOException {
        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + accessToken,
                "Accept", MediaType.APPLICATION_JSON_VALUE
        );

        String responseBody = okHttpUtils.get(userUrl, headers);
        try {
            return jsonUtils.fromJson(responseBody, GitHubUserInfo.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse user info", e);
        }
    }
}
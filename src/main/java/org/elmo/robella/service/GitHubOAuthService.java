package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.model.dto.GitHubUserInfo;
import org.elmo.robella.model.dto.GithubAccessTokenResponse;
import org.elmo.robella.model.entity.User;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.elmo.robella.util.JsonUtils;
import org.elmo.robella.util.JwtUtil;
import org.elmo.robella.util.OkHttpUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubOAuthService {
    
    private final Map<String, String> stateStore = new ConcurrentHashMap<>();
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
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
        String state = generateState();
        stateStore.put(state, Instant.now().toString());

        return String.format("%s?client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                authUrl, clientId, redirectUri, scope, state);
    }

    public String handleOAuthCallback(String code, String state) {
        if (!validateState(state)) {
            throw new IllegalArgumentException("Invalid or expired state parameter");
        }

        try {
            // 获取access token
            String accessToken = getAccessToken(code, state);
            GitHubUserInfo userInfo = getUserInfo(accessToken);

            String refreshToken = processGitHubUser(userInfo);
            log.info("GitHub OAuth login successful");
            return refreshToken;
        } catch (Exception e) {
            log.error("GitHub OAuth login failed: {}", e.getMessage());
            throw new RuntimeException("GitHub OAuth login failed", e);
        }
    }

    private String processGitHubUser(GitHubUserInfo userInfo) throws IOException {
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
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return refreshToken;
    }

    private boolean validateState(String state) {
        String timestamp = stateStore.get(state);
        if (timestamp == null) {
            return false;
        }

        java.time.Instant stateTime = java.time.Instant.parse(timestamp);
        java.time.Instant now = java.time.Instant.now();
        java.time.Duration duration = java.time.Duration.between(stateTime, now);

        boolean isValid = duration.toMinutes() < 10;
        
        // Only remove state if it's valid (prevent replay attacks)
        if (isValid) {
            stateStore.remove(state);
        }
        
        return isValid;
    }

    /**
     * 生成随机的state参数
     */
    private String generateState() {
        return UUID.randomUUID().toString();
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
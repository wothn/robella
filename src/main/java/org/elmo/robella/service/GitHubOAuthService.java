package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.model.dto.GitHubUserInfo;
import org.elmo.robella.model.dto.GithubAccessTokenResponse;
import org.elmo.robella.model.entity.User;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.response.LoginResponse;
import org.elmo.robella.repository.UserRepository;
import org.elmo.robella.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubOAuthService {

    private final WebClient webClient;
    private final Map<String, String> stateStore = new ConcurrentHashMap<>();
    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

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

    public Mono<LoginResponse> handleOAuthCallback(String code, String state) {
        if (!validateState(state)) {
            return Mono.error(new IllegalArgumentException("Invalid or expired state parameter"));
        }

        // 获取access token
        return getAccessToken(code, state)
                .flatMap(this::getUserInfo)
                .flatMap(this::processGitHubUser)
                .doOnSuccess(response -> log.info("GitHub OAuth login successful"))
                .doOnError(error -> log.error("GitHub OAuth login failed: {}", error.getMessage()));
    }

    private Mono<LoginResponse> processGitHubUser(GitHubUserInfo userInfo) {
        String githubId = String.valueOf(userInfo.getId());
        String username = userInfo.getLogin();
        String email = userInfo.getEmail();
        String name = userInfo.getName();
        String avatar = userInfo.getAvatarUrl();

        final String finalEmail = (email == null || email.trim().isEmpty()) ? username + "@github.local" : email;
        final String finalName = (name == null || name.trim().isEmpty()) ? username : name;

        return userService.getUserByGithubId(githubId)
                .switchIfEmpty(Mono.defer(() -> {
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
                    
                    return userRepository.save(newUser);
                }))
                .flatMap(user -> {
                    user.setLastLoginAt(OffsetDateTime.now());
                    return userRepository.save(user);
                })
                .flatMap(user -> {
                    String accessToken = jwtUtil.generateAccessToken(user);
                    String refreshToken = jwtUtil.generateRefreshToken(user);
                    
                    return Mono.just(new LoginResponse(accessToken, refreshToken));
                });
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

    public Mono<String> getAccessToken(String code, String state) {
        return webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue("client_id=" + clientId + 
                          "&client_secret=" + clientSecret + 
                          "&code=" + code + 
                          "&redirect_uri=" + redirectUri + 
                          "&grant_type=authorization_code")
                .retrieve()
                .bodyToMono(GithubAccessTokenResponse.class)
                .map(GithubAccessTokenResponse::getAccessToken);
    }

    public Mono<GitHubUserInfo> getUserInfo(String accessToken) {
        return webClient.get()
                .uri(userUrl)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(GitHubUserInfo.class);
    }
}
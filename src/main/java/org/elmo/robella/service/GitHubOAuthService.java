package org.elmo.robella.service;

import org.elmo.robella.model.User;
import org.elmo.robella.model.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@Slf4j
public class GitHubOAuthService {

    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.github.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.github.user-info-uri}")
    private String userInfoUri;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private UserService userService;

    public String getAuthorizationUrl(String redirectUri, String state) {
    String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
    String stateParam = state != null && !state.isEmpty()
        ? "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8)
        : "";
    return String.format(
        "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=user:email,read:user%s",
        clientId, encodedRedirect, stateParam);
    }

    public Mono<User> exchangeCodeForUser(String code, String redirectUri) {
        return exchangeCodeForAccessToken(code, redirectUri)
                .flatMap(this::getUserInfo)
                .flatMap(this::findOrCreateUser);
    }

    private Mono<String> exchangeCodeForAccessToken(String code, String redirectUri) {
        return webClientBuilder.build()
                .post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Accept", "application/json") // 添加这一行来获取JSON响应
                .body(BodyInserters.fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("code", code)
                        .with("redirect_uri", redirectUri)
                        .with("grant_type", "authorization_code"))
                .retrieve()
                .bodyToMono(Map.class) // 直接解析为Map
                .doOnSuccess(response -> log.info("GitHub token response: {}", response))
                .map(response -> {
                    String accessToken = (String) response.get("access_token");
                    if (accessToken == null) {
                        throw new RuntimeException("Access token not found in response: " + response);
                    }
                    return accessToken;
                })
                .doOnSuccess(token -> log.info("Successfully obtained access token from GitHub"))
                .doOnError(error -> log.error("Failed to obtain access token from GitHub: {}", error.getMessage()));
    }

    @SuppressWarnings("unchecked")
    private Mono<Map<String, Object>> getUserInfo(String accessToken) {
        return webClientBuilder.build()
                .get()
                .uri(userInfoUri)
                .header("Authorization", "Bearer " + accessToken)
                .header("User-Agent", "Robella")
                .retrieve()
                .bodyToMono(Map.class)
                .map(map -> (Map<String, Object>) map)
                .doOnSuccess(userInfo -> log.info("Successfully obtained user info from GitHub: {}", userInfo))
                .doOnError(error -> log.error("Failed to obtain user info from GitHub: {}", error.getMessage()))
                .onErrorMap(ex -> new RuntimeException("Failed to get user info from GitHub: " + ex.getMessage(), ex));
    }

    private Mono<User> findOrCreateUser(Map<String, Object> userInfo) {
        String githubId = String.valueOf(userInfo.get("id"));
        String username = (String) userInfo.get("login");
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");
        String avatar = (String) userInfo.get("avatar_url");

        final String finalEmail = (email == null || email.trim().isEmpty()) ? username + "@github.local" : email;
        final String finalName = (name == null || name.trim().isEmpty()) ? username : name;

        return userService.getUserByGithubId(githubId)
                .switchIfEmpty(Mono.defer(() -> {
                    User newUser = User.builder()
                            .username(username)
                            .email(finalEmail)
                            .fullName(finalName)
                            .avatar(avatar)
                            .githubId(githubId)
                            .provider("github")
                            .providerId(githubId)
                            .active(true)
                            .role("USER")
                            .emailVerified("true")
                            .build();

                    return userService.createOAuthUser(newUser)
                            .map(this::convertToUser);
                }))
                .doOnSuccess(user -> log.info("Successfully found or created user for GitHub user: {}", username))
                .doOnError(error -> log.error("Failed to find or create user for GitHub user {}: {}", username,
                        error.getMessage()));
    }

    private User convertToUser(UserResponse userResponse) {
        return User.builder()
                .id(userResponse.getId())
                .username(userResponse.getUsername())
                .email(userResponse.getEmail())
                .fullName(userResponse.getFullName())
                .avatar(userResponse.getAvatar())
                .githubId(userResponse.getGithubId())
                .provider(userResponse.getProvider())
                .providerId(userResponse.getProviderId())
                .active(userResponse.getActive())
                .role(userResponse.getRole())
                .createdAt(userResponse.getCreatedAt())
                .updatedAt(userResponse.getUpdatedAt())
                .lastLoginAt(userResponse.getLastLoginAt())
                .emailVerified(String.valueOf(userResponse.getEmailVerified()))
                .phoneVerified(String.valueOf(userResponse.getPhoneVerified()))
                .build();
    }
}
package org.elmo.robella.service;

import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.dto.AuthTokens;
import org.elmo.robella.model.entity.User;
import org.elmo.robella.model.request.LoginRequest;
import org.elmo.robella.model.response.LoginResponse;
import org.elmo.robella.model.response.UserResponse;
import org.elmo.robella.repository.UserRepository;
import org.elmo.robella.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    
    public Mono<UserResponse> createUser(User user) {
        return userRepository.existsByUsername(user.getUsername())
            .flatMap(existsByUsername -> {
                if (existsByUsername) {
                    return Mono.error(new IllegalArgumentException("用户名已存在"));
                }
                return userRepository.existsByEmail(user.getEmail());
            })
            .flatMap(existsByEmail -> {
                if (existsByEmail) {
                    return Mono.error(new IllegalArgumentException("邮箱已存在"));
                }
                
                user.setActive(true);
                user.setRole(Role.USER);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                
                return userRepository.save(user);
            })
            .map(this::convertToResponse)
            .doOnSuccess(createdUser -> log.info("用户创建成功: {}", createdUser.getUsername()))
            .doOnError(error -> log.error("用户创建失败: {}", error.getMessage()));
    }
    
    public Mono<UserResponse> getUserById(Long id) {
        return userRepository.findById(id)
            .map(this::convertToResponse)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户不存在")));
    }
    
    public Mono<UserResponse> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .map(this::convertToResponse)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户不存在")));
    }
    
    public Mono<UserResponse> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .map(this::convertToResponse)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户不存在")));
    }
    
    public Flux<UserResponse> getAllUsers() {
        return userRepository.findAll()
            .map(this::convertToResponse);
    }
    
    public Flux<UserResponse> getActiveUsers() {
        return userRepository.findByActive(true)
            .map(this::convertToResponse);
    }
    
    public Mono<UserResponse> updateUser(Long id, User user) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户不存在")))
            .flatMap(existingUser -> {
                if (!existingUser.getUsername().equals(user.getUsername())) {
                    return userRepository.existsByUsername(user.getUsername())
                        .flatMap(exists -> exists ? 
                            Mono.error(new IllegalArgumentException("用户名已存在")) : 
                            Mono.just(existingUser));
                }
                return Mono.just(existingUser);
            })
            .flatMap(existingUser -> {
                if (!existingUser.getEmail().equals(user.getEmail())) {
                    return userRepository.existsByEmail(user.getEmail())
                        .flatMap(exists -> exists ? 
                            Mono.error(new IllegalArgumentException("邮箱已存在")) : 
                            Mono.just(existingUser));
                }
                return Mono.just(existingUser);
            })
            .flatMap(existingUser -> {
                BeanUtils.copyProperties(user, existingUser, "id", "password", "createdAt", "emailVerified", "phoneVerified");
                
                if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                    existingUser.setPassword(user.getPassword());
                }
                
                existingUser.setUpdatedAt(LocalDateTime.now());
                
                return userRepository.save(existingUser);
            })
            .map(this::convertToResponse)
            .doOnSuccess(updatedUser -> log.info("用户更新成功: {}", updatedUser.getUsername()))
            .doOnError(error -> log.error("用户更新失败: {}", error.getMessage()));
    }
    
    public Mono<Void> deleteUser(Long id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户不存在")))
            .flatMap(user -> userRepository.deleteById(id))
            .doOnSuccess(v -> log.info("用户删除成功: {}", id))
            .doOnError(error -> log.error("用户删除失败: {}", error.getMessage()));
    }
    
    public Mono<UserResponse> activateUser(Long id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户不存在")))
            .flatMap(user -> {
                user.setActive(true);
                user.setUpdatedAt(LocalDateTime.now());
                return userRepository.save(user);
            })
            .map(this::convertToResponse);
    }
    
    public Mono<UserResponse> deactivateUser(Long id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户不存在")))
            .flatMap(user -> {
                user.setActive(false);
                user.setUpdatedAt(LocalDateTime.now());
                return userRepository.save(user);
            })
            .map(this::convertToResponse);
    }
    
    
    public Mono<Boolean> validateUser(String username, String password) {
        return userRepository.findByUsername(username)
            .flatMap(user -> {
                if (!user.getActive()) {
                    return Mono.just(false);
                }
                return Mono.just(passwordEncoder.matches(password, user.getPassword()));
            })
            .defaultIfEmpty(false);
    }
    
    public Mono<AuthTokens> login(LoginRequest loginRequest) {
        return userRepository.findByUsername(loginRequest.getUsername())
            .flatMap(user -> {
                if (!user.getActive()) {
                    return Mono.error(new RuntimeException("用户已被禁用"));
                }
                
                if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                    return Mono.error(new RuntimeException("用户名或密码错误"));
                }
                user.setLastLoginAt(LocalDateTime.now());
                String accessToken = jwtUtil.generateAccessToken(user);
                String refreshToken = jwtUtil.generateRefreshToken(user);
                
                return userRepository.save(user)
                .then(Mono.just(createAuthTokens(accessToken, refreshToken)));
            })
            .switchIfEmpty(Mono.error(new RuntimeException("用户名或密码错误")));
    }
    
    public Mono<LoginResponse> refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            return Mono.error(new RuntimeException("无效的刷新令牌"));
        }

        String username = jwtUtil.extractUsername(refreshToken);

        return userRepository.findByUsername(username)
            .flatMap(user -> {
                if (!user.getActive()) {
                    return Mono.error(new RuntimeException("用户已被禁用"));
                }
                String newAccessToken = jwtUtil.generateAccessToken(user);
                LoginResponse response = new LoginResponse(newAccessToken);
                return Mono.just(response);
            })
            .switchIfEmpty(Mono.error(new RuntimeException("用户不存在")));
    }
    
    public Mono<Void> logout(String username) {
        return userRepository.findByUsername(username)
            .flatMap(user -> {
                user.setUpdatedAt(LocalDateTime.now());
                return userRepository.save(user).then();
            });
    }
    
    private AuthTokens createAuthTokens(String accessToken, String refreshToken) {
        AuthTokens tokens = new AuthTokens();
        tokens.setAccessToken(accessToken);
        tokens.setRefreshToken(refreshToken);
        return tokens;
    }
    
    public UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        BeanUtils.copyProperties(user, response);
        return response;
    }
    
    public Mono<User> getUserByGithubId(String githubId) {
        return userRepository.findByGithubId(githubId);
    }
    
    public Mono<UserResponse> createOAuthUser(User user) {
        return userRepository.existsByUsername(user.getUsername())
            .flatMap(existsByUsername -> {
                if (existsByUsername) {
                    return Mono.error(new IllegalArgumentException("用户名已存在"));
                }
                return userRepository.existsByEmail(user.getEmail());
            })
            .flatMap(existsByEmail -> {
                if (existsByEmail) {
                    return Mono.error(new IllegalArgumentException("邮箱已存在"));
                }
                
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                user.setActive(true);
                user.setRole(Role.GUEST);
                // For OAuth users, set password to null since they don't have one
                user.setPassword(null);
                
                return userRepository.save(user);
            })
            .map(this::convertToResponse)
            .doOnSuccess(u -> log.info("OAuth用户创建成功: {}", u.getUsername()))
            .doOnError(error -> log.error("OAuth用户创建失败: {}", error.getMessage()));
    }
    
    // OAuth登录响应逻辑已移至 AuthService
    
    public Mono<String> updatePassword(Long userId, String newPassword) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("用户不存在")))
            .flatMap(user -> {
                user.setPassword(newPassword);
                user.setUpdatedAt(LocalDateTime.now());
                return userRepository.save(user);
            })
            .map(user -> "密码更新成功")
            .doOnSuccess(v -> log.info("密码更新成功: {}", userId))
            .doOnError(error -> log.error("密码更新失败: {}", error.getMessage()));
    }
    
    public Mono<User> processGitHubOAuthUser(Map<String, Object> attributes) {
        String githubId = String.valueOf(attributes.get("id"));
        String username = (String) attributes.get("login");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String avatar = (String) attributes.get("avatar_url");
        
        final String finalEmail = (email == null || email.trim().isEmpty()) ? username + "@github.local" : email;
        final String finalName = (name == null || name.trim().isEmpty()) ? username : name;
        
        return getUserByGithubId(githubId)
            .switchIfEmpty(Mono.defer(() -> {
                User newUser = User.builder()
                    .username(username)
                    .email(finalEmail)
                    .displayName(finalName)
                    .avatar(avatar)
                    .githubId(githubId)
                    .githubId(githubId)
                    .active(true)
                    .role(Role.USER)
                    .build();
                
                return userRepository.save(newUser);
            }))
            .doOnSuccess(user -> log.info("Successfully found or created user for GitHub user: {}", username))
            .doOnError(error -> log.error("Failed to find or create user for GitHub user {}: {}", username, error.getMessage()));
    }
}
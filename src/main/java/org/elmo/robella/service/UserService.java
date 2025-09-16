package org.elmo.robella.service;

import org.elmo.robella.model.dto.AuthTokens;
import org.elmo.robella.model.entity.User;
import org.elmo.robella.model.common.Role;
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
        return Mono.fromCallable(() -> jwtUtil.validateToken(refreshToken))
            .filter(valid -> valid)
            .switchIfEmpty(Mono.error(new RuntimeException("无效的刷新令牌")))
            .flatMap(valid -> Mono.just(jwtUtil.extractUsername(refreshToken)))
            .flatMap(userRepository::findByUsername)
            .switchIfEmpty(Mono.error(new RuntimeException("用户不存在")))
            .flatMap(user -> {
                if (!user.getActive()) {
                    return Mono.error(new RuntimeException("用户已被禁用"));
                }
                String newAccessToken = jwtUtil.generateAccessToken(user);
                String newRefreshToken = jwtUtil.generateRefreshToken(user);
                return Mono.just(new LoginResponse(newAccessToken, newRefreshToken));
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
}
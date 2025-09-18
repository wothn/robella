package org.elmo.robella.service;

import org.elmo.robella.model.dto.AuthTokens;
import org.elmo.robella.model.entity.User;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.request.LoginRequest;
import org.elmo.robella.model.request.UserProfileUpdateRequest;
import org.elmo.robella.model.response.LoginResponse;
import org.elmo.robella.model.response.UserResponse;
import org.elmo.robella.repository.UserRepository;
import org.elmo.robella.util.JwtUtil;
import org.elmo.robella.exception.*;
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
                    return Mono.error(new ResourceConflictException("User", "username", user.getUsername()));
                }
                return userRepository.existsByEmail(user.getEmail());
            })
            .flatMap(existsByEmail -> {
                if (existsByEmail) {
                    return Mono.error(new ResourceConflictException("User", "email", user.getEmail()));
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
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", id)));
    }
    
    public Mono<UserResponse> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .map(this::convertToResponse)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", username)));
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
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", id)))
            .flatMap(existingUser -> {
                if (!existingUser.getUsername().equals(user.getUsername())) {
                    return userRepository.existsByUsername(user.getUsername())
                        .flatMap(exists -> exists ?
                            Mono.error(new ResourceConflictException("User", "username", user.getUsername())) :
                            Mono.just(existingUser));
                }
                return Mono.just(existingUser);
            })
            .flatMap(existingUser -> {
                if (!existingUser.getEmail().equals(user.getEmail())) {
                    return userRepository.existsByEmail(user.getEmail())
                        .flatMap(exists -> exists ?
                            Mono.error(new ResourceConflictException("User", "email", user.getEmail())) :
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
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", id)))
            .flatMap(user -> userRepository.deleteById(id))
            .doOnSuccess(v -> log.info("用户删除成功: {}", id))
            .doOnError(error -> log.error("用户删除失败: {}", error.getMessage()));
    }
    
    public Mono<UserResponse> activateUser(Long id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", id)))
            .flatMap(user -> {
                user.setActive(true);
                user.setUpdatedAt(LocalDateTime.now());
                return userRepository.save(user);
            })
            .map(this::convertToResponse);
    }
    
    public Mono<UserResponse> deactivateUser(Long id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", id)))
            .flatMap(user -> {
                user.setActive(false);
                user.setUpdatedAt(LocalDateTime.now());
                return userRepository.save(user);
            })
            .map(this::convertToResponse);
    }
    
      
    public Mono<AuthTokens> login(LoginRequest loginRequest) {
        return userRepository.findByUsername(loginRequest.getUsername())
            .switchIfEmpty(Mono.error(new InvalidCredentialsException()))
            .flatMap(user -> {
                if (!user.getActive()) {
                    return Mono.error(new UserDisabledException(user.getUsername()));
                }
                
                if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                    return Mono.error(new InvalidCredentialsException(user.getUsername()));
                }
                
                user.setLastLoginAt(LocalDateTime.now());
                String accessToken = jwtUtil.generateAccessToken(user);
                String refreshToken = jwtUtil.generateRefreshToken(user);
                
                return userRepository.save(user)
                    .then(Mono.just(createAuthTokens(accessToken, refreshToken)));
            });
    }
    
    public Mono<LoginResponse> refreshToken(String refreshToken) {
        return Mono.fromCallable(() -> jwtUtil.validateToken(refreshToken))
            .filter(valid -> valid)
            .switchIfEmpty(Mono.error(new RefreshTokenException(ErrorCode.REFRESH_TOKEN_INVALID)))
            .flatMap(valid -> Mono.just(jwtUtil.extractUsername(refreshToken)))
            .flatMap(userRepository::findByUsername)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User")))
            .flatMap(user -> {
                if (!user.getActive()) {
                    return Mono.error(new UserDisabledException(user.getUsername()));
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

    public Mono<UserResponse> updateUserProfile(String username, UserProfileUpdateRequest updateRequest) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", username)))
                .flatMap(existingUser -> {
                    if (!existingUser.getActive()) {
                        return Mono.error(new UserDisabledException(username));
                    }

                    return Mono.just(existingUser)
                            .flatMap(user -> {
                                if (updateRequest.getUsername() != null && !updateRequest.getUsername().equals(user.getUsername())) {
                                    return userRepository.existsByUsername(updateRequest.getUsername())
                                            .flatMap(exists -> exists ?
                                                Mono.error(new ResourceConflictException("User", "username", updateRequest.getUsername())) :
                                                Mono.just(user))
                                            .map(u -> {
                                                u.setUsername(updateRequest.getUsername());
                                                return u;
                                            });
                                }
                                return Mono.just(user);
                            })
                            .flatMap(user -> {
                                if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
                                    return userRepository.existsByEmail(updateRequest.getEmail())
                                            .flatMap(exists -> exists ?
                                                Mono.error(new ResourceConflictException("User", "email", updateRequest.getEmail())) :
                                                Mono.just(user))
                                            .map(u -> {
                                                u.setEmail(updateRequest.getEmail());
                                                return u;
                                            });
                                }
                                return Mono.just(user);
                            })
                            .map(user -> {
                                boolean needsUpdate = false;

                                if (updateRequest.getDisplayName() != null && !updateRequest.getDisplayName().equals(user.getDisplayName())) {
                                    user.setDisplayName(updateRequest.getDisplayName());
                                    needsUpdate = true;
                                }

                                if (updateRequest.getAvatar() != null && !updateRequest.getAvatar().equals(user.getAvatar())) {
                                    user.setAvatar(updateRequest.getAvatar());
                                    needsUpdate = true;
                                }

                                if (needsUpdate || updateRequest.getUsername() != null || updateRequest.getEmail() != null) {
                                    user.setUpdatedAt(LocalDateTime.now());
                                }

                                return user;
                            });
                })
                .flatMap(userRepository::save)
                .map(this::convertToResponse)
                .doOnSuccess(updatedUser -> log.info("用户资料更新成功: {}", updatedUser.getUsername()))
                .doOnError(error -> log.error("用户资料更新失败: {}", error.getMessage()));
    }
}
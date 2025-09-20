package org.elmo.robella.service;

import org.elmo.robella.model.dto.AuthTokens;
import org.elmo.robella.model.entity.User;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.request.LoginRequest;
import org.elmo.robella.model.request.UserProfileUpdateRequest;
import org.elmo.robella.model.request.UserCreateRequest;
import org.elmo.robella.model.request.UserUpdateRequest;
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

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public Mono<UserResponse> createUser(UserCreateRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setDisplayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername());
        user.setAvatar(request.getAvatar());

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
                user.setCreatedAt(OffsetDateTime.now());
                user.setUpdatedAt(OffsetDateTime.now());

                return userRepository.save(user);
            })
            .map(this::convertToResponse)
            .doOnSuccess(createdUser -> log.info("用户创建成功: {}", createdUser.getUsername()))
            .doOnError(error -> log.error("用户创建失败: {}", error.getMessage()));
    }

    public Flux<UserResponse> getUsers(Boolean active) {
        if (active != null) {
            return userRepository.findByActive(active)
                .map(this::convertToResponse);
        }
        return userRepository.findAll()
            .map(this::convertToResponse);
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

    public Mono<UserResponse> updateUser(Long id, UserUpdateRequest request) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", id)))
            .flatMap(existingUser -> {
                if (request.getUsername() != null && !existingUser.getUsername().equals(request.getUsername())) {
                    return userRepository.existsByUsername(request.getUsername())
                        .flatMap(exists -> exists ?
                            Mono.error(new ResourceConflictException("User", "username", request.getUsername())) :
                            Mono.just(existingUser));
                }
                return Mono.just(existingUser);
            })
            .flatMap(existingUser -> {
                if (request.getEmail() != null && !existingUser.getEmail().equals(request.getEmail())) {
                    return userRepository.existsByEmail(request.getEmail())
                        .flatMap(exists -> exists ?
                            Mono.error(new ResourceConflictException("User", "email", request.getEmail())) :
                            Mono.just(existingUser));
                }
                return Mono.just(existingUser);
            })
            .flatMap(existingUser -> {
                boolean needsUpdate = false;

                if (request.getUsername() != null && !request.getUsername().equals(existingUser.getUsername())) {
                    existingUser.setUsername(request.getUsername());
                    needsUpdate = true;
                }

                if (request.getEmail() != null && !request.getEmail().equals(existingUser.getEmail())) {
                    existingUser.setEmail(request.getEmail());
                    needsUpdate = true;
                }

                if (request.getDisplayName() != null && !request.getDisplayName().equals(existingUser.getDisplayName())) {
                    existingUser.setDisplayName(request.getDisplayName());
                    needsUpdate = true;
                }

                if (request.getAvatar() != null && !request.getAvatar().equals(existingUser.getAvatar())) {
                    existingUser.setAvatar(request.getAvatar());
                    needsUpdate = true;
                }

  
                if (request.getActive() != null && !request.getActive().equals(existingUser.getActive())) {
                    existingUser.setActive(request.getActive());
                    needsUpdate = true;
                }

                if (needsUpdate) {
                    existingUser.setUpdatedAt(OffsetDateTime.now());
                }

                return userRepository.save(existingUser);
            })
            .map(this::convertToResponse)
            .doOnSuccess(updatedUser -> log.info("用户更新成功: {}", updatedUser.getUsername()))
            .doOnError(error -> log.error("用户更新失败: {}", error.getMessage()));
    }

    public Mono<UserResponse> setUserActive(Long id, Boolean active) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", id)))
            .flatMap(user -> {
                if (!user.getActive().equals(active)) {
                    user.setActive(active);
                    user.setUpdatedAt(OffsetDateTime.now());
                    return userRepository.save(user);
                }
                return Mono.just(user);
            })
            .map(this::convertToResponse)
            .doOnSuccess(user -> log.info("用户状态更新成功: {} -> {}", user.getUsername(), active))
            .doOnError(error -> log.error("用户状态更新失败: {}", error.getMessage()));
    }

    public Mono<Void> deleteUser(Long id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", id)))
            .flatMap(user -> userRepository.deleteById(id))
            .doOnSuccess(v -> log.info("用户删除成功: {}", id))
            .doOnError(error -> log.error("用户删除失败: {}", error.getMessage()));
    }

    public Mono<Void> deleteUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", username)))
            .flatMap(user -> userRepository.delete(user))
            .doOnSuccess(v -> log.info("用户删除成功: {}", username))
            .doOnError(error -> log.error("用户删除失败: {}", error.getMessage()));
    }

    public Mono<Void> changePassword(String username, String currentPassword, String newPassword) {
        return userRepository.findByUsername(username)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", username)))
            .flatMap(user -> {
                if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                    return Mono.error(new InvalidCredentialsException("当前密码不正确"));
                }

                if (!user.getActive()) {
                    return Mono.error(new UserDisabledException(username));
                }

                user.setPassword(passwordEncoder.encode(newPassword));
                user.setUpdatedAt(OffsetDateTime.now());

                return userRepository.save(user);
            })
            .doOnSuccess(v -> log.info("密码修改成功: {}", username))
            .doOnError(error -> log.error("密码修改失败: {}", error.getMessage()))
            .then();
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

                user.setLastLoginAt(OffsetDateTime.now());
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
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setDisplayName(user.getDisplayName());
        response.setAvatar(user.getAvatar());
        response.setActive(user.getActive());

        // Convert Role enum to string
        if (user.getRole() != null) {
            response.setRole(user.getRole().getValue());
        } else {
            response.setRole(Role.USER.getValue());
        }

        // Set OffsetDateTime directly
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setLastLoginAt(user.getLastLoginAt());

        response.setGithubId(user.getGithubId());
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
                                    user.setUpdatedAt(OffsetDateTime.now());
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
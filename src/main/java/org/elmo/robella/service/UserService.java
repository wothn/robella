package org.elmo.robella.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elmo.robella.model.dto.AuthTokens;
import org.elmo.robella.model.entity.User;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.request.LoginRequest;
import org.elmo.robella.model.request.UserProfileUpdateRequest;
import org.elmo.robella.model.request.UserCreateRequest;
import org.elmo.robella.model.request.UserUpdateRequest;
import org.elmo.robella.model.response.LoginResponse;
import org.elmo.robella.model.response.UserResponse;
import org.elmo.robella.mapper.UserMapper;
import org.elmo.robella.util.JwtUtil;
import org.elmo.robella.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService extends ServiceImpl<UserMapper, User> {

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setDisplayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername());
        user.setAvatar(request.getAvatar());

        if (existsByUsername(user.getUsername())) {
            throw new ResourceConflictException("User", "username", user.getUsername());
        }

        if (existsByEmail(user.getEmail())) {
            throw new ResourceConflictException("User", "email", user.getEmail());
        }

        user.setActive(true);
        user.setRole(Role.USER);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());

        save(user);
        UserResponse response = convertToResponse(user);
        log.info("用户创建成功: {}", response.getUsername());
        return response;
    }

    public List<UserResponse> getUsers(Boolean active) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (active != null) {
            queryWrapper.eq(User::getActive, active);
        }
        List<User> users = list(queryWrapper);
        return users.stream().map(this::convertToResponse).toList();
    }

    public UserResponse getUserById(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", id);
        }
        return convertToResponse(user);
    }

    public UserResponse getUserByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = getOne(queryWrapper);
        if (user == null) {
            throw new ResourceNotFoundException("User", username);
        }
        return convertToResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        // 先验证用户是否存在
        if (getById(id) == null) {
            throw new ResourceNotFoundException("User", id);
        }

        // 检查用户名和邮箱冲突
        if (request.getUsername() != null && existsByUsername(request.getUsername())) {
            LambdaQueryWrapper<User> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(User::getUsername, request.getUsername()).ne(User::getId, id);
            if (count(checkWrapper) > 0) {
                throw new ResourceConflictException("User", "username", request.getUsername());
            }
        }

        if (request.getEmail() != null && existsByEmail(request.getEmail())) {
            LambdaQueryWrapper<User> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(User::getEmail, request.getEmail()).ne(User::getId, id);
            if (count(checkWrapper) > 0) {
                throw new ResourceConflictException("User", "email", request.getEmail());
            }
        }

        // 构建更新条件
        LambdaQueryWrapper<User> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(User::getId, id);

        // 构建更新内容
        User updateUser = new User();
        updateUser.setId(id); // 设置ID用于条件更新

        if (request.getUsername() != null) {
            updateUser.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            updateUser.setEmail(request.getEmail());
        }
        if (request.getDisplayName() != null) {
            updateUser.setDisplayName(request.getDisplayName());
        }
        if (request.getAvatar() != null) {
            updateUser.setAvatar(request.getAvatar());
        }
        if (request.getActive() != null) {
            updateUser.setActive(request.getActive());
        }

        updateUser.setUpdatedAt(OffsetDateTime.now());

        update(updateUser, updateWrapper);
        User updatedUser = getById(id);
        UserResponse response = convertToResponse(updatedUser);
        log.info("用户更新成功: {}", response.getUsername());
        return response;
    }

    @Transactional
    public UserResponse setUserActive(Long id, Boolean active) {
        if (getById(id) == null) {
            throw new ResourceNotFoundException("User", id);
        }

        LambdaQueryWrapper<User> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(User::getId, id);

        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setActive(active);
        updateUser.setUpdatedAt(OffsetDateTime.now());

        update(updateUser, updateWrapper);
        User updatedUser = getById(id);
        UserResponse response = convertToResponse(updatedUser);
        log.info("用户状态更新成功: {} -> {}", response.getUsername(), active);
        return response;
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", id);
        }
        removeById(id);
        log.info("用户删除成功: {}", id);
    }

    @Transactional
    public void deleteUserByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = getOne(queryWrapper);
        if (user == null) {
            throw new ResourceNotFoundException("User", username);
        }
        remove(queryWrapper);
        log.info("用户删除成功: {}", username);
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = getOne(queryWrapper);
        if (user == null) {
            throw new ResourceNotFoundException("User", username);
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidCredentialsException("当前密码不正确");
        }

        if (!user.getActive()) {
            throw new UserDisabledException(username);
        }

        LambdaQueryWrapper<User> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(User::getUsername, username);

        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setPassword(passwordEncoder.encode(newPassword));
        updateUser.setUpdatedAt(OffsetDateTime.now());

        update(updateUser, updateWrapper);
        log.info("密码修改成功: {}", username);
    }

    @Transactional
    public AuthTokens login(LoginRequest loginRequest) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginRequest.getUsername());
        User user = getOne(queryWrapper);
        if (user == null) {
            throw new InvalidCredentialsException();
        }

        if (!user.getActive()) {
            throw new UserDisabledException(user.getUsername());
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException(user.getUsername());
        }

        LambdaQueryWrapper<User> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(User::getUsername, loginRequest.getUsername());

        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setLastLoginAt(OffsetDateTime.now());

        update(updateUser, updateWrapper);

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        return createAuthTokens(accessToken, refreshToken);
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RefreshTokenException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String username = jwtUtil.extractUsername(refreshToken);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = getOne(queryWrapper);
        if (user == null) {
            throw new ResourceNotFoundException("User");
        }

        if (!user.getActive()) {
            throw new UserDisabledException(user.getUsername());
        }

        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        return new LoginResponse(newAccessToken);
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

    public User getUserByGithubId(String githubId) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getGithubId, githubId);
        return getOne(queryWrapper);
    }

    @Transactional
    public UserResponse updateUserProfile(String username, UserProfileUpdateRequest updateRequest) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User existingUser = getOne(queryWrapper);
        if (existingUser == null) {
            throw new ResourceNotFoundException("User", username);
        }

        if (!existingUser.getActive()) {
            throw new UserDisabledException(username);
        }

        // 检查用户名和邮箱冲突（排除当前用户）
        if (updateRequest.getUsername() != null && !updateRequest.getUsername().equals(existingUser.getUsername())) {
            LambdaQueryWrapper<User> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(User::getUsername, updateRequest.getUsername()).ne(User::getId, existingUser.getId());
            if (count(checkWrapper) > 0) {
                throw new ResourceConflictException("User", "username", updateRequest.getUsername());
            }
        }

        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(existingUser.getEmail())) {
            LambdaQueryWrapper<User> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(User::getEmail, updateRequest.getEmail()).ne(User::getId, existingUser.getId());
            if (count(checkWrapper) > 0) {
                throw new ResourceConflictException("User", "email", updateRequest.getEmail());
            }
        }

        // 构建更新条件
        LambdaQueryWrapper<User> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(User::getUsername, username);

        // 构建更新内容
        User updateUser = new User();
        updateUser.setId(existingUser.getId());

        if (updateRequest.getUsername() != null) {
            updateUser.setUsername(updateRequest.getUsername());
        }
        if (updateRequest.getEmail() != null) {
            updateUser.setEmail(updateRequest.getEmail());
        }
        if (updateRequest.getDisplayName() != null) {
            updateUser.setDisplayName(updateRequest.getDisplayName());
        }
        if (updateRequest.getAvatar() != null) {
            updateUser.setAvatar(updateRequest.getAvatar());
        }

        // 只要有任何更新就设置更新时间
        if (updateRequest.getUsername() != null || updateRequest.getEmail() != null ||
            updateRequest.getDisplayName() != null || updateRequest.getAvatar() != null) {
            updateUser.setUpdatedAt(OffsetDateTime.now());
        }

        update(updateUser, updateWrapper);
        User updatedUser = getById(existingUser.getId());
        UserResponse response = convertToResponse(updatedUser);
        log.info("用户资料更新成功: {}", response.getUsername());
        return response;
    }

    private boolean existsByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        return count(queryWrapper) > 0;
    }

    private boolean existsByEmail(String email) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, email);
        return count(queryWrapper) > 0;
    }
}
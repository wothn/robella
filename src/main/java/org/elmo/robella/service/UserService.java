package org.elmo.robella.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elmo.robella.model.entity.User;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.request.LoginRequest;
import org.elmo.robella.model.request.UserProfileUpdateRequest;
import org.elmo.robella.model.request.UserCreateRequest;
import org.elmo.robella.model.request.UserUpdateRequest;
import org.elmo.robella.model.response.UserResponse;
import org.elmo.robella.mapper.UserMapper;
import org.elmo.robella.exception.InsufficientCreditsException;
import cn.dev33.satoken.stp.StpUtil;
import org.elmo.robella.common.ErrorCodeConstants;
import org.elmo.robella.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService extends ServiceImpl<UserMapper, User> {

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
            throw new BusinessException(ErrorCodeConstants.RESOURCE_CONFLICT, "Username already exists!");
        }

        if (existsByEmail(user.getEmail())) {
            throw new BusinessException(ErrorCodeConstants.RESOURCE_CONFLICT, "Email already exists!");
        }

        user.setActive(true);
        user.setRole(Role.USER);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());

        try {
            save(user);
        } catch (Exception e) {
            // 处理并发插入导致的唯一约束冲突
            if (existsByUsername(user.getUsername())) {
                throw new BusinessException(ErrorCodeConstants.RESOURCE_CONFLICT, "Username already exists!");
            }
            if (existsByEmail(user.getEmail())) {
                throw new BusinessException(ErrorCodeConstants.RESOURCE_CONFLICT, "Email already exists!");
            }
            // 如果不是唯一约束冲突，重新抛出异常
            throw e;
        }
        
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
            throw new ResourceNotFoundException(ErrorCodeConstants.USER_NOT_FOUND, "User not found");
        }
        return convertToResponse(user);
    }


    public UserResponse getUserByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCodeConstants.RESOURCE_NOT_FOUND, "User not found: " + username);
        }
        return convertToResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        // 先验证用户是否存在
        if (getById(id) == null) {
            throw new BusinessException(ErrorCodeConstants.RESOURCE_NOT_FOUND, "User not found: " + id);
        }

        // 构建更新条件
        LambdaQueryWrapper<User> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(User::getId, id);

        // 构建更新内容
        User updateUser = new User();
        updateUser.setId(id); // 设置ID用于条件更新
        
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
            throw new BusinessException(ErrorCodeConstants.RESOURCE_NOT_FOUND, "User not found: " + id);
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
    public void updateUserCredits(Long userId, BigDecimal credits) {
        LambdaQueryWrapper<User> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(User::getId, userId);

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setCredits(credits);
        updateUser.setUpdatedAt(OffsetDateTime.now());

        update(updateUser, updateWrapper);
        log.info("用户credits更新成功: userId={}, credits={}", userId, credits);
    }

    @Transactional
    public void deductUserCredits(Long userId, BigDecimal amount) throws InsufficientCreditsException {
        // 获取当前用户
        User user = getById(userId);
        if (user == null) {
            throw new ResourceNotFoundException(ErrorCodeConstants.USER_NOT_FOUND, "User not found with id: " + userId);
        }

        // 扣减credits（允许为负值）
        BigDecimal currentCredits = user.getCredits() != null ? user.getCredits() : BigDecimal.ZERO;
        BigDecimal newCredits = currentCredits.subtract(amount);
        updateUserCredits(userId, newCredits);
        log.info("用户credits扣减成功: userId={}, amount={}, newCredits={}", userId, amount, newCredits);
        
        // 如果扣减后余额为负，抛出异常（但仍然扣减）
        if (newCredits.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientCreditsException("Insufficient credits for user: " + userId + ". Required: " + amount + ", Available: " + currentCredits);
        }
    }

    @Transactional
    public void refundUserCredits(Long userId, BigDecimal amount) {
        // 获取当前用户
        User user = getById(userId);
        if (user == null) {
            throw new ResourceNotFoundException(ErrorCodeConstants.USER_NOT_FOUND, "User not found with id: " + userId);
        }

        // 退还credits
        BigDecimal currentCredits = user.getCredits() != null ? user.getCredits() : BigDecimal.ZERO;
        BigDecimal newCredits = currentCredits.add(amount);
        updateUserCredits(userId, newCredits);
        log.info("用户credits退还成功: userId={}, refundAmount={}, newCredits={}", userId, amount, newCredits);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodeConstants.RESOURCE_NOT_FOUND, "User not found: " + id);
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
            throw new BusinessException(ErrorCodeConstants.RESOURCE_NOT_FOUND, "User not found: " + username);
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
            throw new BusinessException(ErrorCodeConstants.RESOURCE_NOT_FOUND, "User not found: " + username);
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "当前密码不正确");
        }

        if (!user.getActive()) {
            throw new BusinessException(ErrorCodeConstants.ACCOUNT_DISABLED, "User account is disabled: " + username);
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
    public void login(LoginRequest loginRequest) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginRequest.getUsername());
        User user = getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "Invalid username or password");
        }

        if (!user.getActive()) {
            throw new BusinessException(ErrorCodeConstants.ACCOUNT_DISABLED, "User account is disabled: " + user.getUsername());
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "Invalid username or password");
        }

        LambdaQueryWrapper<User> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(User::getUsername, loginRequest.getUsername());

        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setLastLoginAt(OffsetDateTime.now());

        update(updateUser, updateWrapper);

        // 使用 Sa-Token 进行登录
        StpUtil.login(user.getId());

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
    public UserResponse updateUserProfile(Long userId, UserProfileUpdateRequest updateRequest) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, userId);
        User existingUser = getOne(queryWrapper);
        if (existingUser == null) {
            throw new BusinessException(ErrorCodeConstants.RESOURCE_NOT_FOUND, "User not found");
        }

        if (!existingUser.getActive()) {
            throw new BusinessException(ErrorCodeConstants.ACCOUNT_DISABLED, "User account is disabled: " + existingUser.getUsername());
        }

        // 构建更新条件
        LambdaQueryWrapper<User> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(User::getId, userId);

        // 构建更新内容
        User updateUser = new User();
        updateUser.setId(existingUser.getId());
        
        if (updateRequest.getDisplayName() != null) {
            updateUser.setDisplayName(updateRequest.getDisplayName());
        }
        if (updateRequest.getAvatar() != null) {
            updateUser.setAvatar(updateRequest.getAvatar());
        }

        // 只要有任何更新就设置更新时间
        if (updateRequest.getDisplayName() != null || updateRequest.getAvatar() != null) {
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
package org.elmo.robella.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elmo.robella.model.entity.User;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.request.*;
import org.elmo.robella.model.response.UserResponse;
import org.elmo.robella.mapper.UserMapper;
import org.elmo.robella.exception.InsufficientCreditsException;
import cn.dev33.satoken.stp.StpUtil;
import org.elmo.robella.common.ErrorCodeConstants;
import org.elmo.robella.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCredits(BigDecimal.ZERO);
        user.setActive(true);
        user.setRole(Role.USER);
        if (existsByUsername(user.getUsername())) {
            throw new BusinessException(ErrorCodeConstants.RESOURCE_CONFLICT, "Username already exists!");
        }
        if (existsByEmail(user.getEmail())) {
            throw new BusinessException(ErrorCodeConstants.RESOURCE_CONFLICT, "Email already exists!");
        }
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


    @Transactional
    public boolean updateUser(Long id, UserUpdateRequest request) {
        User user = new User();

        user.setId(id);
        BeanUtils.copyProperties(request, user);

        // 使用 updateById 而不是 update(entity, wrapper) 以确保 TypeHandler 生效
        updateById(user);

        log.info("用户更新成功: {}", user.getUsername());
        return true;
    }

    @Transactional
    public boolean setUserActive(Long id, Boolean active) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodeConstants.RESOURCE_NOT_FOUND, "User not found: " + id);
        }

        user.setId(id);
        user.setActive(active);
        boolean result = updateById(user);
        
        if (result) {
            log.info("用户状态更新成功: {} -> {}", user.getUsername(), active);
        }
        return result;
    }

    @Transactional
    public boolean unlinkGitHubAccount(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeConstants.RESOURCE_NOT_FOUND, "User not found: " + userId);
        }

        if (user.getGithubId() == null) {
            return true;
        }

        user.setGithubId(null);
        user.setUpdatedAt(OffsetDateTime.now());
        boolean result = updateById(user);
        if (result) {
            log.info("GitHub account unlinked for user {}", userId);
        }
        return result;
    }

    @Transactional
    public void updateUserCredits(Long userId, BigDecimal credits) {
        User user = getById(userId);
        if (user == null) {
            throw new ResourceNotFoundException(ErrorCodeConstants.USER_NOT_FOUND, "User not found with id: " + userId);
        }

        user.setCredits(credits);
        user.setUpdatedAt(OffsetDateTime.now());

        // 使用 updateById 而不是 update(entity, wrapper) 以确保 TypeHandler 生效
        updateById(user);
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
    public boolean deleteUser(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodeConstants.RESOURCE_NOT_FOUND, "User not found: " + id);
        }
        boolean result = removeById(id);
        if (result) {
            log.info("用户删除成功: {}", id);
        }
        return result;
    }


    @Transactional
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, userId);
        User user = getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCodeConstants.RESOURCE_NOT_FOUND, "User not found: " + userId);
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "当前密码不正确");
        }

        if (!user.getActive()) {
            throw new BusinessException(ErrorCodeConstants.ACCOUNT_DISABLED, "User account is disabled: " + userId);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(OffsetDateTime.now());

        // 使用 updateById 而不是 update(entity, wrapper) 以确保 TypeHandler 生效
        boolean result = updateById(user);
        if (result) {
            log.info("密码修改成功: {}", userId);
        }
        return result;
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

        user.setLastLoginAt(OffsetDateTime.now());

        // 使用 updateById 而不是 update(entity, wrapper) 以确保 TypeHandler 生效
        updateById(user);

        // 使用 Sa-Token 进行登录
        StpUtil.login(user.getId());

    }

    public UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        // Copy all common properties using BeanUtils
        BeanUtils.copyProperties(user, response);
        
        // Convert Role enum to string
        if (user.getRole() != null) {
            response.setRole(user.getRole().getValue());
        } else {
            response.setRole(Role.USER.getValue());
        }
        
        return response;
    }

    @Transactional
    public boolean updateUserProfile(Long userId, UserProfileUpdateRequest updateRequest) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, userId);
        User existingUser = getOne(queryWrapper);
        if (existingUser == null) {
            throw new BusinessException(ErrorCodeConstants.RESOURCE_NOT_FOUND, "User not found");
        }

        if (!existingUser.getActive()) {
            throw new BusinessException(ErrorCodeConstants.ACCOUNT_DISABLED, "User account is disabled: " + existingUser.getUsername());
        }

        boolean hasUpdate = false;
        
        if (updateRequest.getDisplayName() != null) {
            existingUser.setDisplayName(updateRequest.getDisplayName());
            hasUpdate = true;
        }
        if (updateRequest.getAvatar() != null) {
            existingUser.setAvatar(updateRequest.getAvatar());
            hasUpdate = true;
        }

        // 只要有任何更新就设置更新时间并保存
        if (hasUpdate) {
            existingUser.setUpdatedAt(OffsetDateTime.now());
            // 使用 updateById 而不是 update(entity, wrapper) 以确保 TypeHandler 生效
            boolean result = updateById(existingUser);
            if (result) {
                log.info("用户资料更新成功: {}", existingUser.getUsername());
            }
            return result;
        }
        
        // 如果没有实际更新，返回true
        return true;
    }

    private boolean existsByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        return count(queryWrapper) > 0;
    }

    @Transactional
    public void register(RegisterRequest registerRequest) {
        // Check if passwords match
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new BusinessException(ErrorCodeConstants.INVALID_CREDENTIALS, "Passwords do not match");
        }
        
        // Create user entity
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setDisplayName(registerRequest.getDisplayName());
        user.setCredits(BigDecimal.ZERO);
        user.setActive(true);
        user.setRole(Role.USER);
        
        // Check if username already exists
        if (existsByUsername(user.getUsername())) {
            throw new BusinessException(ErrorCodeConstants.RESOURCE_CONFLICT, "Username already exists!");
        }
        
        // Save the user to database
        try {
            save(user);
        } catch (Exception e) {
            // Handle concurrent insertion due to unique constraint conflicts
            if (existsByUsername(user.getUsername())) {
                throw new BusinessException(ErrorCodeConstants.RESOURCE_CONFLICT, "Username already exists!");
            }
            // If it's not a unique constraint conflict, re-throw the exception
            throw e;
        }
        
        log.info("用户注册成功: {}", user.getUsername());
    }

    private boolean existsByEmail(String email) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, email);
        return count(queryWrapper) > 0;
    }
}
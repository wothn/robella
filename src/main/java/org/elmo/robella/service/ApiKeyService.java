package org.elmo.robella.service;

import org.elmo.robella.model.entity.ApiKey;
import org.elmo.robella.mapper.ApiKeyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService extends ServiceImpl<ApiKeyMapper, ApiKey> {

    private final PasswordEncoder passwordEncoder;

    private static final String API_KEY_PREFIX = "rk-";
    private static final int API_KEY_LENGTH = 32;

    public ApiKey createApiKey(Long userId, String name, String description) {
        return createApiKey(userId, name, description, null, null, 60);
    }

    public ApiKey createApiKey(Long userId, String name, String description,
                                     Integer dailyLimit, Integer monthlyLimit, Integer rateLimit) {
        String apiKey = generateApiKey();
        String keyHash = passwordEncoder.encode(apiKey);
        String keyPrefix = apiKey.substring(0, 16);

        ApiKey newApiKey = ApiKey.builder()
                .userId(userId)
                .keyHash(keyHash)
                .keyPrefix(keyPrefix)
                .name(name)
                .description(description)
                .dailyLimit(dailyLimit)
                .monthlyLimit(monthlyLimit)
                .rateLimit(rateLimit)
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        boolean success = save(newApiKey);
        if (success) {
            // 返回完整的API密钥（只在创建时显示一次）
            newApiKey.setKeyHash(apiKey); // 临时存储完整密钥用于返回
            return newApiKey;
        }
        throw new RuntimeException("Failed to create API key");
    }

    public List<ApiKey> getUserApiKeys(Long userId) {
        LambdaQueryWrapper<ApiKey> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiKey::getUserId, userId)
                   .orderByDesc(ApiKey::getCreatedAt);

        List<ApiKey> keys = list(queryWrapper);
        return keys.stream()
                .peek(key -> {
                    // 隐藏密钥的哈希值
                    key.setKeyHash("********");
                })
                .collect(Collectors.toList());
    }

    public boolean deleteApiKey(Long id, Long userId) {
        LambdaQueryWrapper<ApiKey> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiKey::getId, id)
                   .eq(ApiKey::getUserId, userId);

        return remove(queryWrapper);
    }

    public ApiKey toggleApiKeyStatus(Long id, Long userId) {
        LambdaQueryWrapper<ApiKey> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiKey::getId, id)
                   .eq(ApiKey::getUserId, userId);

        ApiKey key = getOne(queryWrapper);
        if (key == null) {
            throw new RuntimeException("API key not found or unauthorized");
        }

        key.setActive(!key.getActive());
        key.setUpdatedAt(OffsetDateTime.now());
        boolean success = updateById(key);
        if (success) {
            return key;
        }
        throw new RuntimeException("Failed to toggle API key status");
    }

    public ApiKey validateApiKey(String apiKey) {
        if (apiKey == null || !apiKey.startsWith(API_KEY_PREFIX)) {
            return null;
        }

        String keyPrefix = apiKey.substring(0, 16);
        LambdaQueryWrapper<ApiKey> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiKey::getKeyPrefix, keyPrefix);

        ApiKey key = getOne(queryWrapper);
        if (key != null && key.getActive() &&
                (key.getExpiresAt() == null || key.getExpiresAt().isAfter(OffsetDateTime.now()))) {
            if (passwordEncoder.matches(apiKey, key.getKeyHash())) {
                key.setLastUsedAt(OffsetDateTime.now());
                updateById(key);
                return key;
            }
        }
        return null;
    }

    public boolean checkRateLimit(Long userId, String keyPrefix) {
        return true;
    }

    public boolean checkDailyLimit(Long userId, String keyPrefix) {
        return true;
    }

    public boolean checkMonthlyLimit(Long userId, String keyPrefix) {
        return true;
    }

    private String generateApiKey() {
        StringBuilder sb = new StringBuilder(API_KEY_PREFIX);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        for (int i = 0; i < API_KEY_LENGTH; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }

        return sb.toString();
    }

    public boolean isApiKeyOwner(Long apiKeyId, Long userId) {
        LambdaQueryWrapper<ApiKey> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiKey::getId, apiKeyId)
                   .eq(ApiKey::getUserId, userId);

        return exists(queryWrapper);
    }
}
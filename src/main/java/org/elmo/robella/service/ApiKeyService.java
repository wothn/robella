package org.elmo.robella.service;

import org.elmo.robella.model.entity.ApiKey;
import org.elmo.robella.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String API_KEY_PREFIX = "rk-";
    private static final int API_KEY_LENGTH = 32;

    public Mono<ApiKey> createApiKey(Long userId, String name, String description) {
        return createApiKey(userId, name, description, null, null, 60);
    }

    public Mono<ApiKey> createApiKey(Long userId, String name, String description,
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
                .build();

        return apiKeyRepository.save(newApiKey)
                .map(savedKey -> {
                    // 返回完整的API密钥（只在创建时显示一次）
                    savedKey.setKeyHash(apiKey); // 临时存储完整密钥用于返回
                    return savedKey;
                });
    }

    public Flux<ApiKey> getUserApiKeys(Long userId) {
        return apiKeyRepository.findByUserId(userId)
                .map(key -> {
                    // 隐藏密钥的哈希值
                    key.setKeyHash("********");
                    return key;
                });
    }

    public Mono<Void> deleteApiKey(Long id, Long userId) {
        return apiKeyRepository.deleteByIdAndUserId(id, userId);
    }

    public Mono<ApiKey> toggleApiKeyStatus(Long id, Long userId) {
        return apiKeyRepository.findById(id)
                .filter(key -> key.getUserId().equals(userId))
                .flatMap(key -> {
                    key.setActive(!key.getActive());
                    return apiKeyRepository.save(key);
                });
    }

    public Mono<ApiKey> validateApiKey(String apiKey) {
        if (apiKey == null || !apiKey.startsWith(API_KEY_PREFIX)) {
            return Mono.empty();
        }

        String keyPrefix = apiKey.substring(0, 16);

        return apiKeyRepository.findByKeyPrefix(keyPrefix)
                .filter(key -> key.getActive() &&
                        (key.getExpiresAt() == null || key.getExpiresAt().isAfter(LocalDateTime.now())))
                .filterWhen(key -> Mono.fromCallable(() ->
                        passwordEncoder.matches(apiKey, key.getKeyHash())))
                .flatMap(key -> {
                    key.setLastUsedAt(LocalDateTime.now());
                    return apiKeyRepository.save(key);
                });
    }

    public Mono<Boolean> checkRateLimit(Long userId, String keyPrefix) {
        return Mono.just(true);
    }

    public Mono<Boolean> checkDailyLimit(Long userId, String keyPrefix) {
        return Mono.just(true);
    }

    public Mono<Boolean> checkMonthlyLimit(Long userId, String keyPrefix) {
        return Mono.just(true);
    }

    private String generateApiKey() {
        StringBuilder sb = new StringBuilder(API_KEY_PREFIX);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        for (int i = 0; i < API_KEY_LENGTH; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }

        return sb.toString();
    }

    public Mono<Boolean> isApiKeyOwner(Long apiKeyId, Long userId) {
        return apiKeyRepository.findById(apiKeyId)
                .map(key -> key.getUserId().equals(userId))
                .defaultIfEmpty(false);
    }
}
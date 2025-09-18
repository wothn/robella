package org.elmo.robella.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.annotation.RequiredRole;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.dto.ApiKeyCreateRequest;
import org.elmo.robella.model.entity.ApiKey;
import org.elmo.robella.model.response.ApiKeyResponse;
import org.elmo.robella.service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

        private final ApiKeyService apiKeyService;

        @PostMapping
        @RequiredRole(Role.USER)
        public Mono<ResponseEntity<ApiKeyResponse>> createApiKey(
                        @Valid @RequestBody ApiKeyCreateRequest request) {

                return Mono.deferContextual(contextView -> {
                        Long userId = contextView.get("userId");
                        return apiKeyService.createApiKey(
                                        userId,
                                        request.getName(),
                                        request.getDescription(),
                                        request.getDailyLimit(),
                                        request.getMonthlyLimit(),
                                        request.getRateLimit())
                                        .map(apiKey -> ResponseEntity.ok(toApiKeyResponse(apiKey, true))); // 包含完整的API密钥
                });
        }

        @GetMapping
        @RequiredRole(Role.USER)
        public Flux<ApiKeyResponse> getUserApiKeys() {

                return Mono.deferContextual(contextView -> {
                        Long userId = contextView.get("userId");
                        return Mono.just(userId);
                })
                                .flatMapMany(apiKeyService::getUserApiKeys)
                                .map(apiKey -> toApiKeyResponse(apiKey, false)); // 不包含完整的API密钥
        }

        @DeleteMapping("/{id}")
        @RequiredRole(Role.USER)
        public Mono<ResponseEntity<Void>> deleteApiKey(@PathVariable Long id) {
                return Mono.deferContextual(contextView -> {
                        Long userId = contextView.get("userId");
                        return apiKeyService.isApiKeyOwner(id, userId)
                                .map(isOwner -> new Object[]{userId, isOwner});
                })
                .flatMap(data -> {
                        Long userId = (Long) data[0];
                        Boolean isOwner = (Boolean) data[1];
                        
                        if (isOwner) {
                                return apiKeyService.deleteApiKey(id, userId)
                                        .then(Mono.just(ResponseEntity.noContent().<Void>build()));
                        }
                        return Mono.just(ResponseEntity.notFound().build());
                });
        }

        @PatchMapping("/{id}/toggle")
        @RequiredRole(Role.USER)
        public Mono<ResponseEntity<ApiKeyResponse>> toggleApiKeyStatus(
                        @PathVariable Long id) {

                return Mono.deferContextual(contextView -> {
                        Long userId = contextView.get("userId");
                        return apiKeyService.isApiKeyOwner(id, userId)
                                .map(isOwner -> new Object[]{userId, isOwner});
                })
                .flatMap(data -> {
                        Long userId = (Long) data[0];
                        Boolean isOwner = (Boolean) data[1];
                        
                        if (isOwner) {
                                return apiKeyService.toggleApiKeyStatus(id, userId)
                                        .map(apiKey -> ResponseEntity.ok(toApiKeyResponse(apiKey, false))); // 不包含完整的API密钥
                        }
                        return Mono.just(ResponseEntity.notFound().build());
                });
        }

        // =========================私有工具方法=========================//
    /**
     * 构建ApiKeyResponse对象，支持控制是否包含完整的API密钥
     * @param apiKey 要转换的ApiKey实体对象
     * @param includeFullKey 是否包含完整的API密钥
     * @return 转换后的ApiKeyResponse对象
     */
    private ApiKeyResponse toApiKeyResponse(ApiKey apiKey, boolean includeFullKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .description(apiKey.getDescription())
                .keyPrefix(apiKey.getKeyPrefix())
                .apiKey(includeFullKey ? apiKey.getKeyHash() : null)
                .dailyLimit(apiKey.getDailyLimit())
                .monthlyLimit(apiKey.getMonthlyLimit())
                .rateLimit(apiKey.getRateLimit())
                .active(apiKey.getActive())
                .lastUsedAt(apiKey.getLastUsedAt())
                .expiresAt(apiKey.getExpiresAt())
                .createdAt(apiKey.getCreatedAt())
                .updatedAt(apiKey.getUpdatedAt())
                .build();
    }
}
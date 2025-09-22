package org.elmo.robella.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.annotation.RequiredRole;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.dto.ApiKeyCreateRequest;
import org.elmo.robella.model.entity.ApiKey;
import org.elmo.robella.model.response.ApiKeyResponse;
import org.elmo.robella.service.ApiKeyService;
import org.elmo.robella.context.RequestContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

        private final ApiKeyService apiKeyService;

        @PostMapping
        @RequiredRole(Role.USER)
        public ResponseEntity<ApiKeyResponse> createApiKey(
                        HttpServletRequest httpRequest,
                        @Valid @RequestBody ApiKeyCreateRequest request) {

                Long userId = getCurrentUserId();
                ApiKey apiKey = apiKeyService.createApiKey(
                                userId,
                                request.getName(),
                                request.getDescription(),
                                request.getDailyLimit(),
                                request.getMonthlyLimit(),
                                request.getRateLimit());
                return ResponseEntity.ok(toApiKeyResponse(apiKey, true)); // 包含完整的API密钥
        }

        @GetMapping
        @RequiredRole(Role.USER)
        public List<ApiKeyResponse> getUserApiKeys(HttpServletRequest httpRequest) {
                Long userId = getCurrentUserId();
                return apiKeyService.getUserApiKeys(userId).stream()
                                .map(apiKey -> toApiKeyResponse(apiKey, false)) // 不包含完整的API密钥
                                .toList();
        }

        @DeleteMapping("/{id}")
        @RequiredRole(Role.USER)
        public ResponseEntity<Void> deleteApiKey(HttpServletRequest httpRequest, @PathVariable Long id) {
                Long userId = getCurrentUserId();
                boolean isOwner = apiKeyService.isApiKeyOwner(id, userId);

                if (isOwner) {
                        apiKeyService.deleteApiKey(id, userId);
                        return ResponseEntity.noContent().build();
                }
                return ResponseEntity.notFound().build();
        }

        @PatchMapping("/{id}/toggle")
        @RequiredRole(Role.USER)
        public ResponseEntity<ApiKeyResponse> toggleApiKeyStatus(
                        HttpServletRequest httpRequest,
                        @PathVariable Long id) {

                Long userId = getCurrentUserId();
                boolean isOwner = apiKeyService.isApiKeyOwner(id, userId);

                if (isOwner) {
                        ApiKey apiKey = apiKeyService.toggleApiKeyStatus(id, userId);
                        return ResponseEntity.ok(toApiKeyResponse(apiKey, false)); // 不包含完整的API密钥
                }
                return ResponseEntity.notFound().build();
        }

        // =========================私有工具方法=========================//
        /**
         * 获取当前登录用户的ID
         * @return 当前用户ID
         */
        private Long getCurrentUserId() {
                return RequestContextHolder.getContext() != null ?
                    RequestContextHolder.getContext().getUserId() : null;
        }

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
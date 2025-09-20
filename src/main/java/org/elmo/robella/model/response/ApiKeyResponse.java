package org.elmo.robella.model.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime; 

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponse {

    private Long id;
    private String name;
    private String description;
    private String keyPrefix;
    private String apiKey; // 只在创建时显示
    private Integer dailyLimit;
    private Integer monthlyLimit;
    private Integer rateLimit;
    private Boolean active;
    private OffsetDateTime lastUsedAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
package org.elmo.robella.model.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("api_key")
public class ApiKey {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("key_hash")
    private String keyHash;

    @Column("key_prefix")
    private String keyPrefix;

    private String name;

    private String description;

    @Column("daily_limit")
    private Integer dailyLimit;

    @Column("monthly_limit")
    private Integer monthlyLimit;

    @Column("rate_limit")
    private Integer rateLimit;

    private Boolean active;

    @Column("last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column("expires_at")
    private OffsetDateTime expiresAt;


    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
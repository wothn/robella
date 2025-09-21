package org.elmo.robella.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("api_key")
public class ApiKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("key_hash")
    private String keyHash;

    @TableField("key_prefix")
    private String keyPrefix;

    private String name;

    private String description;

    @TableField("daily_limit")
    private Integer dailyLimit;

    @TableField("monthly_limit")
    private Integer monthlyLimit;

    @TableField("rate_limit")
    private Integer rateLimit;

    private Boolean active;

    @TableField("last_used_at")
    private OffsetDateTime lastUsedAt;

    @TableField("expires_at")
    private OffsetDateTime expiresAt;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
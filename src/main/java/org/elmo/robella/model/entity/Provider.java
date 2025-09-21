package org.elmo.robella.model.entity;

import org.elmo.robella.common.EndpointType;
import org.elmo.robella.common.ProviderType;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("provider")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Provider {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "Provider name cannot be blank")
    private String name;

    @NotNull(message = "Endpoint type cannot be null")
    private EndpointType endpointType;

    @NotNull(message = "Provider type cannot be null")
    private ProviderType providerType;

    @NotBlank(message = "Base URL cannot be blank")
    private String baseUrl;

    @NotBlank(message = "API key cannot be blank")
    private String apiKey;

    private String config;

    @NotNull(message = "Enabled status cannot be null")
    private Boolean enabled;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.UPDATE)
    private OffsetDateTime updatedAt;
}
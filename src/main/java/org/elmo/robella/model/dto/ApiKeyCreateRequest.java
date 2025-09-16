package org.elmo.robella.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApiKeyCreateRequest {

    @NotBlank(message = "API密钥名称不能为空")
    @Size(max = 100, message = "API密钥名称不能超过100个字符")
    private String name;

    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;

    private Integer dailyLimit;

    private Integer monthlyLimit;

    private Integer rateLimit;
}
package org.elmo.robella.model.entity;

import java.time.LocalDateTime;

import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("logs")
public class Log {
    private Long id;
    private Long userId;
    private Long providerId;
    private String providerName;
    private String modelKey;
    private String vendorModelKey;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private LocalDateTime createdAt;
    private Integer quota;
    private Integer useTime;
    private Boolean streaming;
    private String endpointType;
    private String tokenId;
    private String tokenPrefix;
}

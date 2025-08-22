package org.elmo.robella.model.internal;

import lombok.Builder;
import lombok.Data;

@Data
public class UnifiedError {
    private String code;        // 统一错误码
    private String message;     // 描述
    private String provider;    // 相关 provider
    private String raw;         // 原始错误信息
}

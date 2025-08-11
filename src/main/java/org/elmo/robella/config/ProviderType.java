package org.elmo.robella.config;

import lombok.Getter;

/**
 * 支持的厂商类型枚举。用于 AdapterFactory、Transform 分发等。
 */
@Getter
public enum ProviderType {
    OpenAI("OpenAI"),
    AzureOpenAI("AzureOpenAI"), // Azure OpenAI 兼容模式
    Anthropic("Anthropic");

    private final String name;

    ProviderType(String name) {
        this.name = name;
    }

    public static ProviderType fromString(String v) {
        if (v == null || v.isEmpty()) return OpenAI;
        for (ProviderType t : values()) {
            if (t.name().equalsIgnoreCase(v)) return t;
        }
        // 兼容大小写不一致或未知，默认 OpenAI
        return OpenAI;
    }
}

package org.elmo.robella.model.common;

import lombok.Getter;

/**
 * 支持的厂商类型枚举。用于 AdapterFactory、Transform 分发等。
 */
@Getter
public enum EndpointType {
    OpenAI("OpenAI"),
    Anthropic("Anthropic");

    private final String name;

    EndpointType(String name) {
        this.name = name;
    }
}

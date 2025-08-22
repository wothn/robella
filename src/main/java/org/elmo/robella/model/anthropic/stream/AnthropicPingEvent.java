package org.elmo.robella.model.anthropic.stream;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic Ping 事件
 * 用于保持连接活跃的心跳事件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicPingEvent extends AnthropicStreamEvent {
    // ping 事件仅包含 type 字段，无其他数据
}

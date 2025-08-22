package org.elmo.robella.model.anthropic.stream;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 消息停止事件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicMessageStopEvent extends AnthropicStreamEvent {
    // 停止事件没有额外的参数
}

package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单条对话消息。role 可为 "user" | "assistant" | "system" (system 会被放在 request.system 里, 这里通常只出现 user/assistant)。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnthropicMessage {

    private String role; // user / assistant

    private List<AnthropicContentBlock> content; // 多模态/文本片段
}

package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Messages API 的响应对象。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {
    private String id;
    private String type; // message
    private String role; // assistant
    private List<MessageResponseContentBlock> content; // 生成内容
    private String model;
    private MessageUsage usage;

    @JsonProperty("stop_reason")
    private String stopReason;

    @JsonProperty("stop_sequence")
    private String stopSequence;

    private Map<String, Object> metadata;
}

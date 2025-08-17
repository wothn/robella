package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Anthropic 增量数据
 */
@Data
public class AnthropicDelta {
    
    /**
     * 文本增量
     */
    private String text;
    
    /**
     * 停止原因
     */
    @JsonProperty("stop_reason")
    private String stopReason;
    
    /**
     * 使用量统计
     */
    private AnthropicUsage usage;
    
    /**
     * 工具输入的增量数据
     */
    @JsonProperty("partial_json")
    private String partialJson;
    
    /**
     * 其他动态字段
     */
    private Map<String, Object> additionalProperties;
}

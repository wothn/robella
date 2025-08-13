package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OpenAI 流式输出选项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamOptions {
    
    /**
     * 是否包含使用统计信息
     */
    @JsonProperty("include_usage")
    private Boolean includeUsage;

    /**
     * 是否包含混淆信息
     */
    @JsonProperty("include_obfuscation")
    private Boolean include_obfuscation;
}

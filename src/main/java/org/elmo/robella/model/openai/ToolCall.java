package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI Tool调用
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolCall {
    
    /**
     * Tool调用的ID
     */
    private String id;
    
    /**
     * Tool的类型，目前仅支持function
     */
    private String type;
    
    /**
     * Function调用信息
     */
    private FunctionCall function;
}

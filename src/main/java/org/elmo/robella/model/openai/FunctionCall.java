package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OpenAI Function调用
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionToolCall {
    
    /**
     * 要调用的function名称
     */
    private String name;
    
    /**
     * 要调用的function的参数，JSON格式
     */
    private String arguments;
}

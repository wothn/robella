package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OpenAI Function定义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Function {
    
    /**
     * Function名称
     */
    private String name;
    
    /**
     * Function描述
     */
    private String description;
    
    /**
     * Function参数，JSON Schema格式
     */
    private Object parameters;
}

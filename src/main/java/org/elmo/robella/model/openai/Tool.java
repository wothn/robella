package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OpenAI Tool定义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tool {
    
    /**
     * Tool的类型，目前仅支持function
     */
    private String type;
    
    /**
     * Function定义
     */
    private Function function;
}

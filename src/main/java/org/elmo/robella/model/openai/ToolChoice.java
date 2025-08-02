package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI Tool选择控制
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolChoice {
    
    /**
     * 选择类型：none, auto, required，或特定tool
     */
    private String type;
    
    /**
     * 特定function选择
     */
    private Function function;
    
    public ToolChoice(String type) {
        this.type = type;
    }
    
    public ToolChoice(String type, Function function) {
        this.type = type;
        this.function = function;
    }
}

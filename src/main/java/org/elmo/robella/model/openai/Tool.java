package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OpenAI Tool定义
 * 支持 function 和 custom 两种工具类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tool {
    
    /**
     * Tool的类型
     * - "function": 函数工具，当前仅支持此类型
     * - "custom": 自定义工具（未来支持）
     */
    private String type;
    
    /**
     * Function定义
     * 当 type="function" 时必需
     */
    private Function function;
    
    /**
     * Custom Tool定义
     * 当 type="custom" 时使用
     */
    private CustomTool custom;
}

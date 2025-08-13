package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OpenAI Custom Tool Format 定义
 * 自定义工具的输入格式配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomToolFormat {
    
    /**
     * 格式类型
     * 当前支持：
     * - "text": 文本格式（无约束自由文本，总是"text"）
     * - "grammar": 语法格式（用户定义的语法，总是"grammar"）
     */
    private String type;
    
    /**
     * 文本格式配置
     * 当 type="text" 时使用
     */
    private TextFormat text;
    
    /**
     * 语法格式配置  
     * 当 type="grammar" 时使用
     */
    private GrammarFormat grammar;
    
    /**
     * 文本格式定义
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TextFormat {
        /**
         * 无约束文本格式类型，总是 "text"
         */
        private String type;
    }
    
    /**
     * 语法格式定义
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GrammarFormat {
        /**
         * 语法格式类型，总是 "grammar"
         */
        private String type;
        
        /**
         * 用户选择的语法
         */
        private Object grammar;
    }
}

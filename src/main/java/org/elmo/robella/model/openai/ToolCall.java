package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI Tool调用
 * 支持 function 和 custom 两种类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolCall {
    
    /**
     * Function调用信息（当 type="function" 时）
     */
    private FunctionToolCall function;
    
    /**
     * 自定义工具调用信息（当 type="custom" 时）
     */
    private CustomToolCall custom;
    
    /**
     * Function工具调用的详细信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FunctionToolCall {

        private Function function;
        
        /**
         * 要调用的function名称
         */
        private String id;
        
        /**
         * 要调用的function的参数，JSON格式字符串
         */
        private String type;
    }
    
    /**
     * 自定义工具调用的详细信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CustomToolCall {

        private Custom custom;
        
        /**
         * 自定义工具的名称
         */
        private String id;
        
        /**
         * 自定义工具的输入参数（JSON字符串格式）
         */
        private String type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Function {

        /**
         * 传递给函数的参数
         */
        private String augument;

        /**
         * 要调用的函数的名称。
         */
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Custom {

        /**
         * 传递给函数的参数
         */
        private String input;

        /**
         * 要调用的函数的名称。
         */
        private String name;
    }
}

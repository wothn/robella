package org.elmo.robella.model.openai.tool;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI Tool调用
 * 根据官方文档的标准结构定义，支持 function 和 custom 两种类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolCall {
    
    /**
     * 工具调用的ID
     */
    private String id;
    
    /**
     * 工具类型，支持 "function" 和 "custom"
     */
    private String type;
    
    /**
     * 工具调用的索引（在流式响应中使用）
     */
    private Integer index;
    
    /**
     * Function调用信息（当 type="function" 时）
     */
    private Function function;
    
    /**
     * Custom工具调用信息（当 type="custom" 时）
     */
    private Custom custom;

    /**
     * Function工具调用的详细信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Function {

        /**
         * 要调用的函数的名称
         */
        private String name;

        /**
         * 传递给函数的参数，JSON格式字符串
         */
        private String arguments;
    }
    
    /**
     * Custom工具调用的详细信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Custom {

        /**
         * 自定义工具的名称
         */
        private String name;

        /**
         * 传递给自定义工具的输入参数
         */
        private String input;
    }
}

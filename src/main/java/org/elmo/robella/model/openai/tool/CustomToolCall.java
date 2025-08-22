package org.elmo.robella.model.openai.tool;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自定义工具调用
 * 用于调用用户定义的自定义工具
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomToolCall {
    
    /**
     * 自定义工具调用的ID
     */
    private String id;
    
    /**
     * 工具类型，对于自定义工具通常是 "custom"
     */
    private String type;
    
    /**
     * 自定义工具的详细信息
     */
    private CustomTool custom;
    
    /**
     * 自定义工具的具体信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CustomTool {
        
        /**
         * 自定义工具的输入参数（JSON字符串格式）
         */
        private String input;
        
        /**
         * 自定义工具的名称
         */
        private String name;
        
        /**
         * 自定义工具调用的唯一标识符
         */
        private String id;
        
        /**
         * 自定义工具的类型标识
         */
        private String type;
    }
}

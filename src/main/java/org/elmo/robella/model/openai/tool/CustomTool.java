package org.elmo.robella.model.openai.tool;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OpenAI Custom Tool配置
 * 用于指定自定义工具，使用指定格式处理输入
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomTool {
    
    /**
     * 自定义工具名称
     * 必需，用于在工具调用中标识此工具
     */
    private String name;
    
    /**
     * 自定义工具描述
     * 可选，自定义工具的描述，用于提供更多上下文
     */
    private String description;
    
    /**
     * 自定义工具的输入格式
     * 可选，默认为无约束文本
     */
    private CustomToolFormat format;
}

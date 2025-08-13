package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OpenAI Function定义
 * 用于描述可被模型调用的函数工具
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Function {
    
    /**
     * Function名称
     * 必需，函数名称，必须是a-z, A-Z, 0-9，或包含下划线和破折号，最大长度64
     */
    private String name;
    
    /**
     * Function描述
     * 可选，函数功能描述，用于模型选择何时以及如何调用函数
     */
    private String description;
    
    /**
     * Function参数
     * 可选，函数接受的参数，描述为JSON Schema对象
     * 省略parameters定义一个没有参数的函数
     */
    private Object parameters;
    
    /**
     * 严格模式
     * 可选，默认为false
     * 是否在生成函数调用时启用严格的模式遵循
     * 如果设置为true，模型将严格遵循parameters字段中定义的模式
     * 只有当strict为true时，才支持JSON Schema的结构化输出
     */
    private Boolean strict;
}

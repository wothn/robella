package org.elmo.robella.model.openai.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OpenAI 响应格式定义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseFormat {
    
    /**
     * 格式类型：text 或 json_object
     */
    private String type;

    private JsonSchema jsonSchema;

    public static class JsonSchema {
        private String name;    // 响应格式的名称
        private String description;    // 响应格式的用途描述
        private Object schema;    // 响应格式的架构，描述为 JSON Schema 对象。
        private Boolean strict;    // 是否在生成输出时启用严格架构遵守。
    }
}

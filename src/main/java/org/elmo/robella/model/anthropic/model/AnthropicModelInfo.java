package org.elmo.robella.model.anthropic.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Anthropic 模型信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnthropicModelInfo {
    
    /**
     * 模型ID
     */
    private String id;
    
    /**
     * 对象类型
     */
    private String type;
    
    /**
     * 模型显示名称
     */
    private String display_name;
    
    /**
     * 创建时间
     */
    private LocalDateTime created_at;
    
    /**
     * 创建一个带有基本信息的AnthropicModelInfo
     */
    public AnthropicModelInfo(String id, String display_name) {
        this.id = id;
        this.type = "model";
        this.display_name = display_name;
        this.created_at = LocalDateTime.now();
    }
}
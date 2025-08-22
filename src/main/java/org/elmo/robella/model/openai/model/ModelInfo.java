package org.elmo.robella.model.openai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OpenAI 模型信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelInfo {
    
    /**
     * 模型ID
     */
    private String id;
    
    /**
     * 对象类型
     */
    private String object;
    
    /**
     * 模型拥有者
     */
    private String ownedBy;
}

package org.elmo.robella.model.openai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI 模型列表响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelListResponse {
    
    /**
     * 对象类型
     */
    private String object;
    
    /**
     * 模型列表
     */
    private List<ModelInfo> data;
    
    /**
     * 获取模型列表
     */
    public List<ModelInfo> getData() {
        return data;
    }
    
    /**
     * 创建一个带有对象类型和空数据列表的ModelListResponse
     */
    public ModelListResponse(String object) {
        this.object = object;
        this.data = new java.util.ArrayList<>();
    }
}

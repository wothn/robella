package org.elmo.robella.model.anthropic.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Anthropic 模型列表响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnthropicModelListResponse {
    
    /**
     * 模型列表数据
     */
    private List<AnthropicModelInfo> data;
    
    /**
     * 第一页的ID，用于分页
     */
    private String first_id;
    
    /**
     * 是否还有更多数据
     */
    private Boolean has_more;
    
    /**
     * 最后一页的ID，用于分页
     */
    private String last_id;
    
    /**
     * 创建一个带有模型列表的响应
     */
    public AnthropicModelListResponse(List<AnthropicModelInfo> data) {
        this.data = data;
        this.has_more = false;
        this.first_id = data != null && !data.isEmpty() ? data.get(0).getId() : null;
        this.last_id = data != null && !data.isEmpty() ? data.get(data.size() - 1).getId() : null;
    }
}
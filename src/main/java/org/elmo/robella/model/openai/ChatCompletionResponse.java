package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 聊天完成响应
 */
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionResponse {
    
    /**
     * 对话唯一标识符
     */
    private String id;
    
    /**
     * 对象类型
     */
    private String object;
    
    /**
     * 创建时间戳
     */
    private Long created;
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 系统指纹
     */
    @JsonProperty("system_fingerprint")
    private String systemFingerprint;
    
    /**
     * 选择列表
     */
    private List<Choice> choices;
    
    /**
     * 使用统计信息
     */
    private Usage usage;

    @JsonProperty("undefined")
    private Map<String, Object> undefined = new HashMap<>();

    @JsonAnySetter
    private void addUndefined(String key, Object value) {
        undefined.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getUndefined() {
        return undefined;
    }
}

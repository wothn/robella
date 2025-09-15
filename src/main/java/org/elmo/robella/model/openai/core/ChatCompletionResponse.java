package org.elmo.robella.model.openai.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * OpenAI 聊天完成响应
 */
@Data
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
}

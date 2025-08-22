package org.elmo.robella.model.openai.audio;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 音频输出数据结构
 * 当模型生成音频响应时返回的音频数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AudioData {
    
    /**
     * Base64编码的音频数据
     * 音频格式在请求中指定
     */
    private String data;
    
    /**
     * 音频过期时间（Unix时间戳）
     * 音频在服务器上保存的时间限制，用于多轮对话
     */
    @JsonProperty("expires_at")
    private Long expiresAt;
    
    /**
     * 音频唯一标识符
     * 用于在多轮对话中引用此音频响应
     */
    private String id;
    
    /**
     * 音频转录文本
     * 模型生成的音频对应的文本内容
     */
    private String transcript;
}

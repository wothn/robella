package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * 多模态内容片段：text | image_url | input_audio
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentPart {

    /**
     * 片段类型：text, image_url, input_audio
     */
    private String type;

    /**
     * 文本内容（当 type=text）
     */
    private String text;

    /**
     * 图像URL结构（当 type=image_url）
     */
    @JsonProperty("image_url")
    private ImageUrl imageUrl;

    /**
     * 音频输入结构（当 type=input_audio）
     */
    @JsonProperty("input_audio")
    private InputAudio inputAudio;
}

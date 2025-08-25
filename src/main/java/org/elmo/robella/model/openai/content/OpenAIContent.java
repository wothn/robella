package org.elmo.robella.model.openai.content;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * OpenAI 内容块基类
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OpenAITextContent.class, name = "text"),
    @JsonSubTypes.Type(value = OpenAIImageContent.class, name = "image_url"),
    @JsonSubTypes.Type(value = OpenAIAudioContent.class, name = "input_audio")
})
public abstract class OpenAIContent {
    
    /**
     * 内容类型
     */
    private String type;
}

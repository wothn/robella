package org.elmo.robella.model.openai.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OpenAI 图像内容块
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OpenAIImageContent extends OpenAIContent {
    
    /**
     * 图像URL结构
     */
    @JsonProperty("image_url")
    private ImageUrl imageUrl;
}

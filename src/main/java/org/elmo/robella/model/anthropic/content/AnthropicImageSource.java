package org.elmo.robella.model.anthropic.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Anthropic 图片源数据
 */
@Data
public class AnthropicImageSource {
    
    /**
     * 源类型，目前只支持 "base64"
     */
    private String type;
    
    /**
     * 媒体类型，支持: image/jpeg, image/png, image/gif, image/webp
     */
    @JsonProperty("media_type")
    private String mediaType;
    
    /**
     * base64 编码的图片数据
     */
    private String data;
}

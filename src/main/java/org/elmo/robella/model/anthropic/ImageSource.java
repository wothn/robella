package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图像内容来源：支持 base64 (media_type + data) 或 URL（后续需要可扩展）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageSource {
    private String type; // e.g. base64, url

    @JsonProperty("media_type")
    private String mediaType; // e.g. image/png

    private String data; // base64 数据
    private String url;  // 当 type=url
}

package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * 多模态内容片段：text | image_url | input_audio
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    // --- 静态工厂方法 ---
    public static ContentPart ofText(String text) {
        return ContentPart.builder().type("text").text(text).build();
    }
    public static ContentPart ofImage(String url, String detail) {
        return ContentPart.builder().type("image_url")
                .imageUrl(ImageUrl.builder().url(url).detail(detail).build())
                .build();
    }
    public static ContentPart ofAudio(String base64, String format) {
        return ContentPart.builder().type("input_audio")
                .inputAudio(InputAudio.builder().data(base64).format(format).build())
                .build();
    }
}

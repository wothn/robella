package org.elmo.robella.model.anthropic.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Anthropic 文档内容的 source 对象。
 * type: base64 / text / url
 * mediaType: application/pdf, text/plain, image/gif, image/webp 等
 * data: 当 type=base64 或 text 时内容（若 base64 则为编码后字符串）
 * url: 当 type=url 时的资源地址
 */
@Data
public class AnthropicDocumentSource {
    /**
     * base64, text, url
     */
    private String type;

    /**
     * 媒体类型: application/pdf, text/plain, image/gif, image/webp ...
     */
    @JsonProperty("media_type")
    private String mediaType;

    /**
     * base64 或文本内容
     */
    private String data;

    /**
     * 远程 URL
     */
    private String url;
}

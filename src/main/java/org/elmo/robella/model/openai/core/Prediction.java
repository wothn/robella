package org.elmo.robella.model.openai.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Prediction {
    /**
     * 预测输出的类型，目前 OpenAI 仅支持固定值 "content"。
     */
    private String type;

    /**
     * 预测的静态内容。根据官方说明：当重新生成一个文件大部分内容相同，只是少量差异时，
     * 可以把已知的大段输出内容放到这里以加速响应。支持字符串或字符串数组形式。
     *
     * 这里用 List<String> 承载，配合 @JsonFormat(ACCEPT_SINGLE_VALUE_AS_ARRAY) 兼容单字符串输入；
     * 序列化时仍写入为数组或单值（Jackson 会自动处理）。
     */
    @JsonProperty("content")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> content;
}

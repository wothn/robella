package org.elmo.robella.model.openai.audio;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAIAudio {

    // 指定输出音频格式
    private String format;

    // 模型用于响应的声音
    private String voice;
}

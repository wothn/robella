package org.elmo.robella.model.openai.audio;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 输入音频结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InputAudio {
    /**
     * Base64 编码的音频数据
     */
    private String data;

    /**
     * 格式：wav/mp3/pcm 等
     */
    private String format;
}

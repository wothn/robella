package org.elmo.robella.model.openai.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.elmo.robella.model.openai.audio.InputAudio;

/**
 * OpenAI 音频内容块
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OpenAIAudioContent extends OpenAIContent {
    
    /**
     * 音频输入结构
     */
    @JsonProperty("input_audio")
    private InputAudio inputAudio;
}

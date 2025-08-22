package org.elmo.robella.model.openai.content;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OpenAI 文件内容块
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OpenAIFileContent extends OpenAIContent{
    private File file;
}

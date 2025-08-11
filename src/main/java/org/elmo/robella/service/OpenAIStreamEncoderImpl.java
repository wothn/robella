package org.elmo.robella.service;

import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.openai.*;
import org.elmo.robella.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 默认实现：当前仅支持纯文本增量；后续可扩展 role/tool_calls/reasoning 等。
 */
@Component
public class OpenAIStreamEncoderImpl implements OpenAIStreamEncoder {

    @Override
    public String encodeChunk(UnifiedStreamChunk chunk, String model, String idHint) {
        if (chunk == null) return null;
        if (chunk.isFinished()) return encodeDone();
        String deltaText = chunk.getContentDelta();
        if (deltaText == null || deltaText.isEmpty()) return null;

        ChatCompletionChunk response = new ChatCompletionChunk();
        response.setId(idHint != null ? idHint : ("chatcmpl-" + System.nanoTime()));
        response.setObject("chat.completion.chunk");
        response.setCreated(System.currentTimeMillis() / 1000);
        response.setModel(model);

        Choice choice = new Choice();
        choice.setIndex(0);
        Delta delta = new Delta();
        delta.setContent(List.of(ContentPart.ofText(deltaText)));
        choice.setDelta(delta);
        response.setChoices(List.of(choice));
        return JsonUtils.toJson(response);
    }

    @Override
    public String encodeDone() { return "[DONE]"; }
}

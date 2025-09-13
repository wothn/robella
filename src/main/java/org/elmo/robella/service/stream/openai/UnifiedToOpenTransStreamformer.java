package org.elmo.robella.service.stream.openai;

import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.openai.stream.ChatCompletionChunk;
import org.elmo.robella.service.stream.UnifiedToEndpointStreamTransformer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 统一格式到OpenAI端点格式的转换器
 */
@Component
public class UnifiedToOpenTransStreamformer implements UnifiedToEndpointStreamTransformer<ChatCompletionChunk> {
    
    @Override
    public Flux<ChatCompletionChunk> transform(Flux<UnifiedStreamChunk> unifiedStream, String sessionId) {
        // 从统一格式转换回OpenAI格式
        return unifiedStream.mapNotNull(unifiedChunk -> {
            ChatCompletionChunk chunk = new ChatCompletionChunk();
            chunk.setId(unifiedChunk.getId());
            chunk.setCreated(unifiedChunk.getCreated());
            chunk.setModel(unifiedChunk.getModel());
            chunk.setChoices(unifiedChunk.getChoices());
            chunk.setObject(unifiedChunk.getObject());
            chunk.setSystemFingerprint(unifiedChunk.getSystemFingerprint());
            chunk.setUsage(unifiedChunk.getUsage());
            return chunk;
        });
    }
}
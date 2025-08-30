package org.elmo.robella.service.stream.openai;

import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.openai.stream.ChatCompletionChunk;
import org.elmo.robella.service.stream.StreamToUnifiedTransformer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * OpenAI流式响应到统一格式的转换器
 */
@Component
public class OpenAIStreamToUnifiedTransformer implements StreamToUnifiedTransformer<ChatCompletionChunk> {
    
    @Override
    public Flux<UnifiedStreamChunk> transformToUnified(Flux<ChatCompletionChunk> vendorStream, String sessionId) {
        // OpenAI的转换逻辑相对简单，因为其格式与统一格式接近
        return vendorStream.map(chunk -> {
            UnifiedStreamChunk unifiedChunk = new UnifiedStreamChunk();
            unifiedChunk.setId(chunk.getId());
            unifiedChunk.setCreated(chunk.getCreated());
            unifiedChunk.setModel(chunk.getModel());
            unifiedChunk.setChoices(chunk.getChoices());
            unifiedChunk.setObject(chunk.getObject());
            unifiedChunk.setSystemFingerprint(chunk.getSystemFingerprint());
            unifiedChunk.setUsage(chunk.getUsage());
            return unifiedChunk;
        }).filter(unifiedChunk -> unifiedChunk != null && unifiedChunk.getChoices() != null && !unifiedChunk.getChoices().isEmpty());
    }
}
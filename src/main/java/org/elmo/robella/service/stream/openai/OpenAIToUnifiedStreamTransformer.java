package org.elmo.robella.service.stream.openai;

import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.openai.stream.ChatCompletionChunk;
import org.elmo.robella.service.stream.EndpointToUnifiedStreamTransformer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * OpenAI流式响应到统一格式的转换器
 */
@Component
public class OpenAIToUnifiedStreamTransformer implements EndpointToUnifiedStreamTransformer<ChatCompletionChunk> {
    
    @Override
    public Flux<UnifiedStreamChunk> transform(Flux<ChatCompletionChunk> vendorStream, String sessionId) {
        // OpenAI的转换逻辑相对简单，因为其格式与统一格式接近
        // 虽然输入流已经通过mapNotNull过滤，但保留mapNotNull作为防御性编程
        return vendorStream.mapNotNull(chunk -> {
            UnifiedStreamChunk unifiedChunk = new UnifiedStreamChunk();
            unifiedChunk.setId(chunk.getId());
            unifiedChunk.setCreated(chunk.getCreated());
            unifiedChunk.setModel(chunk.getModel());
            unifiedChunk.setChoices(chunk.getChoices());
            unifiedChunk.setObject(chunk.getObject());
            unifiedChunk.setSystemFingerprint(chunk.getSystemFingerprint());
            unifiedChunk.setUsage(chunk.getUsage());
            return unifiedChunk;
        });
    }
}
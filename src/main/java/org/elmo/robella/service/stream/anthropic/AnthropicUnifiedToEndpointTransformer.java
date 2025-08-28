package org.elmo.robella.service.stream.anthropic;

import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.anthropic.stream.*;
import org.elmo.robella.model.openai.core.Choice;
import org.elmo.robella.model.openai.stream.Delta;
import org.elmo.robella.service.stream.UnifiedToEndpointTransformer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统一格式到Anthropic端点格式的转换器
 * 需要维护会话状态，如消息ID、工具调用索引等
 */
@Component
public class AnthropicUnifiedToEndpointTransformer implements UnifiedToEndpointTransformer<Object> {
    
    // 会话状态存储，实际应用中可能需要使用更持久化的存储方案
    private final Map<String, AnthropicEndpointSessionState> sessionStates = new ConcurrentHashMap<>();
    
    @Override
    public Flux<Object> transformToEndpoint(Flux<UnifiedStreamChunk> unifiedStream, String sessionId) {
        // 初始化会话状态
        sessionStates.putIfAbsent(sessionId, new AnthropicEndpointSessionState());
        
        return unifiedStream.map(unifiedChunk -> {
            AnthropicEndpointSessionState state = sessionStates.get(sessionId);
            return processUnifiedChunk(unifiedChunk, state);
        }).doFinally(signalType -> {
            // 清理会话状态
            sessionStates.remove(sessionId);
        });
    }
    
    private Object processUnifiedChunk(UnifiedStreamChunk unifiedChunk, AnthropicEndpointSessionState state) {
        // 在实际实现中，我们需要将UnifiedStreamChunk转换为Anthropic的SSE事件格式
        // 这里简化处理，返回一个基本的消息开始事件
        
        // 如果是第一个chunk，创建MessageStartEvent
        if (state.getMessageId() == null && unifiedChunk.getId() != null) {
            state.setMessageId(unifiedChunk.getId());
            state.setLastModel(unifiedChunk.getModel());
            
            AnthropicMessageStartEvent messageStart = new AnthropicMessageStartEvent();
            messageStart.setType("message_start");
            // 在实际实现中，这里需要构造完整的消息对象
            return messageStart;
        }
        
        // 处理choices中的delta内容
        if (unifiedChunk.getChoices() != null && !unifiedChunk.getChoices().isEmpty()) {
            // 简化处理，只处理第一个choice
            Choice choice = unifiedChunk.getChoices().get(0);
            
            if (choice.getDelta() != null) {
                // 创建ContentBlockDeltaEvent
                AnthropicContentBlockDeltaEvent blockDelta = new AnthropicContentBlockDeltaEvent();
                blockDelta.setType("content_block_delta");
                blockDelta.setIndex(0); // 简化处理，固定索引
                
                AnthropicDelta delta = new AnthropicDelta();
                
                // 根据delta内容类型设置
                if (choice.getDelta().getReasoningContent() != null) {
                    delta.setType("text_delta");
                    delta.setText(choice.getDelta().getReasoningContent());
                }
                
                blockDelta.setDelta(delta);
                return blockDelta;
            }
        }
        
        // 默认返回ping事件保持连接
        AnthropicPingEvent ping = new AnthropicPingEvent();
        ping.setType("ping");
        return ping;
    }
    
    /**
     * Anthropic端点会话状态
     */
    private static class AnthropicEndpointSessionState {
        private String messageId;
        private String lastModel;
        
        // Getters and setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getLastModel() { return lastModel; }
        public void setLastModel(String lastModel) { this.lastModel = lastModel; }
    }
}
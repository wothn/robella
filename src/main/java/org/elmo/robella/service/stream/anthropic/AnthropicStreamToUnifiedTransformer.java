package org.elmo.robella.service.stream.anthropic;

import org.elmo.robella.model.anthropic.stream.AnthropicMessageStartEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicContentBlockStartEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicContentBlockDeltaEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicMessageDeltaEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicContentBlockStopEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicMessageStopEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicPingEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicErrorEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicDelta;
import org.elmo.robella.model.anthropic.content.AnthropicContent;
import org.elmo.robella.model.anthropic.content.AnthropicToolUseContent;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.openai.core.Choice;
import org.elmo.robella.model.openai.stream.Delta;
import org.elmo.robella.model.openai.tool.ToolCall;
import org.elmo.robella.service.stream.StreamToUnifiedTransformer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Anthropic流式响应到统一格式的转换器
 * 需要维护会话状态，如消息ID、工具调用索引等
 */
@Component
public class AnthropicStreamToUnifiedTransformer implements StreamToUnifiedTransformer<Object> {
    
    // 会话状态存储，实际应用中可能需要使用更持久化的存储方案
    private final Map<String, AnthropicStreamSessionState> sessionStates = new ConcurrentHashMap<>();
    
    @Override
    public Flux<UnifiedStreamChunk> transformToUnified(Flux<Object> vendorStream, String sessionId) {
        // 初始化会话状态
        sessionStates.putIfAbsent(sessionId, new AnthropicStreamSessionState());
        
        return vendorStream.map(event -> {
            AnthropicStreamSessionState state = sessionStates.get(sessionId);
            return processEvent(event, state);
        }).doFinally(signalType -> {
            // 清理会话状态
            sessionStates.remove(sessionId);
        });
    }
    
    private UnifiedStreamChunk processEvent(Object event, AnthropicStreamSessionState state) {
        UnifiedStreamChunk chunk = new UnifiedStreamChunk();
        chunk.setObject("chat.completion.chunk");
        
        if (event instanceof AnthropicMessageStartEvent) {
            AnthropicMessageStartEvent messageStart = (AnthropicMessageStartEvent) event;
            state.setMessageId(messageStart.getMessage().getId());
            state.setLastModel(messageStart.getMessage().getModel());
            
            // 创建初始chunk
            chunk.setId(state.getMessageId());
            chunk.setModel(state.getLastModel());
            chunk.setChoices(new ArrayList<>());
            
        } else if (event instanceof AnthropicContentBlockStartEvent) {
            AnthropicContentBlockStartEvent blockStart = (AnthropicContentBlockStartEvent) event;
            int index = blockStart.getIndex();
            
            // 更新状态
            state.getContentBlocks().put(index, blockStart.getContentBlock());
            
            // 如果是tool_use类型，记录映射关系
            if (blockStart.getContentBlock() != null && 
                "tool_use".equals(blockStart.getContentBlock().getType())) {
                state.getToolUseIndices().put(index, state.getNextToolCallIndex().getAndIncrement());
            }
            
            // 创建chunk
            chunk.setId(state.getMessageId());
            chunk.setModel(state.getLastModel());
            
            Choice choice = new Choice();
            choice.setIndex(0);
            
            Delta delta = new Delta();
            delta.setRole("assistant");
            delta.setReasoningContent("");
            
            choice.setDelta(delta);
            chunk.setChoices(List.of(choice));
            
        } else if (event instanceof AnthropicContentBlockDeltaEvent) {
            AnthropicContentBlockDeltaEvent blockDelta = (AnthropicContentBlockDeltaEvent) event;
            int index = blockDelta.getIndex();
            AnthropicDelta delta = blockDelta.getDelta();
            
            chunk.setId(state.getMessageId());
            chunk.setModel(state.getLastModel());
            
            Choice choice = new Choice();
            choice.setIndex(0);
            
            Delta unifiedDelta = new Delta();
            
            // 处理不同类型的内容块
            AnthropicContent contentBlock = state.getContentBlocks().get(index);
            if (contentBlock != null && "text".equals(contentBlock.getType())) {
                // 文本内容
                unifiedDelta.setReasoningContent(delta.getText());
            } else if (contentBlock != null && "tool_use".equals(contentBlock.getType())) {
                // 工具调用
                Integer toolCallIndex = state.getToolUseIndices().get(index);
                if (toolCallIndex != null) {
                    List<ToolCall> toolCalls = new ArrayList<>();
                    ToolCall toolCall = new ToolCall();
                    toolCall.setIndex(toolCallIndex);
                    
                    // 创建Function对象并设置属性
                    ToolCall.Function function = new ToolCall.Function();
                    function.setArguments(delta.getPartialJson());
                    toolCall.setFunction(function);
                    
                    toolCalls.add(toolCall);
                    unifiedDelta.setToolCalls(toolCalls);
                }
            }
            
            choice.setDelta(unifiedDelta);
            chunk.setChoices(List.of(choice));
            
        } else if (event instanceof AnthropicMessageDeltaEvent) {
            AnthropicMessageDeltaEvent messageDelta = (AnthropicMessageDeltaEvent) event;
            
            chunk.setId(state.getMessageId());
            chunk.setModel(state.getLastModel());
            // 注意：这里需要进行类型转换，将AnthropicUsage转换为OpenAI的Usage
            // 在实际实现中，您可能需要创建一个转换方法
            chunk.setUsage(null); // 暂时设置为null，实际实现中需要转换
            
            Choice choice = new Choice();
            choice.setIndex(0);
            choice.setFinishReason(messageDelta.getDelta().getStopReason());
            chunk.setChoices(List.of(choice));
            
        } else if (event instanceof AnthropicContentBlockStopEvent || 
                   event instanceof AnthropicMessageStopEvent || 
                   event instanceof AnthropicPingEvent ||
                   event instanceof AnthropicErrorEvent) {
            // 这些事件不需要转换为统一格式的chunk
            // 可以根据需要处理错误或停止事件
            chunk.setId(state.getMessageId());
            chunk.setModel(state.getLastModel());
            chunk.setChoices(new ArrayList<>());
        }
        
        return chunk;
    }
    
    /**
     * Anthropic流式会话状态
     */
    private static class AnthropicStreamSessionState {
        private String messageId;
        private String lastModel;
        private final Map<Integer, AnthropicContent> contentBlocks = new ConcurrentHashMap<>();
        private final Map<Integer, Integer> toolUseIndices = new ConcurrentHashMap<>();
        private final AtomicInteger nextToolCallIndex = new AtomicInteger(0);
        
        // Getters and setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getLastModel() { return lastModel; }
        public void setLastModel(String lastModel) { this.lastModel = lastModel; }
        
        public Map<Integer, AnthropicContent> getContentBlocks() { return contentBlocks; }
        public Map<Integer, Integer> getToolUseIndices() { return toolUseIndices; }
        public AtomicInteger getNextToolCallIndex() { return nextToolCallIndex; }
    }
}
package org.elmo.robella.service.stream.anthropic;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.anthropic.stream.*;
import org.elmo.robella.model.anthropic.content.*;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.anthropic.core.AnthropicUsage;
import org.elmo.robella.model.openai.content.OpenAITextContent;
import org.elmo.robella.model.openai.core.Usage;
import org.elmo.robella.model.openai.stream.Delta;
import org.elmo.robella.model.openai.tool.ToolCall;
import org.elmo.robella.service.stream.UnifiedToEndpointStreamTransformer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UnifiedToAnthropicStreamTransformer implements UnifiedToEndpointStreamTransformer<AnthropicStreamEvent> {

    private final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();

    @Override
    public Stream<AnthropicStreamEvent> transform(Stream<UnifiedStreamChunk> unifiedStream, String sessionId) {
        SessionState state = sessionStates.computeIfAbsent(sessionId, k -> new SessionState());
        log.info("[AnthropicTransformer] 开始流式转换，sessionId: {}", sessionId);

        try {
            return unifiedStream
                    .flatMap(chunk -> processChunk(chunk, state).stream())
                    .onClose(() -> {
                        sessionStates.remove(sessionId);
                        log.info("[AnthropicTransformer] 流式转换结束，sessionId: {}", sessionId);
                    });
        } catch (Exception e) {
            log.error("[AnthropicTransformer] 流式转换过程中发生异常: sessionId: {}", sessionId, e);
            throw new RuntimeException("流式转换失败: " + e.getMessage(), e);
        }
    }

    private List<AnthropicStreamEvent> processChunk(UnifiedStreamChunk chunk, SessionState state) {
        log.debug("[AnthropicTransformer] 处理chunk，model: {}", chunk.getModel());
        
        // 添加详细日志来调试usage chunk
        if (chunk.getUsage() != null) {
            log.debug("[AnthropicTransformer] processChunk - 收到包含usage的chunk: id={}, choicesSize={}, choicesNull={}, usage={}", 
                chunk.getId(), 
                chunk.getChoices() != null ? chunk.getChoices().size() : "null",
                chunk.getChoices() == null,
                chunk.getUsage());
        }

        List<AnthropicStreamEvent> events = new ArrayList<>();

        try {
            // 1. 检查是否需要发送 message_start 事件
            if (!state.isMessageStarted() && hasRole(chunk)) {
                events.add(createMessageStartEvent(chunk, state));
                state.setMessageStarted(true);
                log.debug("[AnthropicTransformer] 发送 message_start 事件");
            }

            // 2. 按顺序处理内容，避免频繁切换
            ContentBlockType newContentType = determineContentType(chunk);
            
            if (newContentType != null) {
                // 如果内容类型发生变化，先关闭旧块
                if (state.getActiveContent() != null && state.getActiveContent() != newContentType) {
                    events.add(createContentBlockStopEvent(state.getContentBlockIndex()));
                    state.incrementContentBlockIndex();
                    state.setActiveContent(null);
                }
                
                // 开启新块（如果需要）
                if (state.getActiveContent() == null) {
                    events.add(createContentBlockStartEvent(newContentType, chunk, state));
                    state.setActiveContent(newContentType);
                }
                
                // 处理内容增量
                switch (newContentType) {
                    case THINKING:
                        events.addAll(processReasoningDelta(chunk, state));
                        break;
                    case TEXT:
                        events.addAll(processTextDelta(chunk, state));
                        break;
                    case TOOL_USE:
                        events.addAll(processToolCallDelta(chunk, state));
                        break;
                }
            }

            // 5. 处理消息结束
            if (hasFinishReason(chunk)) {
                state.setFinishReason(chunk.getChoices().get(0).getFinishReason());
                log.debug("[AnthropicTransformer] 检测到消息结束，原因: {}", state.getFinishReason());
            }

            if (hasUsage(chunk)) {
                state.setUsage(chunk.getUsage());
                log.debug("[AnthropicTransformer] 收到usage信息");
            }

            if (state.getFinishReason() != null && state.getUsage() != null && !state.isMessageEnded()) {
                events.addAll(processMessageEnd(state));
                log.debug("[AnthropicTransformer] 处理消息结束");
            }

            return events;

        } catch (Exception e) {
            log.error("[AnthropicTransformer] 处理chunk时发生异常", e);
            throw new RuntimeException("流式转换失败: " + e.getMessage(), e);
        }
    }

    private ContentBlockType determineContentType(UnifiedStreamChunk chunk) {
        if (hasReasoningContent(chunk)) {
            return ContentBlockType.THINKING;
        }
        if (hasToolCalls(chunk)) {
            return ContentBlockType.TOOL_USE;
        }
        if (hasTextContent(chunk)) {
            return ContentBlockType.TEXT;
        }
        return null;
    }

    /* --------------------------- 核心处理方法 --------------------------- */

    private boolean hasRole(UnifiedStreamChunk chunk) {
        return chunk.getChoices() != null && !chunk.getChoices().isEmpty()
                && chunk.getChoices().get(0).getDelta() != null
                && chunk.getChoices().get(0).getDelta().getRole() != null;
    }

    private boolean hasFinishReason(UnifiedStreamChunk chunk) {
        return chunk.getChoices() != null && !chunk.getChoices().isEmpty()
                && chunk.getChoices().get(0).getFinishReason() != null;
    }

    private boolean hasTextContent(UnifiedStreamChunk chunk) {
        if (chunk.getChoices() == null || chunk.getChoices().isEmpty()) {
            return false;
        }
        Delta delta = chunk.getChoices().get(0).getDelta();
        if (delta == null || delta.getContent() == null) {
            return false;
        }
        return delta.getContent().stream()
                .anyMatch(content -> "text".equals(content.getType()) &&
                        content instanceof OpenAITextContent &&
                        ((OpenAITextContent) content).getText() != null &&
                        !((OpenAITextContent) content).getText().isEmpty());
    }

    private boolean hasToolCalls(UnifiedStreamChunk chunk) {
        if (chunk.getChoices() == null || chunk.getChoices().isEmpty()) {
            return false;
        }
        Delta delta = chunk.getChoices().get(0).getDelta();
        return delta != null && delta.getToolCalls() != null && !delta.getToolCalls().isEmpty();
    }

    private boolean hasReasoningContent(UnifiedStreamChunk chunk) {
        if (chunk.getChoices() == null || chunk.getChoices().isEmpty()) {
            return false;
        }
        Delta delta = chunk.getChoices().get(0).getDelta();
        return delta != null && delta.getReasoningContent() != null && 
               !delta.getReasoningContent().trim().isEmpty();
    }

    private boolean hasUsage(UnifiedStreamChunk chunk) {
        return chunk.getUsage() != null;
    }

    /* --------------------------- 增量处理方法 --------------------------- */

    private List<AnthropicStreamEvent> processTextDelta(UnifiedStreamChunk chunk, SessionState state) {
        List<AnthropicStreamEvent> events = new ArrayList<>();
        String textDelta = extractTextDelta(chunk);
        
        if (textDelta != null && !textDelta.isEmpty()) {
            events.add(createTextDeltaEvent(textDelta, state));
            log.trace("[AnthropicTransformer] 添加文本增量: {}", textDelta);
        }
        
        return events;
    }

    private List<AnthropicStreamEvent> processToolCallDelta(UnifiedStreamChunk chunk, SessionState state) {
        List<AnthropicStreamEvent> events = new ArrayList<>();
        Delta delta = chunk.getChoices().get(0).getDelta();
        ToolCall toolCall = delta.getToolCalls().get(0);
        
        // 处理工具参数增量
        if (toolCall.getFunction() != null && toolCall.getFunction().getArguments() != null) {
            String argsDelta = toolCall.getFunction().getArguments();
            events.add(createInputJsonDeltaEvent(argsDelta, state));
            log.trace("[AnthropicTransformer] 添加工具参数增量: {}", argsDelta);
        }
        
        return events;
    }

    private List<AnthropicStreamEvent> processReasoningDelta(UnifiedStreamChunk chunk, SessionState state) {
        List<AnthropicStreamEvent> events = new ArrayList<>();
        String thinkingDelta = chunk.getChoices().get(0).getDelta().getReasoningContent();
        
        if (thinkingDelta != null && !thinkingDelta.isEmpty()) {
            events.add(createThinkingDeltaEvent(thinkingDelta, state));
            log.trace("[AnthropicTransformer] 添加推理增量: {}", thinkingDelta);
        }
        
        return events;
    }

    private List<AnthropicStreamEvent> processMessageEnd(SessionState state) {
        List<AnthropicStreamEvent> events = new ArrayList<>();
        
        // 避免重复处理
        if (state.isMessageEnded()) {
            return events;
        }
        
        // 关闭当前内容块（如果有）
        if (state.getActiveContent() != null) {
            events.add(createContentBlockStopEvent(state.getContentBlockIndex()));
            state.setActiveContent(null);
        }

        // 创建消息增量事件
        events.add(createMessageDeltaEvent(state));

        // 添加消息停止事件
        events.add(createMessageStopEvent());

        state.setMessageEnded(true);
        log.info("[AnthropicTransformer] 消息结束");
        return events;
    }

    /* --------------------------- 具体事件创建方法 --------------------------- */

    private AnthropicMessageStartEvent createMessageStartEvent(UnifiedStreamChunk chunk, SessionState state) {
        AnthropicMessageStartEvent event = new AnthropicMessageStartEvent();
        event.setType("message_start");

        AnthropicMessage message = new AnthropicMessage();
        String messageId = chunk.getId() != null ? chunk.getId()
                : "msg_" + UUID.randomUUID().toString().replace("-", "");
        message.setId(messageId);
        message.setType("message");
        message.setRole("assistant");
        message.setModel(chunk.getModel());
        message.setContent(new ArrayList<>());

        AnthropicUsage usage = new AnthropicUsage();
        usage.setInputTokens(0);
        usage.setOutputTokens(0);

        message.setUsage(usage);
        event.setMessage(message);
        return event;
    }

    private AnthropicContentBlockStartEvent createContentBlockStartEvent(ContentBlockType type, UnifiedStreamChunk chunk, SessionState state) {
        AnthropicContentBlockStartEvent event = new AnthropicContentBlockStartEvent();
        event.setType("content_block_start");
        event.setIndex(state.getContentBlockIndex());

        AnthropicContent content;
        switch (type) {
            case TEXT:
                content = new AnthropicTextContent();
                ((AnthropicTextContent) content).setType("text");
                ((AnthropicTextContent) content).setText("");
                break;
            case THINKING:
                content = new AnthropicThinkingContent();
                ((AnthropicThinkingContent) content).setType("thinking");
                ((AnthropicThinkingContent) content).setThinking("");
                break;
            case TOOL_USE:
                // 获取工具调用信息
                Delta delta = chunk.getChoices().get(0).getDelta();
                ToolCall toolCall = delta.getToolCalls().get(0);
                
                content = new AnthropicToolUseContent();
                ((AnthropicToolUseContent) content).setType("tool_use");
                ((AnthropicToolUseContent) content).setId(toolCall.getId());
                ((AnthropicToolUseContent) content).setName(toolCall.getFunction().getName());
                ((AnthropicToolUseContent) content).setInput(new HashMap<>());
                break;
            default:
                throw new IllegalArgumentException("不支持的内容类型: " + type);
        }

        event.setContentBlock(content);
        return event;
    }

    private AnthropicContentBlockDeltaEvent createTextDeltaEvent(String text, SessionState state) {
        AnthropicContentBlockDeltaEvent event = new AnthropicContentBlockDeltaEvent();
        event.setType("content_block_delta");
        event.setIndex(state.getContentBlockIndex());

        AnthropicDelta delta = new AnthropicDelta();
        delta.setType("text_delta");
        delta.setText(text);

        event.setDelta(delta);
        return event;
    }

    private AnthropicContentBlockDeltaEvent createInputJsonDeltaEvent(String partialJson, SessionState state) {
        AnthropicContentBlockDeltaEvent event = new AnthropicContentBlockDeltaEvent();
        event.setType("content_block_delta");
        event.setIndex(state.getContentBlockIndex());

        AnthropicDelta delta = new AnthropicDelta();
        delta.setType("input_json_delta");
        delta.setPartialJson(partialJson);

        event.setDelta(delta);
        return event;
    }

    private AnthropicContentBlockDeltaEvent createThinkingDeltaEvent(String thinking, SessionState state) {
        AnthropicContentBlockDeltaEvent event = new AnthropicContentBlockDeltaEvent();
        event.setType("content_block_delta");
        event.setIndex(state.getContentBlockIndex());

        AnthropicDelta delta = new AnthropicDelta();
        delta.setType("thinking_delta");
        delta.setThinking(thinking);

        event.setDelta(delta);
        return event;
    }

    private AnthropicContentBlockStopEvent createContentBlockStopEvent(Integer index) {
        AnthropicContentBlockStopEvent event = new AnthropicContentBlockStopEvent();
        event.setType("content_block_stop");
        event.setIndex(index);
        return event;
    }

    private AnthropicMessageDeltaEvent createMessageDeltaEvent(SessionState state) {
        AnthropicMessageDeltaEvent event = new AnthropicMessageDeltaEvent();
        event.setType("message_delta");

        AnthropicDelta delta = new AnthropicDelta();
        delta.setStopReason(mapFinishReason(state.getFinishReason()));

        event.setUsage(mapUsage(state.getUsage()));
        event.setDelta(delta);
        return event;
    }

    private AnthropicMessageStopEvent createMessageStopEvent() {
        AnthropicMessageStopEvent event = new AnthropicMessageStopEvent();
        event.setType("message_stop");
        return event;
    }

    /* --------------------------- 辅助方法 --------------------------- */

    private String extractTextDelta(UnifiedStreamChunk chunk) {
        if (chunk.getChoices() == null || chunk.getChoices().isEmpty()) {
            return null;
        }

        Delta delta = chunk.getChoices().get(0).getDelta();
        if (delta == null || delta.getContent() == null) {
            return null;
        }

        return delta.getContent().stream()
                .filter(content -> "text".equals(content.getType()))
                .map(content -> ((OpenAITextContent) content).getText())
                .filter(text -> text != null && !text.isEmpty())
                .collect(Collectors.joining());
    }

    private String mapFinishReason(String openaiFinishReason) {
        if (openaiFinishReason == null) {
            return "end_turn";
        }

        switch (openaiFinishReason) {
            case "stop":
                return "end_turn";
            case "length":
                return "max_tokens";
            case "tool_calls":
                return "tool_use";
            case "content_filter":
                return "end_turn";
            default:
                return "end_turn";
        }
    }

    private AnthropicUsage mapUsage(Usage usage) {
        if (usage == null) {
            return null;
        }
        AnthropicUsage anthropicUsage = new AnthropicUsage();
        anthropicUsage.setInputTokens(usage.getPromptTokens() != null ? usage.getPromptTokens() : 0);
        anthropicUsage.setOutputTokens(usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0);
        anthropicUsage.setCacheReadInputTokens(usage.getPromptCacheHitTokens() != null ? usage.getPromptCacheHitTokens() : 0);
        return anthropicUsage;
    }

    /* --------------------------- 状态管理类 --------------------------- */

    @Data
    private static class SessionState {
        private boolean messageStarted;
        private ContentBlockType activeContent;
        private Integer contentBlockIndex = 0;
        private String finishReason;
        private Usage usage;
        private boolean messageEnded;
        
        public void incrementContentBlockIndex() {
            this.contentBlockIndex++;
        }
    }

    private enum ContentBlockType {
        TOOL_USE,
        THINKING,
        TEXT,
    }
}
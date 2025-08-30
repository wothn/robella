package org.elmo.robella.service.stream.anthropic;

import lombok.Data;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.anthropic.stream.*;
import org.elmo.robella.model.anthropic.content.AnthropicTextContent;
import org.elmo.robella.model.anthropic.content.AnthropicToolUseContent;
import org.elmo.robella.model.anthropic.content.AnthropicThinkingContent;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.anthropic.core.AnthropicUsage;
import org.elmo.robella.model.openai.core.Choice;
import org.elmo.robella.model.openai.stream.Delta;
import org.elmo.robella.model.openai.content.OpenAIContent;
import org.elmo.robella.model.openai.content.OpenAITextContent;
import org.elmo.robella.model.openai.tool.ToolCall;
import org.elmo.robella.service.stream.UnifiedToEndpointTransformer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

        return unifiedStream.flatMap(unifiedChunk -> {
            AnthropicEndpointSessionState state = sessionStates.get(sessionId);
            Object result = processUnifiedChunk(unifiedChunk, state);

            if (result instanceof List) {
                // 如果返回的是事件列表，则将列表中的每个事件作为单独的流元素发出
                @SuppressWarnings("unchecked")
                List<Object> events = (List<Object>) result;
                return Flux.fromIterable(events);
            } else if (result != null) {
                // 如果返回的是单个事件，则直接发出
                return Flux.just(result);
            } else {
                // 如果返回null，则跳过
                return Flux.empty();
            }
        }).doFinally(signalType -> {
            // 清理会话状态
            sessionStates.remove(sessionId);
        });
    }

    private Object processUnifiedChunk(UnifiedStreamChunk unifiedChunk, AnthropicEndpointSessionState state) {
        // 用于返回多个事件的结果列表
        List<Object> events = new ArrayList<>();

        // 处理消息开始事件
        if (state.getMessageId() == null && unifiedChunk.getId() != null) {
            state.setMessageId(unifiedChunk.getId());
            state.setLastModel(unifiedChunk.getModel());
            state.setNextContentBlockIndex(new AtomicInteger(0)); // 重置内容块索引

            // 创建MessageStartEvent
            AnthropicMessageStartEvent messageStart = new AnthropicMessageStartEvent();
            messageStart.setType("message_start");

            // 构造完整的消息对象
            AnthropicMessage message = new AnthropicMessage();
            message.setId(unifiedChunk.getId());
            message.setType("message");
            message.setModel(unifiedChunk.getModel());

            // 设置初始使用量信息
            if (unifiedChunk.getUsage() != null) {
                AnthropicUsage usage = new AnthropicUsage();
                usage.setInputTokens(unifiedChunk.getUsage().getPromptTokens());
                usage.setOutputTokens(unifiedChunk.getUsage().getCompletionTokens());
                message.setUsage(usage);
            }

            messageStart.setMessage(message);
            events.add(messageStart);
        }

        // 处理choices中的delta内容
        if (unifiedChunk.getChoices() != null && !unifiedChunk.getChoices().isEmpty()) {
            // 处理第一个choice
            Choice choice = unifiedChunk.getChoices().get(0);

            if (choice.getDelta() != null) {
                Delta delta = choice.getDelta();

                // 处理角色信息（首次出现时）
                if (delta.getRole() != null) {
                    // 创建ContentBlockStartEvent
                    AnthropicContentBlockStartEvent blockStart = new AnthropicContentBlockStartEvent();
                    blockStart.setType("content_block_start");
                    int contentIndex = state.getNextContentBlockIndex().getAndIncrement();

                    // 创建文本内容块
                    AnthropicTextContent contentBlock = new AnthropicTextContent();
                    contentBlock.setType("text");
                    contentBlock.setText("");

                    blockStart.setIndex(contentIndex);
                    blockStart.setContentBlock(contentBlock);
                    events.add(blockStart);

                    // 记录内容块状态
                    BlockState blockState = new BlockState();
                    blockState.setType(BlockType.TEXT);
                    blockState.setIndex(contentIndex);
                    blockState.setStarted(true);
                    blockState.setStopped(false);
                    state.getBlockStates().put(contentIndex, blockState);
                }

                // 处理工具调用
                if (delta.getToolCalls() != null && !delta.getToolCalls().isEmpty()) {
                    Object toolCallEvent = processToolCalls(delta.getToolCalls(), state);
                    if (toolCallEvent != null) {
                        events.add(toolCallEvent);
                    }
                    
                    // 如果这是最后一个工具调用参数片段，停止tool_use块
                    if (isLastToolCallChunk(delta.getToolCalls())) {
                        Object toolStopEvent = generateContentBlockStopEvent(state, BlockType.TOOL_USE);
                        if (toolStopEvent != null) {
                            events.add(toolStopEvent);
                        }
                    }
                }

                // 处理文本内容
                if (delta.getContent() != null && !delta.getContent().isEmpty()) {
                    Object contentEvent = processContent(delta.getContent(), state);
                    if (contentEvent != null) {
                        events.add(contentEvent);
                    }
                }
                
                // 检测文本内容是否完成（当推理内容开始或工具调用开始时）
                if (shouldStopTextBlock(delta)) {
                    Object textStopEvent = generateContentBlockStopEvent(state, BlockType.TEXT);
                    if (textStopEvent != null) {
                        events.add(textStopEvent);
                    }
                }

                // 处理推理内容
                if (delta.getReasoningContent() != null) {
                    Object reasoningEvent = processReasoningContent(delta.getReasoningContent(), state);
                    if (reasoningEvent != null) {
                        if (reasoningEvent instanceof List) {
                            // 如果返回的是事件列表，添加所有事件
                            @SuppressWarnings("unchecked")
                            List<Object> reasoningEvents = (List<Object>) reasoningEvent;
                            events.addAll(reasoningEvents);
                        } else {
                            // 如果是单个事件，直接添加
                            events.add(reasoningEvent);
                        }
                    }
                    
                    // 如果这是最后一个推理内容片段，生成signature_delta并停止thinking块
                    if (isLastReasoningChunk(unifiedChunk)) {
                        // 生成signature_delta事件
                        Object signatureEvent = generateSignatureDeltaEvent(state);
                        if (signatureEvent != null) {
                            events.add(signatureEvent);
                        }
                        
                        // 停止thinking块
                        Object thinkingStopEvent = generateContentBlockStopEvent(state, BlockType.THINKING);
                        if (thinkingStopEvent != null) {
                            events.add(thinkingStopEvent);
                        }
                    }
                }
            }

            // 处理完成原因
            if (choice.getFinishReason() != null) {
                // 为所有已启动但未停止的内容块生成content_block_stop事件
                generateStopEventsForAllBlocks(state, events);

                // 创建MessageDeltaEvent
                AnthropicMessageDeltaEvent messageDelta = new AnthropicMessageDeltaEvent();
                messageDelta.setType("message_delta");

                AnthropicDelta delta = new AnthropicDelta();
                delta.setStopReason(convertFinishReasonToStopReason(choice.getFinishReason()));

                // 处理使用量信息
                if (unifiedChunk.getUsage() != null) {
                    AnthropicUsage usage = new AnthropicUsage();
                    usage.setOutputTokens(unifiedChunk.getUsage().getCompletionTokens());
                    delta.setUsage(usage);
                }

                messageDelta.setDelta(delta);
                events.add(messageDelta);

                // 创建MessageStopEvent
                AnthropicMessageStopEvent messageStop = new AnthropicMessageStopEvent();
                messageStop.setType("message_stop");
                events.add(messageStop);

                // 标记消息完成
                state.setMessageCompleted(true);
            }
        }

        // 验证事件序列的完整性
        validateEventSequence(events, state);

        // 根据事件数量返回结果
        if (!events.isEmpty()) {
            if (events.size() == 1) {
                return events.get(0);
            } else {
                // 返回事件列表，需要修改transformToEndpoint方法的返回类型
                return events;
            }
        }

        // 默认返回ping事件保持连接
        AnthropicPingEvent ping = new AnthropicPingEvent();
        ping.setType("ping");
        return ping;
    }

    /**
     * 处理工具调用
     */
    private Object processToolCalls(List<ToolCall> toolCalls, AnthropicEndpointSessionState state) {
        for (ToolCall toolCall : toolCalls) {
            // 如果是工具调用的开始（有ID和名称）
            if (toolCall.getId() != null && toolCall.getFunction() != null && toolCall.getFunction().getName() != null) {
                // 按照Anthropic规范，tool_use块应该在thinking和text块之后（索引2）
                int contentIndex = state.isToolUseStarted() ? 
                    state.getNextContentBlockIndex().getAndIncrement() : 
                    (state.isThinkingStarted() ? (state.isTextStarted() ? 2 : 1) : (state.isTextStarted() ? 1 : 0));
                
                state.setToolUseStarted(true);
                state.setToolUseBlockCount(state.getToolUseBlockCount() + 1);
                
                // 记录工具调用索引映射
                state.getToolCallIndices().put(toolCall.getIndex(), contentIndex);

                // 创建ContentBlockStartEvent
                AnthropicContentBlockStartEvent blockStart = new AnthropicContentBlockStartEvent();
                blockStart.setType("content_block_start");
                blockStart.setIndex(contentIndex);

                // 创建工具使用内容块
                AnthropicToolUseContent contentBlock = new AnthropicToolUseContent();
                contentBlock.setType("tool_use");
                contentBlock.setId(toolCall.getId());
                contentBlock.setName(toolCall.getFunction().getName());
                // 注意：初始时没有输入参数

                blockStart.setContentBlock(contentBlock);

                // 记录内容块状态
                BlockState blockState = new BlockState();
                blockState.setType(BlockType.TOOL_USE);
                blockState.setIndex(contentIndex);
                blockState.setStarted(true);
                blockState.setStopped(false);
                state.getBlockStates().put(contentIndex, blockState);

                return blockStart;
            }
            // 如果是工具调用的增量（只有参数增量）
            else if (toolCall.getFunction() != null && toolCall.getFunction().getArguments() != null) {
                // 获取对应的content block索引
                Integer contentBlockIndex = state.getToolCallIndices().get(toolCall.getIndex());
                if (contentBlockIndex != null) {
                    // 创建ContentBlockDeltaEvent
                    AnthropicContentBlockDeltaEvent blockDelta = new AnthropicContentBlockDeltaEvent();
                    blockDelta.setType("content_block_delta");
                    blockDelta.setIndex(contentBlockIndex);

                    AnthropicDelta delta = new AnthropicDelta();
                    delta.setType("input_json_delta");
                    delta.setPartialJson(toolCall.getFunction().getArguments());

                    blockDelta.setDelta(delta);
                    return blockDelta;
                }
            }
        }

        return null;
    }

    /**
     * 处理文本内容
     */
    private Object processContent(List<OpenAIContent> contents, AnthropicEndpointSessionState state) {
        for (OpenAIContent content : contents) {
            if (content instanceof OpenAITextContent textContent) {
                // 查找文本内容块状态
                BlockState textBlockState = findBlockStateByType(state, BlockType.TEXT);

                if (textBlockState == null || !textBlockState.isStarted()) {
                    // 按照Anthropic规范，text块应该在thinking块之后（索引1）
                    int contentIndex = state.isTextStarted() ? 
                        state.getNextContentBlockIndex().getAndIncrement() : (state.isThinkingStarted() ? 1 : 0);
                    
                    state.setTextStarted(true);
                    state.setTextBlockCount(state.getTextBlockCount() + 1);

                    // 创建ContentBlockStartEvent
                    AnthropicContentBlockStartEvent blockStart = new AnthropicContentBlockStartEvent();
                    blockStart.setType("content_block_start");

                    // 创建文本内容块
                    AnthropicTextContent contentBlock = new AnthropicTextContent();
                    contentBlock.setType("text");
                    contentBlock.setText("");

                    blockStart.setIndex(contentIndex);
                    blockStart.setContentBlock(contentBlock);

                    // 记录内容块状态
                    BlockState blockState = new BlockState();
                    blockState.setType(BlockType.TEXT);
                    blockState.setIndex(contentIndex);
                    blockState.setStarted(true);
                    blockState.setStopped(false);
                    state.getBlockStates().put(contentIndex, blockState);

                    return blockStart;
                } else {
                    // 创建ContentBlockDeltaEvent
                    AnthropicContentBlockDeltaEvent blockDelta = new AnthropicContentBlockDeltaEvent();
                    blockDelta.setType("content_block_delta");
                    blockDelta.setIndex(textBlockState.getIndex());

                    AnthropicDelta delta = new AnthropicDelta();
                    delta.setType("text_delta");
                    delta.setText(textContent.getText());

                    blockDelta.setDelta(delta);
                    return blockDelta;
                }
            }
        }

        return null;
    }

    /**
     * 处理推理内容
     */
    private Object processReasoningContent(String reasoningContent, AnthropicEndpointSessionState state) {
        // 查找推理内容块状态
        BlockState thinkingBlockState = findBlockStateByType(state, BlockType.THINKING);

        if (thinkingBlockState == null || !thinkingBlockState.isStarted()) {
            // 创建一个事件列表，同时包含start和delta事件
            List<Object> reasoningEvents = new ArrayList<>();
            
            // 按照Anthropic规范，thinking块应该是第一个内容块（索引0）
            int contentIndex = state.isThinkingStarted() ? 
                state.getNextContentBlockIndex().getAndIncrement() : 0;
            
            state.setThinkingStarted(true);
            state.setThinkingBlockCount(state.getThinkingBlockCount() + 1);

            // 创建ContentBlockStartEvent
            AnthropicContentBlockStartEvent blockStart = new AnthropicContentBlockStartEvent();
            blockStart.setType("content_block_start");

            // 创建推理内容块
            AnthropicThinkingContent contentBlock = new AnthropicThinkingContent();
            contentBlock.setType("thinking");
            contentBlock.setThinking("");

            blockStart.setIndex(contentIndex);
            blockStart.setContentBlock(contentBlock);
            reasoningEvents.add(blockStart);

            // 创建对应的thinking_delta事件
            AnthropicContentBlockDeltaEvent blockDelta = new AnthropicContentBlockDeltaEvent();
            blockDelta.setType("content_block_delta");
            blockDelta.setIndex(contentIndex);

            AnthropicDelta delta = new AnthropicDelta();
            delta.setType("thinking_delta");
            delta.setThinking(reasoningContent);

            blockDelta.setDelta(delta);
            reasoningEvents.add(blockDelta);

            // 记录内容块状态
            BlockState blockState = new BlockState();
            blockState.setType(BlockType.THINKING);
            blockState.setIndex(contentIndex);
            blockState.setStarted(true);
            blockState.setStopped(false);
            state.getBlockStates().put(contentIndex, blockState);

            return reasoningEvents; // 返回事件列表
        } else {
            // 创建ContentBlockDeltaEvent
            AnthropicContentBlockDeltaEvent blockDelta = new AnthropicContentBlockDeltaEvent();
            blockDelta.setType("content_block_delta");
            blockDelta.setIndex(thinkingBlockState.getIndex());

            AnthropicDelta delta = new AnthropicDelta();
            delta.setType("thinking_delta");
            delta.setThinking(reasoningContent);

            blockDelta.setDelta(delta);
            return blockDelta;
        }
    }

    /**
     * 将OpenAI的完成原因转换为Anthropic的停止原因
     */
    private String convertFinishReasonToStopReason(String finishReason) {
        if (finishReason == null) {
            return null;
        }

        return switch (finishReason) {
            case "stop" -> "end_turn";
            case "length" -> "max_tokens";
            case "tool_calls" -> "tool_use";
            case "content_filter" -> "end_turn";
            default -> "end_turn";
        };
    }

    /**
     * 为所有已启动但未停止的内容块生成content_block_stop事件
     */
    private void generateStopEventsForAllBlocks(AnthropicEndpointSessionState state, List<Object> events) {
        for (BlockState blockState : state.getBlockStates().values()) {
            if (blockState.isStarted() && !blockState.isStopped()) {
                // 创建ContentBlockStopEvent
                AnthropicContentBlockStopEvent blockStop = new AnthropicContentBlockStopEvent();
                blockStop.setType("content_block_stop");
                blockStop.setIndex(blockState.getIndex());
                events.add(blockStop);

                // 标记为已停止
                blockState.setStopped(true);
            }
        }
    }

    /**
     * 根据类型查找内容块状态
     */
    private BlockState findBlockStateByType(AnthropicEndpointSessionState state, BlockType type) {
        for (BlockState blockState : state.getBlockStates().values()) {
            if (blockState.getType() == type) {
                return blockState;
            }
        }
        return null;
    }
    
    /**
     * 为指定类型的内容块生成停止事件
     */
    private Object generateContentBlockStopEvent(AnthropicEndpointSessionState state, BlockType type) {
        BlockState blockState = findBlockStateByType(state, type);
        if (blockState != null && blockState.isStarted() && !blockState.isStopped()) {
            AnthropicContentBlockStopEvent blockStop = new AnthropicContentBlockStopEvent();
            blockStop.setType("content_block_stop");
            blockStop.setIndex(blockState.getIndex());
            
            // 标记为已停止
            blockState.setStopped(true);
            return blockStop;
        }
        return null;
    }
    
    /**
     * 判断是否为最后一个推理内容片段
     */
    private boolean isLastReasoningChunk(UnifiedStreamChunk unifiedChunk) {
        // 如果当前片段包含非推理内容（如文本或工具调用），说明推理已完成
        if (unifiedChunk.getChoices() != null && !unifiedChunk.getChoices().isEmpty()) {
            Choice choice = unifiedChunk.getChoices().get(0);
            if (choice.getDelta() != null) {
                Delta delta = choice.getDelta();
                // 如果同时包含推理内容和其他内容，说明推理即将完成
                boolean hasNonReasoning = delta.getContent() != null || 
                                      (delta.getToolCalls() != null && !delta.getToolCalls().isEmpty());
                boolean hasReasoning = delta.getReasoningContent() != null;
                
                // 如果包含非推理内容，或者推理内容为空，说明推理完成
                return hasNonReasoning || (hasReasoning && delta.getReasoningContent().isEmpty());
            }
        }
        return false;
    }
    
    /**
     * 判断是否为最后一个工具调用片段
     */
    private boolean isLastToolCallChunk(List<ToolCall> toolCalls) {
        // 检查是否有完整的JSON参数
        for (ToolCall toolCall : toolCalls) {
            if (toolCall.getFunction() != null && toolCall.getFunction().getArguments() != null) {
                String args = toolCall.getFunction().getArguments();
                // 更严格的JSON完整性检查
                return isCompleteJson(args);
            }
        }
        return false;
    }
    
    /**
     * 检查JSON是否完整（包括括号匹配、字符串引号匹配等）
     */
    private boolean isCompleteJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = json.trim();
        
        // 必须以{开头，}结尾
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return false;
        }
        
        // 检查括号匹配和字符串引号匹配
        return isValidJsonStructure(trimmed);
    }
    
    /**
     * 验证JSON结构的有效性（包括括号和引号匹配）
     */
    private boolean isValidJsonStructure(String json) {
        int braceBalance = 0;
        int bracketBalance = 0;
        boolean inString = false;
        boolean escape = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escape) {
                escape = false;
                continue;
            }
            
            if (c == '\\') {
                escape = true;
                continue;
            }
            
            if (c == '"' && !escape) {
                inString = !inString;
                continue;
            }
            
            if (inString) {
                continue;
            }
            
            switch (c) {
                case '{':
                    braceBalance++;
                    break;
                case '}':
                    braceBalance--;
                    if (braceBalance < 0) return false;
                    break;
                case '[':
                    bracketBalance++;
                    break;
                case ']':
                    bracketBalance--;
                    if (bracketBalance < 0) return false;
                    break;
            }
        }
        
        return braceBalance == 0 && bracketBalance == 0 && !inString;
    }
    
    /**
     * 生成signature_delta事件
     */
    private Object generateSignatureDeltaEvent(AnthropicEndpointSessionState state) {
        BlockState thinkingBlockState = findBlockStateByType(state, BlockType.THINKING);
        if (thinkingBlockState != null && thinkingBlockState.isStarted() && !thinkingBlockState.isStopped()) {
            // 创建ContentBlockDeltaEvent
            AnthropicContentBlockDeltaEvent blockDelta = new AnthropicContentBlockDeltaEvent();
            blockDelta.setType("content_block_delta");
            blockDelta.setIndex(thinkingBlockState.getIndex());

            AnthropicDelta delta = new AnthropicDelta();
            delta.setType("signature_delta");
            // 生成一个模拟的签名，实际应用中可能需要真实的签名生成逻辑
            delta.setSignature("EqQBCgIYAhIM1gbcDa9GJwZA2b3hGgxBdjrkzLoky3dl1pkiMOYds...");

            blockDelta.setDelta(delta);
            return blockDelta;
        }
        return null;
    }

    /**
     * 验证事件序列的完整性
     */
    private void validateEventSequence(List<Object> events, AnthropicEndpointSessionState state) {
        // 检查是否有必需的message_start事件
        boolean hasMessageStart = events.stream()
            .anyMatch(event -> event instanceof AnthropicMessageStartEvent);
        
        if (!hasMessageStart && state.getMessageId() == null) {
            // 如果还没有message_start，添加到事件列表开头
            AnthropicMessageStartEvent messageStart = new AnthropicMessageStartEvent();
            messageStart.setType("message_start");
            
            AnthropicMessage message = new AnthropicMessage();
            message.setId("msg_" + System.currentTimeMillis());
            message.setType("message");
            message.setModel(state.getLastModel() != null ? state.getLastModel() : "claude-3-opus-20240229");
            
            messageStart.setMessage(message);
            events.add(0, messageStart);
        }
        
        // 检查是否所有已启动的内容块都有对应的stop事件
        for (BlockState blockState : state.getBlockStates().values()) {
            if (blockState.isStarted() && !blockState.isStopped()) {
                // 检查是否已有stop事件
                boolean hasStopEvent = events.stream()
                    .anyMatch(event -> event instanceof AnthropicContentBlockStopEvent stopEvent && 
                                   stopEvent.getIndex() == blockState.getIndex());
                
                if (!hasStopEvent) {
                    // 添加missing的stop事件
                    AnthropicContentBlockStopEvent stopEvent = new AnthropicContentBlockStopEvent();
                    stopEvent.setType("content_block_stop");
                    stopEvent.setIndex(blockState.getIndex());
                    events.add(stopEvent);
                    blockState.setStopped(true);
                }
            }
        }
    }

    /**
     * 判断是否应该停止文本内容块
     */
    private boolean shouldStopTextBlock(Delta delta) {
        // 当开始工具调用或推理内容时，文本内容应该停止
        return (delta.getToolCalls() != null && !delta.getToolCalls().isEmpty()) ||
               (delta.getReasoningContent() != null && !delta.getReasoningContent().isEmpty());
    }

    /**
     * 内容块类型枚举
     */
    enum BlockType {
        TEXT, THINKING, TOOL_USE
    }

    /**
     * 内容块状态
     */
    @Data
    private static class BlockState {
        private BlockType type;
        private Integer index;
        private boolean started;
        private boolean stopped;
    }

    /**
     * Anthropic端点会话状态
     */
    @Data
    private static class AnthropicEndpointSessionState {
        private String messageId;
        private String lastModel;
        private AtomicInteger nextContentBlockIndex = new AtomicInteger(0); // 下一个内容块索引
        private final Map<Integer, BlockState> blockStates = new ConcurrentHashMap<>(); // 内容块状态
        private final Map<Integer, Integer> toolCallIndices = new ConcurrentHashMap<>(); // 工具调用索引到内容块索引的映射
        private boolean messageCompleted = false; // 消息是否完成
        
        // 添加内容块类型计数器，确保按正确顺序生成索引
        private int thinkingBlockCount = 0;
        private int textBlockCount = 0;
        private int toolUseBlockCount = 0;
        
        // 标记哪些类型的内容块已经启动
        private boolean thinkingStarted = false;
        private boolean textStarted = false;
        private boolean toolUseStarted = false;
    }
}
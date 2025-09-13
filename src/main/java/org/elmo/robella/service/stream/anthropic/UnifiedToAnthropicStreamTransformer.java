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
import org.elmo.robella.model.openai.core.Usage;
import org.elmo.robella.model.openai.stream.Delta;
import org.elmo.robella.model.openai.content.OpenAIContent;
import org.elmo.robella.model.openai.content.OpenAITextContent;
import org.elmo.robella.model.openai.tool.ToolCall;
import org.elmo.robella.service.stream.UnifiedToEndpointStreamTransformer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 统一格式到Anthropic端点格式的转换器
 * 采用分层设计：事件识别 → 状态管理 → 数据转换 → 输出生成
 */
@Slf4j
@Component
public class UnifiedToAnthropicStreamTransformer implements UnifiedToEndpointStreamTransformer<Object> {

    // 会话状态存储
    private final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();

    @Override
    public Flux<Object> transform(Flux<UnifiedStreamChunk> unifiedStream, String sessionId) {
        // 初始化会话状态
        sessionStates.putIfAbsent(sessionId, new SessionState());

        return unifiedStream.flatMap(unifiedChunk -> {
            SessionState state = sessionStates.get(sessionId);
            return processUnifiedChunk(unifiedChunk, state);
        }).doFinally(signalType -> {
            // 清理会话状态
            sessionStates.remove(sessionId);
        });
    }

    /**
     * 处理统一格式的chunk，返回Anthropic事件流
     */
    private Flux<Object> processUnifiedChunk(UnifiedStreamChunk unifiedChunk, SessionState state) {
        List<Object> events = new ArrayList<>();

        // 1. 处理消息开始事件
        if (state.getMessageId() == null && unifiedChunk.getId() != null) {
            events.add(createMessageStartEvent(unifiedChunk, state));
        }

        // 2. 处理内容变化和完成信号
        if (unifiedChunk.getChoices() != null && !unifiedChunk.getChoices().isEmpty()) {
            Choice choice = unifiedChunk.getChoices().get(0);
            if (choice.getDelta() != null) {
                events.addAll(processDeltaEvents(choice.getDelta(), state, unifiedChunk));
            }

            // 3. 标记完成信号并进入收尾阶段（仅标记，不立即发事件）
            if (choice.getFinishReason() != null) {
                String stopReason = convertFinishReasonToStopReason(choice.getFinishReason());
                state.setStopReason(stopReason);
                // 第一次进入收尾阶段时，停止所有活跃内容块
                if (state.getPhase() != MessagePhase.FINISHING && state.getPhase() != MessagePhase.FINISHED) {
                    events.addAll(state.stopAllActiveBlocks());
                }
                state.setPhase(MessagePhase.FINISHING);
                log.debug("[AnthropicTransformer] 收到finishReason: {} -> stopReason={}", choice.getFinishReason(), stopReason);
            }
        }

        // 4. 聚合并处理usage与收尾
        if (unifiedChunk.getUsage() != null) {
            state.setUsage(unifiedChunk.getUsage());
            log.debug("[AnthropicTransformer] 收到usage信息: {}", unifiedChunk.getUsage());
        }

        // 5. 尝试输出 message_delta（stopReason / usage）以及 message_stop
        events.addAll(tryFlushEndMeta(state));

        // 如果没有事件，返回空的Flux
        if (events.isEmpty()) {
            return Flux.empty();
        }

        // 记录发出的事件
        for (Object event : events) {
            logEvent(event);
        }

        return Flux.fromIterable(events);
    }

    /**
     * 创建消息开始事件
     */
    private AnthropicMessageStartEvent createMessageStartEvent(UnifiedStreamChunk unifiedChunk, SessionState state) {
        state.setMessageId(unifiedChunk.getId());
        state.setModel(unifiedChunk.getModel());

        // 创建MessageStartEvent
        AnthropicMessageStartEvent messageStart = new AnthropicMessageStartEvent();
        messageStart.setType("message_start");

        AnthropicMessage message = new AnthropicMessage();
        message.setId(unifiedChunk.getId());
        message.setType("message");
        message.setModel(unifiedChunk.getModel());
        message.setRole("assistant");

        AnthropicUsage usage = new AnthropicUsage();
        usage.setInputTokens(0);
        usage.setOutputTokens(1);
        message.setUsage(usage);
        messageStart.setMessage(message);
        return messageStart;
    }

    /**
     * 处理Delta事件
     */
    private List<Object> processDeltaEvents(Delta delta, SessionState state, UnifiedStreamChunk unifiedChunk) {
        List<Object> events = new ArrayList<>();

        // 检查是否需要结束文本块（仅在开始工具调用时）
        List<BlockState> activeTextBlocks = state.getBlockStates(BlockType.TEXT);
        boolean hasActiveTextBlock = !activeTextBlocks.isEmpty();

        // 如果存在活跃文本块且当前开始工具调用，说明文本过程已结束
        if (hasActiveTextBlock && delta.getToolCalls() != null && !delta.getToolCalls().isEmpty()) {
            events.addAll(completeTextProcess(state));
        }

        // 检查是否需要结束推理过程
        List<BlockState> activeThinkingBlocks = state.getBlockStates(BlockType.THINKING);
        boolean hasActiveThinkingBlock = !activeThinkingBlocks.isEmpty();
        boolean hasNonReasoningContent = (delta.getContent() != null && !delta.getContent().isEmpty()) ||
                (delta.getToolCalls() != null && !delta.getToolCalls().isEmpty());

        // 如果存在活跃推理块且当前没有推理内容但有非推理内容，说明推理过程已结束
        if (hasActiveThinkingBlock && delta.getReasoningContent() == null && hasNonReasoningContent) {
            events.addAll(completeReasoningProcess(state));
        }

        // 处理推理内容
        if (delta.getReasoningContent() != null) {
            events.addAll(handleReasoningContent(delta.getReasoningContent(), state, unifiedChunk));
        } else if (delta.getToolCalls() != null && !delta.getToolCalls().isEmpty()) {
            // 优先处理工具调用，避免同时存在的空文本内容干扰
            events.addAll(handleToolCalls(delta.getToolCalls(), state));
        } else if (delta.getContent() != null && !delta.getContent().isEmpty()) {
            // 只有在没有工具调用时才处理文本内容，并且需要检查文本是否有实际内容
            boolean hasActualTextContent = delta.getContent().stream()
                    .filter(content -> content instanceof OpenAITextContent)
                    .map(content -> ((OpenAITextContent) content).getText())
                    .anyMatch(text -> text != null && !text.trim().isEmpty());
            
            if (hasActualTextContent) {
                events.addAll(handleTextContent(delta.getContent(), state));
            }
        }

        return events;
    }


    // 创建单条 MessageDelta（可同时包含 stopReason 与 usage）
    private List<Object> createMessageDelta(String stopReason, Usage usage) {
        List<Object> events = new ArrayList<>();

        AnthropicMessageDeltaEvent messageDelta = new AnthropicMessageDeltaEvent();
        messageDelta.setType("message_delta");

        AnthropicDelta delta = new AnthropicDelta();
        if (stopReason != null) {
            delta.setStopReason(stopReason);
        }
        AnthropicUsage usageInfo = new AnthropicUsage();
        if (usage != null) {

            usageInfo.setInputTokens(usage.getPromptTokens());
            usageInfo.setOutputTokens(usage.getCompletionTokens());
            messageDelta.setUsage(usageInfo);
        } else {
            // 临时设置，防止某些客户端硬是要读取usage
            usageInfo.setInputTokens(0);
            usageInfo.setOutputTokens(1);
            messageDelta.setUsage(usageInfo);
        }

        messageDelta.setDelta(delta);
        events.add(messageDelta);
        return events;
    }

    /**
     * 创建MessageStop事件
     */
    private AnthropicMessageStopEvent createMessageStopEvent() {
        AnthropicMessageStopEvent messageStop = new AnthropicMessageStopEvent();
        messageStop.setType("message_stop");
        return messageStop;
    }

    /**
     * 处理文本内容
     */
    private List<Object> handleTextContent(List<OpenAIContent> contents, SessionState state) {
        List<Object> events = new ArrayList<>();

        for (OpenAIContent content : contents) {
            if (content instanceof OpenAITextContent textContent) {
                List<BlockState> blockStates = state.getBlockStates(BlockType.TEXT);

                if (blockStates.isEmpty()) {
                    // 创建新的文本块
                    events.add(createTextBlock(state));
                    // 重新获取刚创建的块状态
                    blockStates = state.getBlockStates(BlockType.TEXT);
                }

                if (!blockStates.isEmpty()) {
                    // 使用第一个文本块添加增量
                    BlockState blockState = blockStates.get(0);
                    events.add(createTextDelta(blockState.getIndex(), textContent.getText()));
                }
                break;
            }
        }

        return events;
    }

    /**
     * 创建文本块
     */
    private AnthropicContentBlockStartEvent createTextBlock(SessionState state) {
        int index = state.getNextContentIndex();

        // 创建Start事件
        AnthropicContentBlockStartEvent blockStart = new AnthropicContentBlockStartEvent();
        blockStart.setType("content_block_start");
        blockStart.setIndex(index);

        AnthropicTextContent contentBlock = new AnthropicTextContent();
        contentBlock.setType("text");
        contentBlock.setText("");

        blockStart.setContentBlock(contentBlock);

        // 记录块状态
        state.addBlockState(index, BlockType.TEXT);
        return blockStart;
    }

    /**
     * 创建文本增量事件
     */
    private Object createTextDelta(int index, String text) {
        AnthropicContentBlockDeltaEvent blockDelta = new AnthropicContentBlockDeltaEvent();
        blockDelta.setType("content_block_delta");
        blockDelta.setIndex(index);

        AnthropicDelta delta = new AnthropicDelta();
        delta.setType("text_delta");
        delta.setText(text);

        blockDelta.setDelta(delta);
        return blockDelta;
    }

    /**
     * 处理推理内容
     */
    private List<Object> handleReasoningContent(String reasoningContent, SessionState state, UnifiedStreamChunk unifiedChunk) {
        List<Object> events = new ArrayList<>();

        // 检查是否存在活跃的推理块
        List<BlockState> blockStates = state.getBlockStates(BlockType.THINKING);

        if (blockStates.isEmpty()) {
            // 创建新的推理块
            events.add(createThinkingBlock(state));
            // 重新获取刚创建的块状态
            blockStates = state.getBlockStates(BlockType.THINKING);
        }

        if (!blockStates.isEmpty()) {
            BlockState blockState = blockStates.get(0);
            // 添加推理增量
            events.add(createThinkingDelta(blockState.getIndex(), reasoningContent));
        }

        return events;
    }

    /**
     * 完成文本过程
     * 当文本过程结束时（开始推理或工具调用），主动完成文本块
     */
    private List<Object> completeTextProcess(SessionState state) {
        List<Object> events = new ArrayList<>();

        List<BlockState> activeTextBlocks = state.getBlockStates(BlockType.TEXT);
        for (BlockState blockState : activeTextBlocks) {
            // 停止文本块
            events.add(state.stopBlock(blockState.getIndex()));
        }

        return events;
    }

    /**
     * 完成推理过程
     * 当推理过程结束时（接收到非推理内容），主动完成推理块
     */
    private List<Object> completeReasoningProcess(SessionState state) {
        List<Object> events = new ArrayList<>();

        List<BlockState> activeThinkingBlocks = state.getBlockStates(BlockType.THINKING);
        for (BlockState blockState : activeThinkingBlocks) {
            // 添加签名增量
            events.add(createSignatureDelta(blockState.getIndex()));
            // 停止推理块
            events.add(state.stopBlock(blockState.getIndex()));
        }

        return events;
    }

    /**
     * 创建推理块
     */
    private AnthropicContentBlockStartEvent createThinkingBlock(SessionState state) {
        int index = state.getNextContentIndex();

        // 创建Start事件
        AnthropicContentBlockStartEvent blockStart = new AnthropicContentBlockStartEvent();
        blockStart.setType("content_block_start");
        blockStart.setIndex(index);

        AnthropicThinkingContent contentBlock = new AnthropicThinkingContent();
        contentBlock.setType("thinking");
        contentBlock.setThinking("");

        blockStart.setContentBlock(contentBlock);

        // 记录块状态
        state.addBlockState(index, BlockType.THINKING);
        return blockStart;
    }

    /**
     * 创建推理增量事件
     */
    private Object createThinkingDelta(int index, String thinking) {
        AnthropicContentBlockDeltaEvent blockDelta = new AnthropicContentBlockDeltaEvent();
        blockDelta.setType("content_block_delta");
        blockDelta.setIndex(index);

        AnthropicDelta delta = new AnthropicDelta();
        delta.setType("thinking_delta");
        delta.setThinking(thinking);

        blockDelta.setDelta(delta);
        return blockDelta;
    }

    /**
     * 创建签名增量事件
     */
    private Object createSignatureDelta(int index) {
        AnthropicContentBlockDeltaEvent blockDelta = new AnthropicContentBlockDeltaEvent();
        blockDelta.setType("content_block_delta");
        blockDelta.setIndex(index);

        AnthropicDelta delta = new AnthropicDelta();
        delta.setType("signature_delta");
        delta.setSignature("EqQBCgIYAhIM1gbcDa9GJwZA2b3hGgxBdjrkzLoky3dl1pkiMOYds...");

        blockDelta.setDelta(delta);
        return blockDelta;
    }

    /**
     * 处理工具调用
     */
    private List<Object> handleToolCalls(List<ToolCall> toolCalls, SessionState state) {
        List<Object> events = new ArrayList<>();

        for (ToolCall toolCall : toolCalls) {
            if (toolCall.getId() != null && toolCall.getFunction() != null && toolCall.getFunction().getName() != null) {
                // 工具调用开始 - 为每个工具调用创建独立的块
                events.add(createToolUseBlock(toolCall, state));
            } else if (toolCall.getFunction() != null && toolCall.getFunction().getArguments() != null) {
                // 工具调用参数增量 - 根据工具调用ID找到对应的块
                Object delta = createToolUseDelta(toolCall, state);
                if (delta != null) {
                    events.add(delta);
                } else {
                    log.debug("[AnthropicTransformer] 跳过tool_use增量，未找到匹配块或ID缺失");
                }
            }
        }

        // 检查是否有工具调用需要停止
        for (ToolCall toolCall : toolCalls) {
            if (isToolCallComplete(toolCall)) {
                BlockState blockState = null;
                // 优先通过 ID 查找
                if (toolCall.getId() != null) {
                    blockState = state.getBlockStateByToolCallId(toolCall.getId());
                }
                // 若 ID 缺失或未命中，尝试使用 index 映射
                if (blockState == null && toolCall.getIndex() != null) {
                    Integer contentIdx = state.getToolCallIndexMapping(toolCall.getIndex());
                    if (contentIdx != null) {
                        blockState = state.getActiveBlockByIndex(contentIdx);
                    }
                }
                // 若仍未命中且当前只有一个活跃的 tool_use 块，则回退到该块
                if (blockState == null) {
                    List<BlockState> activeToolBlocks = state.getBlockStates(BlockType.TOOL_USE);
                    if (activeToolBlocks.size() == 1) {
                        blockState = activeToolBlocks.get(0);
                    }
                }
                if (blockState != null) {
                    events.add(state.stopBlock(blockState.getIndex()));
                }
            }
        }

        return events;
    }

    /**
     * 创建工具使用块
     */
    private AnthropicContentBlockStartEvent createToolUseBlock(ToolCall toolCall, SessionState state) {
        int index = state.getNextContentIndex();

        // 创建Start事件
        AnthropicContentBlockStartEvent blockStart = new AnthropicContentBlockStartEvent();
        blockStart.setType("content_block_start");
        blockStart.setIndex(index);

        AnthropicToolUseContent contentBlock = new AnthropicToolUseContent();
        contentBlock.setType("tool_use");
        contentBlock.setId(toolCall.getId());
        contentBlock.setName(toolCall.getFunction().getName());

        blockStart.setContentBlock(contentBlock);

        // 记录块状态和工具调用映射
        state.addBlockState(index, BlockType.TOOL_USE, toolCall.getId());
        // 映射：id -> contentIndex
        if (toolCall.getId() != null) {
            state.addToolCallMapping(toolCall.getId(), index);
        }
        // 额外映射：toolCall.index -> contentIndex（用于后续增量缺失id的情况）
        if (toolCall.getIndex() != null) {
            state.addToolCallIndexMapping(toolCall.getIndex(), index);
        }

        return blockStart;
    }

    /**
     * 创建工具使用增量事件
     */
    private Object createToolUseDelta(ToolCall toolCall, SessionState state) {
        Integer contentBlockIndex = null;
        // 优先使用 id 映射
        if (toolCall.getId() != null) {
            contentBlockIndex = state.getToolCallMapping(toolCall.getId());
        }
        // 若 id 缺失或未映射，尝试使用 index 映射
        if (contentBlockIndex == null && toolCall.getIndex() != null) {
            contentBlockIndex = state.getToolCallIndexMapping(toolCall.getIndex());
        }
        // 若仍未命中且当前仅有一个活跃 tool_use 块，则回退到该块
        if (contentBlockIndex == null) {
            List<BlockState> activeToolBlocks = state.getBlockStates(BlockType.TOOL_USE);
            if (activeToolBlocks.size() == 1) {
                contentBlockIndex = activeToolBlocks.get(0).getIndex();
            }
        }
        if (contentBlockIndex == null) {
            return null;
        }

        AnthropicContentBlockDeltaEvent blockDelta = new AnthropicContentBlockDeltaEvent();
        blockDelta.setType("content_block_delta");
        blockDelta.setIndex(contentBlockIndex);

        AnthropicDelta delta = new AnthropicDelta();
        delta.setType("input_json_delta");
        delta.setPartialJson(toolCall.getFunction().getArguments());

        blockDelta.setDelta(delta);
        return blockDelta;
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
     * 判断单个工具调用是否完成
     */
    private boolean isToolCallComplete(ToolCall toolCall) {
        if (toolCall.getFunction() != null && toolCall.getFunction().getArguments() != null) {
            String args = toolCall.getFunction().getArguments();
            return isCompleteJson(args);
        }
        return false;
    }

    /**
     * 检查JSON是否完整
     */
    private boolean isCompleteJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }

        String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return false;
        }

        return isValidJsonStructure(trimmed);
    }

    /**
     * 验证JSON结构的有效性
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
        /**
         * 内容块类型
         */
        private BlockType type;
        /**
         * 内容块索引
         */
        private Integer index;
        /**
         * 是否活跃（已开始未停止）
         */
        private boolean active;
        /**
         * 关联的工具调用ID（用于tool_use类型）
         */
        private String toolCallId;
    }

    /**
     * 简化的会话状态
     */
    @Data
    private static class SessionState {
        /**
         * 消息ID
         */
        private String messageId;
        /**
         * 模型名称
         */
        private String model;
        /**
         * 下一个内容块的索引计数器
         */
        private final AtomicInteger nextContentIndex = new AtomicInteger(0);
        /**
         * 活跃内容块的状态映射（支持多个相同类型的块）
         */
        private final Map<Integer, BlockState> activeBlocks = new ConcurrentHashMap<>();
        /**
         * 工具调用ID到内容块索引的映射
         */
        private final Map<String, Integer> toolCallMappings = new ConcurrentHashMap<>();
    /**
     * 工具调用索引到内容块索引的映射（当上游增量缺失 id，仅有 index 时使用）
     */
    private final Map<Integer, Integer> toolCallIndexMappings = new ConcurrentHashMap<>();
        /**
         * 完成原因（用于处理finish_reason和usage分离的情况）
         */
        private String stopReason;
        /**
         * 使用量信息（用于处理finish_reason和usage分离的情况）
         */
        private Usage usage;
        /**
         * 消息阶段
         */
        private MessagePhase phase = MessagePhase.INIT;
        /**
         * 已发送 stopReason 增量
         */
        private boolean stopReasonEmitted;
        /**
         * 已发送 usage 增量
         */
        private boolean usageEmitted;
        /**
         * 是否已经发送了message_stop事件
         */
        private boolean messageStopSent;

        /**
         * 获取下一个内容块索引
         */
        public int getNextContentIndex() {
            return nextContentIndex.getAndIncrement();
        }

        /**
         * 添加内容块状态
         */
        public void addBlockState(int index, BlockType type) {
            addBlockState(index, type, null);
        }

        /**
         * 添加内容块状态（带工具调用ID）
         */
        public void addBlockState(int index, BlockType type, String toolCallId) {
            BlockState blockState = new BlockState();
            blockState.setType(type);
            blockState.setIndex(index);
            blockState.setActive(true);
            blockState.setToolCallId(toolCallId);
            activeBlocks.put(index, blockState);
        }

        /**
         * 获取指定类型的内容块状态列表
         */
        public List<BlockState> getBlockStates(BlockType type) {
            return activeBlocks.values().stream()
                    .filter(state -> state.getType() == type && state.isActive())
                    .collect(Collectors.toList());
        }

        /**
         * 根据工具调用ID获取内容块状态
         */
        public BlockState getBlockStateByToolCallId(String toolCallId) {
            return activeBlocks.values().stream()
                    .filter(state -> toolCallId.equals(state.getToolCallId()) && state.isActive())
                    .findFirst()
                    .orElse(null);
        }

        /**
         * 停止指定索引的内容块
         */
        public AnthropicContentBlockStopEvent stopBlock(int index) {
            BlockState blockState = activeBlocks.get(index);
            if (blockState != null && blockState.isActive()) {
                // 将内容块从活跃状态移除
                blockState.setActive(false);
                activeBlocks.remove(index);

                AnthropicContentBlockStopEvent stopEvent = new AnthropicContentBlockStopEvent();
                stopEvent.setType("content_block_stop");
                stopEvent.setIndex(index);
                return stopEvent;
            }
            return null;
        }

        /**
         * 停止所有活跃的内容块
         */
        public List<Object> stopAllActiveBlocks() {
            List<Object> stopEvents = new ArrayList<>();
            // 创建副本以避免并发修改问题
            List<Integer> indices = new ArrayList<>(activeBlocks.keySet());
            for (Integer index : indices) {
                AnthropicContentBlockStopEvent stopEvent = stopBlock(index);
                if (stopEvent != null) {
                    stopEvents.add(stopEvent);
                }
            }
            return stopEvents;
        }

        /**
         * 添加工具调用映射
         */
        public void addToolCallMapping(String toolCallId, int contentBlockIndex) {
            toolCallMappings.put(toolCallId, contentBlockIndex);
        }

        /**
         * 获取工具调用映射
         */
        public Integer getToolCallMapping(String toolCallId) {
            return toolCallMappings.get(toolCallId);
        }

        /**
         * 添加工具调用索引映射
         */
        public void addToolCallIndexMapping(Integer toolCallIndex, int contentBlockIndex) {
            if (toolCallIndex != null) {
                toolCallIndexMappings.put(toolCallIndex, contentBlockIndex);
            }
        }

        /**
         * 获取工具调用索引映射
         */
        public Integer getToolCallIndexMapping(Integer toolCallIndex) {
            if (toolCallIndex == null) return null;
            return toolCallIndexMappings.get(toolCallIndex);
        }

        /**
         * 通过内容块索引获取活跃块（仅当块仍活跃时返回）
         */
        public BlockState getActiveBlockByIndex(Integer index) {
            if (index == null) return null;
            BlockState st = activeBlocks.get(index);
            return (st != null && st.isActive()) ? st : null;
        }

        // 便捷访问器
        public MessagePhase getPhase() {
            return phase;
        }

        public void setPhase(MessagePhase phase) {
            this.phase = phase;
        }

        public String getStopReason() {
            return stopReason;
        }

        public void setStopReason(String stopReason) {
            this.stopReason = stopReason;
        }

        public org.elmo.robella.model.openai.core.Usage getUsage() {
            return usage;
        }

        public void setUsage(org.elmo.robella.model.openai.core.Usage usage) {
            this.usage = usage;
        }

        public boolean isStopReasonEmitted() {
            return stopReasonEmitted;
        }

        public void setStopReasonEmitted(boolean v) {
            this.stopReasonEmitted = v;
        }

        public boolean isUsageEmitted() {
            return usageEmitted;
        }

        public void setUsageEmitted(boolean v) {
            this.usageEmitted = v;
        }
    }

    /**
     * 消息阶段
     */
    private enum MessagePhase {
        INIT, STREAMING, FINISHING, FINISHED
    }

    /**
     * 将 stopReason / usage 聚合输出为 message_delta，并在齐备时发送 message_stop
     */
    private List<Object> tryFlushEndMeta(SessionState state) {
        List<Object> out = new ArrayList<>();

        // 如果已结束，直接返回
        if (state.getPhase() == MessagePhase.FINISHED) {
            return out;
        }

        boolean hasStopReason = state.getStopReason() != null;
        boolean hasUsage = state.getUsage() != null;

        // 两者同时具备且都未发出 -> 合并为一条 message_delta
        if (hasStopReason && hasUsage && !state.isStopReasonEmitted() && !state.isUsageEmitted()) {
            out.addAll(createMessageDelta(state.getStopReason(), state.getUsage()));
            state.setStopReasonEmitted(true);
            state.setUsageEmitted(true);
        } else {
            // 按需补发单项
            if (hasStopReason && !state.isStopReasonEmitted()) {
                out.addAll(createMessageDelta(state.getStopReason(), null));
                state.setStopReasonEmitted(true);
            }
            if (hasUsage && !state.isUsageEmitted()) {
                out.addAll(createMessageDelta(null, state.getUsage()));
                state.setUsageEmitted(true);
            }
        }

        // 当两者都已发出时，收尾 message_stop
        if (state.isStopReasonEmitted() && state.isUsageEmitted() && !state.messageStopSent) {
            out.add(createMessageStopEvent());
            state.messageStopSent = true;
            state.setPhase(MessagePhase.FINISHED);
            log.debug("[AnthropicTransformer] 发送message_stop事件");
        }

        return out;
    }

    /**
     * 记录发出的事件
     */
    private void logEvent(Object event) {
        if (event instanceof AnthropicMessageStartEvent) {
            log.debug("[AnthropicTransformer] 发出事件: message_start");
        } else if (event instanceof AnthropicContentBlockStartEvent blockStart) {
            String blockType = "unknown";
            if (blockStart.getContentBlock() instanceof AnthropicTextContent) {
                blockType = "text";
            } else if (blockStart.getContentBlock() instanceof AnthropicToolUseContent) {
                blockType = "tool_use";
            } else if (blockStart.getContentBlock() instanceof AnthropicThinkingContent) {
                blockType = "thinking";
            }
            log.debug("[AnthropicTransformer] 发出事件: content_block_start (index={}, type={})",
                    blockStart.getIndex(), blockType);
        } else if (event instanceof AnthropicContentBlockDeltaEvent blockDelta) {
            String deltaType = "unknown";
            if (blockDelta.getDelta() != null) {
                if (blockDelta.getDelta().isTextDelta()) {
                    deltaType = "text";
                } else if (blockDelta.getDelta().isInputJsonDelta()) {
                    deltaType = "input_json";
                } else if (blockDelta.getDelta().isThinkingDelta()) {
                    deltaType = "thinking";
                } else if ("signature_delta".equals(blockDelta.getDelta().getType())) {
                    deltaType = "signature";
                }
            }
            log.debug("[AnthropicTransformer] 发出事件: content_block_delta (index={}, deltaType={})",
                    blockDelta.getIndex(), deltaType);
        } else if (event instanceof AnthropicContentBlockStopEvent blockStop) {
            log.debug("[AnthropicTransformer] 发出事件: content_block_stop (index={})",
                    blockStop.getIndex());
        } else if (event instanceof AnthropicMessageDeltaEvent messageDelta) {
            String deltaInfo = "";
            if (messageDelta.getDelta() != null) {
                if (messageDelta.getDelta().getStopReason() != null) {
                    deltaInfo += "stopReason=" + messageDelta.getDelta().getStopReason();
                }
                if (messageDelta.getUsage() != null) {
                    if (!deltaInfo.isEmpty()) deltaInfo += ", ";
                    deltaInfo += "usage=" + messageDelta.getUsage();
                }
            }
            log.debug("[AnthropicTransformer] 发出事件: message_delta ({})", deltaInfo);
        } else if (event instanceof AnthropicMessageStopEvent) {
            log.debug("[AnthropicTransformer] 发出事件: message_stop");
        } else if (event instanceof AnthropicPingEvent) {
            log.debug("[AnthropicTransformer] 发出事件: ping");
        } else if (event instanceof AnthropicErrorEvent errorEvent) {
            log.debug("[AnthropicTransformer] 发出事件: error (type={})",
                    errorEvent.getError() != null ? errorEvent.getError().getType() : "unknown");
        } else {
            log.debug("[AnthropicTransformer] 发出事件: {} (未知类型)", event.getClass().getSimpleName());
        }
    }
}
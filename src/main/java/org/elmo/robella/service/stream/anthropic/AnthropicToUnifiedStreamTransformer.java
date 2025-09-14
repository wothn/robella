package org.elmo.robella.service.stream.anthropic;

import lombok.Data;
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
import org.elmo.robella.model.anthropic.content.AnthropicTextContent;
import org.elmo.robella.model.anthropic.content.AnthropicThinkingContent;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.openai.core.Choice;
import org.elmo.robella.model.openai.core.Usage;
import org.elmo.robella.model.openai.stream.Delta;
import org.elmo.robella.model.openai.tool.ToolCall;
import org.elmo.robella.model.openai.content.OpenAITextContent;
import org.elmo.robella.model.anthropic.core.AnthropicUsage;
import org.elmo.robella.service.stream.EndpointToUnifiedStreamTransformer;
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
public class AnthropicToUnifiedStreamTransformer implements EndpointToUnifiedStreamTransformer<Object> {

    // 会话状态存储，实际应用中可能需要使用更持久化的存储方案
    private final Map<String, AnthropicStreamSessionState> sessionStates = new ConcurrentHashMap<>();

    @Override
    public Flux<UnifiedStreamChunk> transform(Flux<Object> vendorStream, String sessionId) {
        // 初始化会话状态
        sessionStates.putIfAbsent(sessionId, new AnthropicStreamSessionState());

        return vendorStream.mapNotNull(event -> {
            AnthropicStreamSessionState state = sessionStates.get(sessionId);
            return processEvent(event, state);
        }).doFinally(signalType -> {
            // 清理会话状态
            sessionStates.remove(sessionId);
        });
    }

    private UnifiedStreamChunk processEvent(Object event, AnthropicStreamSessionState state) {
        if (event instanceof AnthropicMessageStartEvent messageStart) {
            // 流式开始，保存必要的会话状态
            state.setMessageId(messageStart.getMessage().getId());
            state.setModel(messageStart.getMessage().getModel());
            state.setCreated(System.currentTimeMillis() / 1000L);

            // 保存初始的使用量信息（包含input_tokens）
            if (messageStart.getMessage().getUsage() != null) {
                state.setInitialUsage(messageStart.getMessage().getUsage());
            }

            // 创建初始chunk
            Choice choice = new Choice();
            choice.setIndex(0);
            // 创建delta
            Delta delta = new Delta();
            // OpenAI 只在第一条流式中出现角色，后面都是内容的增量
            // Anthropic 的MessageStartEvent只有角色，不包含内容，某些OpenAI兼容的厂商会包含内容
            delta.setRole("assistant");
            choice.setDelta(delta);
            
            return createChunk(state, List.of(choice));

        } else if (event instanceof AnthropicContentBlockStartEvent blockStart) {
            // 内容块开始，记录索引
            int index = blockStart.getIndex();
            // 准备choice
            Choice choice = new Choice();
            choice.setIndex(0);

            Delta delta = new Delta();

            // 根据内容块类型进行不同处理
            AnthropicContent contentBlock = blockStart.getContentBlock();
            if (contentBlock instanceof AnthropicToolUseContent toolUseContent) {
                // 工具调用类型：记录映射关系、初始化工具调用信息并生成包含完整工具信息的toolcalls
                Integer toolCallIndex = state.getNextToolCallIndex().getAndIncrement();
                // 内容块索引映射工具调用索引，记录后面工具调用增量的toolcall index
                state.getToolUseIndices().put(index, toolCallIndex);

                // 保存工具调用的ID和名称
                state.getToolCallIds().put(toolCallIndex, toolUseContent.getId());
                state.getToolCallNames().put(toolCallIndex, toolUseContent.getName());

                // 会不会不支持造成并行工具调用？
                List<ToolCall> toolCalls = new ArrayList<>();
                ToolCall toolCall = new ToolCall();
                toolCall.setIndex(toolCallIndex);
                toolCall.setId(toolUseContent.getId()); // 直接使用工具调用的真实ID
                toolCall.setType("function");

                // 创建Function对象并设置属性
                ToolCall.Function function = new ToolCall.Function();
                function.setName(toolUseContent.getName()); // 直接使用工具调用的名称
                function.setArguments(""); // 初始时参数为空
                toolCall.setFunction(function);

                toolCalls.add(toolCall);
                delta.setToolCalls(toolCalls);
            } else if (contentBlock instanceof AnthropicTextContent) {
                // 普通文本类型：初始化空的文本内容
                OpenAITextContent textContent = new OpenAITextContent();
                textContent.setText("");
                delta.setContent(List.of(textContent));
            } else if (contentBlock instanceof AnthropicThinkingContent) {
                // 思考内容类型：初始化空的推理内容
                delta.setReasoningContent("");
            }

            choice.setDelta(delta);
            return createChunk(state, List.of(choice));

        } else if (event instanceof AnthropicContentBlockDeltaEvent blockDelta) {
            int index = blockDelta.getIndex();
            AnthropicDelta delta = blockDelta.getDelta();

            Choice choice = new Choice();
            choice.setIndex(0);

            Delta unifiedDelta = new Delta();

            // 根据delta的type字段判断增量类型，而不是通过contentBlock的类型
            if (delta.isTextDelta()) {
                OpenAITextContent textContent = new OpenAITextContent();
                textContent.setText(delta.getDeltaContent());
                unifiedDelta.setContent(List.of(textContent));
            } else if (delta.isInputJsonDelta()) {
                // 工具调用增量
                // 只需传递 index 和 arguments 增量，符合 OpenAI 流式格式规范
                // 根据 Anthropic 的 index 获取我们维护的 toolCallIndex
                Integer toolCallIndex = state.getToolUseIndices().get(index);
                if (toolCallIndex != null) {
                    List<ToolCall> toolCalls = new ArrayList<>();
                    ToolCall toolCall = new ToolCall();
                    // 仅设置索引，符合 OpenAI 流式增量规范
                    toolCall.setIndex(toolCallIndex);

                    // 创建Function对象并设置属性
                    ToolCall.Function function = new ToolCall.Function();
                    // 只传递本次的增量参数
                    function.setArguments(delta.getPartialJson());
                    toolCall.setFunction(function);

                    toolCalls.add(toolCall);
                    unifiedDelta.setToolCalls(toolCalls);
                }
            } else if (delta.isThinkingDelta()) {
                // 思考内容 - 使用reasoningContent字段
                unifiedDelta.setReasoningContent(delta.getDeltaContent());
            }

            choice.setDelta(unifiedDelta);
            return createChunk(state, List.of(choice));

        } else if (event instanceof AnthropicMessageDeltaEvent messageDelta) {
            Choice choice = new Choice();
            choice.setIndex(0);

            // 根据delta中包含的属性来设置相应的字段
            AnthropicDelta delta = messageDelta.getDelta();
            
            // 如果包含stopReason，则设置finishReason
            if (delta.getStopReason() != null) {
                choice.setFinishReason(convertStopReasonToFinishReason(delta.getStopReason()));
            }

            UnifiedStreamChunk chunk = createChunk(state, List.of(choice));
            
            // 如果包含usage信息，则设置usage
            if (messageDelta.getUsage() != null) {
                chunk.setUsage(convertUsage(messageDelta.getUsage(), state.getInitialUsage()));
            }

            return chunk;

        } else if (event instanceof AnthropicContentBlockStopEvent ||
                event instanceof AnthropicPingEvent ||
                event instanceof AnthropicErrorEvent ||
                event instanceof AnthropicMessageStopEvent) {
            // 这些事件不需要转换为统一格式的chunk
            // 可以根据需要处理错误或停止事件
            return null;
        }

        return null;
    }

    /**
     * 创建统一的流式响应chunk，统一设置基本字段
     *
     * @param state 会话状态
     * @param choices 选择列表
     * @return 统一的流式响应chunk
     */
    private UnifiedStreamChunk createChunk(AnthropicStreamSessionState state, List<Choice> choices) {
        UnifiedStreamChunk chunk = new UnifiedStreamChunk();
        chunk.setObject("chat.completion.chunk");
        chunk.setId(state.getMessageId());
        chunk.setModel(state.getModel());
        chunk.setCreated(state.getCreated());
        chunk.setChoices(choices);
        return chunk;
    }

    /**
     * 将AnthropicUsage转换为OpenAI的Usage
     *
     * @param currentUsage 当前事件中的使用量统计
     * @param initialUsage 初始使用量统计（可能包含input_tokens）
     * @return OpenAI的使用量统计
     */
    private Usage convertUsage(AnthropicUsage currentUsage, AnthropicUsage initialUsage) {
        if (currentUsage == null) {
            return null;
        }

        Usage usage = new Usage();

        // 使用初始使用量中的input_tokens（如果存在）
        Integer inputTokens = 0;
        if (initialUsage != null && initialUsage.getInputTokens() != null) {
            inputTokens = initialUsage.getInputTokens();
        }
        usage.setPromptTokens(inputTokens);

        // 使用当前使用量中的output_tokens
        Integer outputTokens = currentUsage.getOutputTokens() != null ? currentUsage.getOutputTokens() : 0;
        usage.setCompletionTokens(outputTokens);

        // 计算总token数
        usage.setTotalTokens(inputTokens + outputTokens);

        return usage;
    }

    /**
     * 将Anthropic的停止原因转换为OpenAI的完成原因
     *
     * @param stopReason Anthropic的停止原因
     * @return OpenAI的完成原因
     */
    private String convertStopReasonToFinishReason(String stopReason) {
        if (stopReason == null) {
            return null;
        }

        return switch (stopReason) {
            case "end_turn" -> "stop";
            case "max_tokens" -> "length";
            case "stop_sequence" -> "stop";
            case "tool_use" -> "tool_calls";
            default -> stopReason;
        };
    }

    /**
     * Anthropic流式会话状态
     */
    @Data
    private static class AnthropicStreamSessionState {
        private String messageId; // 当前消息的唯一标识符
        private String model; // 当前会话使用的模型名称
        private Long created; // 当前会话的创建时间
        private AnthropicUsage initialUsage; // 初始使用量信息（来自message_start事件）
        private final Map<Integer, Integer> toolUseIndices = new ConcurrentHashMap<>(); // 内容块索引到工具调用索引的映射
        private final Map<Integer, String> toolCallIds = new ConcurrentHashMap<>(); // 工具调用ID存储
        private final Map<Integer, String> toolCallNames = new ConcurrentHashMap<>(); // 工具调用名称存储
        private final AtomicInteger nextToolCallIndex = new AtomicInteger(0); // 下一个工具调用索引生成器
    }
}
package org.elmo.robella.service.stream.anthropic;

import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.anthropic.stream.*;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.openai.core.Choice;
import org.elmo.robella.model.openai.core.Usage;
import org.elmo.robella.model.openai.stream.Delta;
import org.elmo.robella.model.openai.tool.ToolCall;
import org.elmo.robella.model.openai.content.OpenAITextContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证修复后的转换器行为
 * 专注于验证thinking_delta事件的正确生成
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class AnthropicUnifiedToEndpointTransformerFixedTest {

    private AnthropicUnifiedToEndpointTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new AnthropicUnifiedToEndpointTransformer();
    }

    /**
     * 测试修复后的推理内容处理 - 应该包含thinking_delta事件
     */
    @Test
    void testFixedReasoningContentProcessing() {
        String sessionId = "test-reasoning-fixed-123";
        
        UnifiedStreamChunk chunk = createReasoningChunk("This is a reasoning content");
        
        Flux<Object> anthropicEvents = transformer.transformToEndpoint(Flux.just(chunk), sessionId);
        
        List<Object> collectedEvents = new CopyOnWriteArrayList<>();
        
        StepVerifier.create(anthropicEvents)
                .recordWith(() -> collectedEvents)
                .thenConsumeWhile(event -> true)
                .expectComplete()
                .verify();
        
        // 打印详细事件序列
        log.info("=== 修复后的推理内容事件序列 ===");
        for (int i = 0; i < collectedEvents.size(); i++) {
            Object event = collectedEvents.get(i);
            log.info("事件 {}: {}", i + 1, event.getClass().getSimpleName());
            if (event instanceof AnthropicContentBlockStartEvent) {
                AnthropicContentBlockStartEvent startEvent = (AnthropicContentBlockStartEvent) event;
                log.info("  - 类型: {}, 索引: {}, 内容类型: {}", 
                        startEvent.getType(), 
                        startEvent.getIndex(), 
                        startEvent.getContentBlock().getType());
            } else if (event instanceof AnthropicContentBlockDeltaEvent) {
                AnthropicContentBlockDeltaEvent deltaEvent = (AnthropicContentBlockDeltaEvent) event;
                log.info("  - 类型: {}, 索引: {}, Delta类型: {}, 内容: {}", 
                        deltaEvent.getType(), 
                        deltaEvent.getIndex(), 
                        deltaEvent.getDelta().getType(),
                        deltaEvent.getDelta().getThinking() != null ? 
                            deltaEvent.getDelta().getThinking().substring(0, Math.min(20, deltaEvent.getDelta().getThinking().length())) + "..." : "null");
            }
        }
        log.info("=== 事件序列结束 ===");
        
        // 验证thinking相关事件
        boolean hasMessageStart = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicMessageStartEvent);
        
        boolean hasThinkingStart = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicContentBlockStartEvent &&
                        ((AnthropicContentBlockStartEvent) event).getContentBlock().getType().equals("thinking"));
        
        boolean hasThinkingDelta = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicContentBlockDeltaEvent &&
                        ((AnthropicContentBlockDeltaEvent) event).getDelta().getType().equals("thinking_delta"));
        
        boolean hasThinkingStop = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicContentBlockStopEvent);
        
        log.info("修复后的推理内容测试结果:");
        log.info("- hasMessageStart: {}", hasMessageStart);
        log.info("- hasThinkingStart: {}", hasThinkingStart);
        log.info("- hasThinkingDelta: {}", hasThinkingDelta);
        log.info("- hasThinkingStop: {}", hasThinkingStop);
        log.info("- 总事件数: {}", collectedEvents.size());
        
        // 验证thinking块的完整序列
        assertTrue(hasMessageStart, "应该包含message_start事件");
        assertTrue(hasThinkingStart, "应该包含thinking内容块的start事件");
        assertTrue(hasThinkingDelta, "应该包含thinking_delta事件");
        assertTrue(hasThinkingStop, "应该包含thinking内容块的stop事件");
        
        // 验证事件数量：message_start + thinking_start + thinking_delta + thinking_stop = 4个事件
        assertEquals(4, collectedEvents.size(), "应该生成4个事件");
        
        log.info("✅ 修复成功: 推理内容现在包含thinking_delta事件");
    }

    /**
     * 测试修复后的复杂场景 - 包含thinking、text、tool_use的完整序列
     */
    @Test
    void testFixedComplexScenario() {
        String sessionId = "test-complex-fixed-123";
        
        // 模拟完整的OpenAI流式响应
        List<UnifiedStreamChunk> chunks = List.of(
            createReasoningChunk("用户需要获取天气信息。让我思考一下："),
            createTextChunk("我来帮您查询天气信息。"),
            createToolCallStartChunk("get_weather", "{\"location\":"),
            createToolCallDeltaChunk(" \"San Francisco\""),
            createToolCallDeltaChunk(", CA\"}"),
            createFinishChunk("tool_calls")
        );
        
        Flux<Object> anthropicEvents = transformer.transformToEndpoint(Flux.fromIterable(chunks), sessionId);
        
        List<Object> collectedEvents = new CopyOnWriteArrayList<>();
        
        StepVerifier.create(anthropicEvents)
                .recordWith(() -> collectedEvents)
                .thenConsumeWhile(event -> true)
                .expectComplete()
                .verify();
        
        // 统计各种事件类型
        long messageStartCount = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicMessageStartEvent)
                .count();
        
        long thinkingStartCount = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicContentBlockStartEvent &&
                        ((AnthropicContentBlockStartEvent) event).getContentBlock().getType().equals("thinking"))
                .count();
        
        long textStartCount = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicContentBlockStartEvent &&
                        ((AnthropicContentBlockStartEvent) event).getContentBlock().getType().equals("text"))
                .count();
        
        long toolUseStartCount = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicContentBlockStartEvent &&
                        ((AnthropicContentBlockStartEvent) event).getContentBlock().getType().equals("tool_use"))
                .count();
        
        long thinkingDeltaCount = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicContentBlockDeltaEvent &&
                        ((AnthropicContentBlockDeltaEvent) event).getDelta().getType().equals("thinking_delta"))
                .count();
        
        long textDeltaCount = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicContentBlockDeltaEvent &&
                        ((AnthropicContentBlockDeltaEvent) event).getDelta().getType().equals("text_delta"))
                .count();
        
        long inputJsonDeltaCount = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicContentBlockDeltaEvent &&
                        ((AnthropicContentBlockDeltaEvent) event).getDelta().getType().equals("input_json_delta"))
                .count();
        
        long signatureDeltaCount = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicContentBlockDeltaEvent &&
                        ((AnthropicContentBlockDeltaEvent) event).getDelta().getType().equals("signature_delta"))
                .count();
        
        long contentBlockStopCount = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicContentBlockStopEvent)
                .count();
        
        long messageDeltaCount = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicMessageDeltaEvent)
                .count();
        
        long messageStopCount = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicMessageStopEvent)
                .count();
        
        log.info("修复后的复杂场景事件统计:");
        log.info("- message_start: {}", messageStartCount);
        log.info("- thinking start: {}", thinkingStartCount);
        log.info("- text start: {}", textStartCount);
        log.info("- tool_use start: {}", toolUseStartCount);
        log.info("- thinking_delta: {}", thinkingDeltaCount);
        log.info("- text_delta: {}", textDeltaCount);
        log.info("- input_json_delta: {}", inputJsonDeltaCount);
        log.info("- signature_delta: {}", signatureDeltaCount);
        log.info("- content_block_stop: {}", contentBlockStopCount);
        log.info("- message_delta: {}", messageDeltaCount);
        log.info("- message_stop: {}", messageStopCount);
        log.info("- 总事件数: {}", collectedEvents.size());
        
        // 验证基本结构
        assertEquals(1, messageStartCount, "应该有一个message_start事件");
        assertEquals(1, thinkingStartCount, "应该有一个thinking内容块");
        assertTrue(textStartCount > 0, "应该有text内容块");
        assertTrue(toolUseStartCount > 0, "应该有tool_use内容块");
        assertEquals(1, messageDeltaCount, "应该有一个message_delta事件");
        assertEquals(1, messageStopCount, "应该有一个message_stop事件");
        
        // 验证thinking_delta事件现在存在
        assertTrue(thinkingDeltaCount > 0, "现在应该有thinking_delta事件");
        
        // 验证签名增量事件可能存在
        log.info("- signature_delta存在: {}", signatureDeltaCount > 0);
        
        // 验证start和stop事件数量匹配
        long totalStartEvents = thinkingStartCount + textStartCount + toolUseStartCount;
        assertEquals(totalStartEvents, contentBlockStopCount, "每个start事件应该有对应的stop事件");
        
        log.info("✅ 修复成功: 复杂场景现在包含完整的事件序列");
    }

    /**
     * 创建包含推理内容的chunk
     */
    private UnifiedStreamChunk createReasoningChunk(String reasoningContent) {
        UnifiedStreamChunk chunk = new UnifiedStreamChunk();
        chunk.setId("chatcmpl-123456");
        chunk.setObject("chat.completion.chunk");
        chunk.setCreated(Instant.now().getEpochSecond());
        chunk.setModel("gpt-4o");
        
        Choice choice = new Choice();
        choice.setIndex(0);
        
        Delta delta = new Delta();
        delta.setReasoningContent(reasoningContent);
        
        choice.setDelta(delta);
        chunk.setChoices(List.of(choice));
        
        return chunk;
    }

    /**
     * 创建包含文本内容的chunk
     */
    private UnifiedStreamChunk createTextChunk(String textContent) {
        UnifiedStreamChunk chunk = new UnifiedStreamChunk();
        chunk.setId("chatcmpl-123456");
        chunk.setObject("chat.completion.chunk");
        chunk.setCreated(Instant.now().getEpochSecond());
        chunk.setModel("gpt-4o");
        
        Choice choice = new Choice();
        choice.setIndex(0);
        
        Delta delta = new Delta();
        delta.setRole("assistant");
        
        OpenAITextContent content = new OpenAITextContent();
        content.setText(textContent);
        delta.setContent(List.of(content));
        
        choice.setDelta(delta);
        chunk.setChoices(List.of(choice));
        
        return chunk;
    }

    /**
     * 创建工具调用开始的chunk
     */
    private UnifiedStreamChunk createToolCallStartChunk(String functionName, String arguments) {
        UnifiedStreamChunk chunk = new UnifiedStreamChunk();
        chunk.setId("chatcmpl-123456");
        chunk.setObject("chat.completion.chunk");
        chunk.setCreated(Instant.now().getEpochSecond());
        chunk.setModel("gpt-4o");
        
        Choice choice = new Choice();
        choice.setIndex(0);
        
        Delta delta = new Delta();
        
        ToolCall toolCall = new ToolCall();
        toolCall.setIndex(0);
        toolCall.setId("call_abc123");
        toolCall.setType("function");
        
        ToolCall.Function function = new ToolCall.Function();
        function.setName(functionName);
        function.setArguments(arguments);
        toolCall.setFunction(function);
        
        delta.setToolCalls(List.of(toolCall));
        choice.setDelta(delta);
        chunk.setChoices(List.of(choice));
        
        return chunk;
    }

    /**
     * 创建工具调用参数增量的chunk
     */
    private UnifiedStreamChunk createToolCallDeltaChunk(String arguments) {
        UnifiedStreamChunk chunk = new UnifiedStreamChunk();
        chunk.setId("chatcmpl-123456");
        chunk.setObject("chat.completion.chunk");
        chunk.setCreated(Instant.now().getEpochSecond());
        chunk.setModel("gpt-4o");
        
        Choice choice = new Choice();
        choice.setIndex(0);
        
        Delta delta = new Delta();
        
        ToolCall toolCall = new ToolCall();
        toolCall.setIndex(0);
        
        ToolCall.Function function = new ToolCall.Function();
        function.setArguments(arguments);
        toolCall.setFunction(function);
        
        delta.setToolCalls(List.of(toolCall));
        choice.setDelta(delta);
        chunk.setChoices(List.of(choice));
        
        return chunk;
    }

    /**
     * 创建完成的chunk
     */
    private UnifiedStreamChunk createFinishChunk(String finishReason) {
        UnifiedStreamChunk chunk = new UnifiedStreamChunk();
        chunk.setId("chatcmpl-123456");
        chunk.setObject("chat.completion.chunk");
        chunk.setCreated(Instant.now().getEpochSecond());
        chunk.setModel("gpt-4o");
        
        Usage usage = new Usage();
        usage.setPromptTokens(10);
        usage.setCompletionTokens(20);
        usage.setTotalTokens(30);
        chunk.setUsage(usage);
        
        Choice choice = new Choice();
        choice.setIndex(0);
        choice.setFinishReason(finishReason);
        
        chunk.setChoices(List.of(choice));
        
        return chunk;
    }
}
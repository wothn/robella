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
 * 验证转换器实际行为的测试类
 * 专注于验证转换器是否生成了正确的事件序列
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class AnthropicUnifiedToEndpointTransformerBehaviorTest {

    private AnthropicUnifiedToEndpointTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new AnthropicUnifiedToEndpointTransformer();
    }

    /**
     * 测试转换器的基本行为 - 验证事件生成
     */
    @Test
    void testTransformerBehavior() {
        String sessionId = "test-session-123";
        
        // 创建一个简单的文本内容chunk
        UnifiedStreamChunk chunk = createTextChunk("Hello, world!");
        
        Flux<Object> anthropicEvents = transformer.transformToEndpoint(Flux.just(chunk), sessionId);
        
        List<Object> collectedEvents = new CopyOnWriteArrayList<>();
        
        StepVerifier.create(anthropicEvents)
                .recordWith(() -> collectedEvents)
                .thenConsumeWhile(event -> true) // 收集所有事件
                .expectComplete()
                .verify();
        
        // 打印实际生成的事件
        log.info("=== 实际生成的事件序列 ===");
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
                log.info("  - 类型: {}, 索引: {}, Delta类型: {}", 
                        deltaEvent.getType(), 
                        deltaEvent.getIndex(), 
                        deltaEvent.getDelta().getType());
            } else if (event instanceof AnthropicMessageStartEvent) {
                AnthropicMessageStartEvent messageStartEvent = (AnthropicMessageStartEvent) event;
                log.info("  - 类型: {}, 消息ID: {}, 模型: {}", 
                        messageStartEvent.getType(), 
                        messageStartEvent.getMessage().getId(), 
                        messageStartEvent.getMessage().getModel());
            } else if (event instanceof AnthropicMessageStopEvent) {
                log.info("  - 类型: {}", ((AnthropicMessageStopEvent) event).getType());
            }
        }
        log.info("=== 事件序列结束 ===");
        
        // 验证至少生成了事件
        assertFalse(collectedEvents.isEmpty(), "应该生成了事件");
        
        // 验证事件序列的基本结构
        boolean hasMessageStart = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicMessageStartEvent);
        
        assertTrue(hasMessageStart, "应该包含message_start事件");
        
        log.info("测试通过: 转换器生成了事件");
    }

    /**
     * 测试包含推理内容的转换 - 验证thinking块的处理
     */
    @Test
    void testReasoningContentProcessing() {
        String sessionId = "test-reasoning-123";
        
        UnifiedStreamChunk chunk = createReasoningChunk("This is a reasoning content");
        
        Flux<Object> anthropicEvents = transformer.transformToEndpoint(Flux.just(chunk), sessionId);
        
        List<Object> collectedEvents = new CopyOnWriteArrayList<>();
        
        StepVerifier.create(anthropicEvents)
                .recordWith(() -> collectedEvents)
                .thenConsumeWhile(event -> true)
                .expectComplete()
                .verify();
        
        // 检查thinking相关事件
        boolean hasThinkingStart = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicContentBlockStartEvent &&
                        ((AnthropicContentBlockStartEvent) event).getContentBlock().getType().equals("thinking"));
        
        boolean hasThinkingDelta = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicContentBlockDeltaEvent &&
                        ((AnthropicContentBlockDeltaEvent) event).getDelta().getType().equals("thinking_delta"));
        
        boolean hasThinkingStop = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicContentBlockStopEvent);
        
        log.info("推理内容测试结果:");
        log.info("- hasThinkingStart: {}", hasThinkingStart);
        log.info("- hasThinkingDelta: {}", hasThinkingDelta);
        log.info("- hasThinkingStop: {}", hasThinkingStop);
        
        // 验证thinking块的基本结构
        assertTrue(hasThinkingStart, "应该包含thinking内容块的start事件");
        assertTrue(hasThinkingDelta, "应该包含thinking_delta事件");
        
        // 检查thinking块的索引
        List<Integer> thinkingIndices = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicContentBlockStartEvent &&
                        ((AnthropicContentBlockStartEvent) event).getContentBlock().getType().equals("thinking"))
                .map(event -> ((AnthropicContentBlockStartEvent) event).getIndex())
                .toList();
        
        log.info("Thinking块索引: {}", thinkingIndices);
        
        log.info("测试通过: 推理内容转换完成");
    }

    /**
     * 测试工具调用的转换 - 验证tool_use块的处理
     */
    @Test
    void testToolCallProcessing() {
        String sessionId = "test-tool-123";
        
        // 创建工具调用相关的chunks
        List<UnifiedStreamChunk> chunks = List.of(
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
        
        // 检查tool_use相关事件
        boolean hasToolUseStart = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicContentBlockStartEvent &&
                        ((AnthropicContentBlockStartEvent) event).getContentBlock().getType().equals("tool_use"));
        
        boolean hasInputJsonDelta = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicContentBlockDeltaEvent &&
                        ((AnthropicContentBlockDeltaEvent) event).getDelta().getType().equals("input_json_delta"));
        
        boolean hasToolUseStop = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicContentBlockStopEvent);
        
        boolean hasMessageDelta = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicMessageDeltaEvent);
        
        boolean hasMessageStop = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicMessageStopEvent);
        
        log.info("工具调用测试结果:");
        log.info("- hasToolUseStart: {}", hasToolUseStart);
        log.info("- hasInputJsonDelta: {}", hasInputJsonDelta);
        log.info("- hasToolUseStop: {}", hasToolUseStop);
        log.info("- hasMessageDelta: {}", hasMessageDelta);
        log.info("- hasMessageStop: {}", hasMessageStop);
        
        // 验证tool_use块的基本结构
        assertTrue(hasToolUseStart, "应该包含tool_use内容块的start事件");
        assertTrue(hasInputJsonDelta, "应该包含input_json_delta事件");
        assertTrue(hasToolUseStop, "应该包含tool_use内容块的stop事件");
        assertTrue(hasMessageDelta, "应该包含message_delta事件");
        assertTrue(hasMessageStop, "应该包含message_stop事件");
        
        // 检查tool_use块的索引
        List<Integer> toolUseIndices = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicContentBlockStartEvent &&
                        ((AnthropicContentBlockStartEvent) event).getContentBlock().getType().equals("tool_use"))
                .map(event -> ((AnthropicContentBlockStartEvent) event).getIndex())
                .toList();
        
        log.info("Tool_use块索引: {}", toolUseIndices);
        
        // 检查stop_reason
        List<String> stopReasons = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicMessageDeltaEvent)
                .map(event -> ((AnthropicMessageDeltaEvent) event).getDelta().getStopReason())
                .toList();
        
        log.info("Stop reasons: {}", stopReasons);
        
        log.info("测试通过: 工具调用转换完成");
    }

    /**
     * 测试复杂场景 - 验证thinking、text、tool_use的组合处理
     */
    @Test
    void testComplexScenario() {
        String sessionId = "test-complex-123";
        
        // 模拟复杂的OpenAI流式响应
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
        
        long contentBlockStopCount = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicContentBlockStopEvent)
                .count();
        
        long messageDeltaCount = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicMessageDeltaEvent)
                .count();
        
        long messageStopCount = collectedEvents.stream()
                .filter(event -> event instanceof AnthropicMessageStopEvent)
                .count();
        
        log.info("复杂场景事件统计:");
        log.info("- message_start: {}", messageStartCount);
        log.info("- thinking start: {}", thinkingStartCount);
        log.info("- text start: {}", textStartCount);
        log.info("- tool_use start: {}", toolUseStartCount);
        log.info("- thinking_delta: {}", thinkingDeltaCount);
        log.info("- text_delta: {}", textDeltaCount);
        log.info("- input_json_delta: {}", inputJsonDeltaCount);
        log.info("- content_block_stop: {}", contentBlockStopCount);
        log.info("- message_delta: {}", messageDeltaCount);
        log.info("- message_stop: {}", messageStopCount);
        log.info("- 总事件数: {}", collectedEvents.size());
        
        // 验证基本结构
        assertEquals(1, messageStartCount, "应该有一个message_start事件");
        assertTrue(thinkingStartCount > 0, "应该有thinking内容块");
        assertTrue(textStartCount > 0, "应该有text内容块");
        assertTrue(toolUseStartCount > 0, "应该有tool_use内容块");
        assertEquals(1, messageDeltaCount, "应该有一个message_delta事件");
        assertEquals(1, messageStopCount, "应该有一个message_stop事件");
        
        // 验证start和stop事件数量匹配
        long totalStartEvents = thinkingStartCount + textStartCount + toolUseStartCount;
        assertEquals(totalStartEvents, contentBlockStopCount, "每个start事件应该有对应的stop事件");
        
        log.info("测试通过: 复杂场景转换完成");
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
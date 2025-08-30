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
 * 简化的AnthropicUnifiedToEndpointTransformer测试类
 * 专注于验证转换的基本功能
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class AnthropicUnifiedToEndpointTransformerSimpleTest {

    private AnthropicUnifiedToEndpointTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new AnthropicUnifiedToEndpointTransformer();
    }

    /**
     * 测试单个消息chunk的转换
     */
    @Test
    void testSingleMessageChunk() {
        String sessionId = "test-session-123";
        
        // 创建一个简单的消息chunk
        UnifiedStreamChunk chunk = new UnifiedStreamChunk();
        chunk.setId("chatcmpl-123456");
        chunk.setObject("chat.completion.chunk");
        chunk.setCreated(Instant.now().getEpochSecond());
        chunk.setModel("gpt-4o");
        
        // 转换为Anthropic事件流
        Flux<Object> anthropicEvents = transformer.transformToEndpoint(Flux.just(chunk), sessionId);
        
        // 收集所有事件
        List<Object> collectedEvents = new CopyOnWriteArrayList<>();
        
        StepVerifier.create(anthropicEvents)
                .recordWith(() -> collectedEvents)
                .expectNextMatches(event -> event instanceof AnthropicMessageStartEvent)
                .expectComplete()
                .verify();
        
        // 验证事件
        assertEquals(1, collectedEvents.size(), "应该生成一个事件");
        assertTrue(collectedEvents.get(0) instanceof AnthropicMessageStartEvent, "应该是message_start事件");
        
        AnthropicMessageStartEvent messageStartEvent = (AnthropicMessageStartEvent) collectedEvents.get(0);
        assertEquals("message_start", messageStartEvent.getType());
        assertNotNull(messageStartEvent.getMessage());
        assertEquals("chatcmpl-123456", messageStartEvent.getMessage().getId());
        assertEquals("message", messageStartEvent.getMessage().getType());
        assertEquals("gpt-4o", messageStartEvent.getMessage().getModel());
        
        log.info("测试通过: 单个消息chunk转换正确");
    }

    /**
     * 测试包含文本内容的chunk转换
     */
    @Test
    void testTextContentChunk() {
        String sessionId = "test-session-123";
        
        // 创建包含文本内容的chunk
        UnifiedStreamChunk chunk = createTextChunk("Hello, world!");
        
        Flux<Object> anthropicEvents = transformer.transformToEndpoint(Flux.just(chunk), sessionId);
        
        List<Object> collectedEvents = new CopyOnWriteArrayList<>();
        
        StepVerifier.create(anthropicEvents)
                .recordWith(() -> collectedEvents)
                .expectNextMatches(event -> event instanceof AnthropicMessageStartEvent)
                .expectNextMatches(event -> event instanceof AnthropicContentBlockStartEvent)
                .expectComplete()
                .verify();
        
        // 验证事件
        assertEquals(2, collectedEvents.size(), "应该生成两个事件");
        
        // 验证message_start事件
        assertTrue(collectedEvents.get(0) instanceof AnthropicMessageStartEvent);
        AnthropicMessageStartEvent messageStartEvent = (AnthropicMessageStartEvent) collectedEvents.get(0);
        assertEquals("message_start", messageStartEvent.getType());
        
        // 验证content_block_start事件
        assertTrue(collectedEvents.get(1) instanceof AnthropicContentBlockStartEvent);
        AnthropicContentBlockStartEvent blockStartEvent = (AnthropicContentBlockStartEvent) collectedEvents.get(1);
        assertEquals("content_block_start", blockStartEvent.getType());
        assertEquals("text", blockStartEvent.getContentBlock().getType());
        assertEquals(0, blockStartEvent.getIndex());
        
        log.info("测试通过: 文本内容chunk转换正确");
    }

    /**
     * 测试包含推理内容的chunk转换
     */
    @Test
    void testReasoningContentChunk() {
        String sessionId = "test-session-123";
        
        // 创建包含推理内容的chunk
        UnifiedStreamChunk chunk = createReasoningChunk("This is a reasoning content");
        
        Flux<Object> anthropicEvents = transformer.transformToEndpoint(Flux.just(chunk), sessionId);
        
        List<Object> collectedEvents = new CopyOnWriteArrayList<>();
        
        StepVerifier.create(anthropicEvents)
                .recordWith(() -> collectedEvents)
                .expectNextMatches(event -> event instanceof AnthropicMessageStartEvent)
                .expectNextMatches(event -> event instanceof AnthropicContentBlockStartEvent)
                .expectComplete()
                .verify();
        
        // 验证事件
        assertEquals(2, collectedEvents.size(), "应该生成两个事件");
        
        // 验证message_start事件
        assertTrue(collectedEvents.get(0) instanceof AnthropicMessageStartEvent);
        
        // 验证content_block_start事件
        assertTrue(collectedEvents.get(1) instanceof AnthropicContentBlockStartEvent);
        AnthropicContentBlockStartEvent blockStartEvent = (AnthropicContentBlockStartEvent) collectedEvents.get(1);
        assertEquals("content_block_start", blockStartEvent.getType());
        assertEquals("thinking", blockStartEvent.getContentBlock().getType());
        assertEquals(0, blockStartEvent.getIndex());
        
        log.info("测试通过: 推理内容chunk转换正确");
    }

    /**
     * 测试工具调用开始chunk的转换
     */
    @Test
    void testToolCallStartChunk() {
        String sessionId = "test-session-123";
        
        // 创建工具调用开始的chunk
        UnifiedStreamChunk chunk = createToolCallStartChunk("get_weather", "{\"location\":");
        
        Flux<Object> anthropicEvents = transformer.transformToEndpoint(Flux.just(chunk), sessionId);
        
        List<Object> collectedEvents = new CopyOnWriteArrayList<>();
        
        StepVerifier.create(anthropicEvents)
                .recordWith(() -> collectedEvents)
                .expectNextMatches(event -> event instanceof AnthropicMessageStartEvent)
                .expectNextMatches(event -> event instanceof AnthropicContentBlockStartEvent)
                .expectComplete()
                .verify();
        
        // 验证事件
        assertEquals(2, collectedEvents.size(), "应该生成两个事件");
        
        // 验证message_start事件
        assertTrue(collectedEvents.get(0) instanceof AnthropicMessageStartEvent);
        
        // 验证content_block_start事件
        assertTrue(collectedEvents.get(1) instanceof AnthropicContentBlockStartEvent);
        AnthropicContentBlockStartEvent blockStartEvent = (AnthropicContentBlockStartEvent) collectedEvents.get(1);
        assertEquals("content_block_start", blockStartEvent.getType());
        assertEquals("tool_use", blockStartEvent.getContentBlock().getType());
        assertEquals(0, blockStartEvent.getIndex());
        
        log.info("测试通过: 工具调用开始chunk转换正确");
    }

    /**
     * 测试包含完成原因的chunk转换
     */
    @Test
    void testFinishReasonChunk() {
        String sessionId = "test-session-123";
        
        // 创建包含完成原因的chunk
        UnifiedStreamChunk chunk = createFinishChunk("tool_calls");
        
        Flux<Object> anthropicEvents = transformer.transformToEndpoint(Flux.just(chunk), sessionId);
        
        List<Object> collectedEvents = new CopyOnWriteArrayList<>();
        
        StepVerifier.create(anthropicEvents)
                .recordWith(() -> collectedEvents)
                .expectNextMatches(event -> event instanceof AnthropicMessageStartEvent)
                .expectNextMatches(event -> event instanceof AnthropicContentBlockStartEvent) // 角色信息
                .expectNextMatches(event -> event instanceof AnthropicMessageDeltaEvent)
                .expectNextMatches(event -> event instanceof AnthropicMessageStopEvent)
                .expectComplete()
                .verify();
        
        // 验证事件
        assertEquals(4, collectedEvents.size(), "应该生成四个事件");
        
        // 验证message_delta事件
        assertTrue(collectedEvents.get(2) instanceof AnthropicMessageDeltaEvent);
        AnthropicMessageDeltaEvent deltaEvent = (AnthropicMessageDeltaEvent) collectedEvents.get(2);
        assertEquals("message_delta", deltaEvent.getType());
        assertEquals("tool_use", deltaEvent.getDelta().getStopReason());
        
        // 验证message_stop事件
        assertTrue(collectedEvents.get(3) instanceof AnthropicMessageStopEvent);
        AnthropicMessageStopEvent stopEvent = (AnthropicMessageStopEvent) collectedEvents.get(3);
        assertEquals("message_stop", stopEvent.getType());
        
        log.info("测试通过: 完成原因chunk转换正确");
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
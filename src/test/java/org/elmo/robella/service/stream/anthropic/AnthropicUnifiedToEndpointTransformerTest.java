package org.elmo.robella.service.stream.anthropic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * AnthropicUnifiedToEndpointTransformer 测试类
 * 验证OpenAI流式响应到Anthropic流式事件的转换正确性
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class AnthropicUnifiedToEndpointTransformerTest {

    private AnthropicUnifiedToEndpointTransformer transformer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        transformer = new AnthropicUnifiedToEndpointTransformer();
        objectMapper = new ObjectMapper();
    }

    /**
     * 测试基本的OpenAI流式响应转换为Anthropic流式事件
     * 基于docs/流式片段示例.md中的示例数据
     */
    @Test
    void testOpenAIStreamToAnthropicEvents() {
        String sessionId = "test-session-123";
        
        // 模拟OpenAI流式响应片段
        List<UnifiedStreamChunk> openAIChunks = createOpenAIStreamChunks();
        
        // 转换为Anthropic事件流
        Flux<Object> anthropicEvents = transformer.transformToEndpoint(Flux.fromIterable(openAIChunks), sessionId);
        
        // 收集所有事件
        List<Object> collectedEvents = new CopyOnWriteArrayList<>();
        
        StepVerifier.create(anthropicEvents)
                .recordWith(() -> collectedEvents)
                .expectNextCount(1) // 至少应该有一个事件
                .expectComplete()
                .verify();
        
        // 验证事件序列
        validateAnthropicEventSequence(collectedEvents);
        
        // 打印事件序列用于调试
        printEventSequence(collectedEvents);
    }

    /**
     * 测试仅包含推理内容的转换
     */
    @Test
    void testReasoningContentOnly() {
        String sessionId = "test-reasoning-123";
        
        // 创建仅包含推理内容的chunk
        UnifiedStreamChunk reasoningChunk = createReasoningChunk("This is a reasoning content");
        
        Flux<Object> anthropicEvents = transformer.transformToEndpoint(Flux.just(reasoningChunk), sessionId);
        
        List<Object> collectedEvents = new CopyOnWriteArrayList<>();
        
        StepVerifier.create(anthropicEvents)
                .recordWith(() -> collectedEvents)
                .expectNextCount(1) // 至少应该有一个事件
                .expectComplete()
                .verify();
        
        // 验证推理内容块
        boolean hasThinkingStart = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicContentBlockStartEvent &&
                        ((AnthropicContentBlockStartEvent) event).getContentBlock().getType().equals("thinking"));
        
        assertTrue(hasThinkingStart, "应该包含thinking内容块的start事件");
    }

    /**
     * 测试仅包含文本内容的转换
     */
    @Test
    void testTextContentOnly() {
        String sessionId = "test-text-123";
        
        UnifiedStreamChunk textChunk = createTextChunk("Hello, world!");
        
        Flux<Object> anthropicEvents = transformer.transformToEndpoint(Flux.just(textChunk), sessionId);
        
        List<Object> collectedEvents = new CopyOnWriteArrayList<>();
        
        StepVerifier.create(anthropicEvents)
                .recordWith(() -> collectedEvents)
                .expectNextCount(1) // 至少应该有一个事件
                .expectComplete()
                .verify();
        
        // 验证文本内容块
        boolean hasTextStart = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicContentBlockStartEvent &&
                        ((AnthropicContentBlockStartEvent) event).getContentBlock().getType().equals("text"));
        
        assertTrue(hasTextStart, "应该包含text内容块的start事件");
    }

    /**
     * 测试工具调用转换
     */
    @Test
    void testToolCallConversion() {
        String sessionId = "test-tool-123";
        
        // 创建工具调用相关的chunk
        List<UnifiedStreamChunk> toolCallChunks = createToolCallChunks();
        
        Flux<Object> anthropicEvents = transformer.transformToEndpoint(Flux.fromIterable(toolCallChunks), sessionId);
        
        List<Object> collectedEvents = new CopyOnWriteArrayList<>();
        
        StepVerifier.create(anthropicEvents)
                .recordWith(() -> collectedEvents)
                .expectNextCount(1) // 至少应该有一个事件
                .expectComplete()
                .verify();
        
        // 验证工具调用相关事件
        boolean hasToolUseStart = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicContentBlockStartEvent &&
                        ((AnthropicContentBlockStartEvent) event).getContentBlock().getType().equals("tool_use"));
        
        assertTrue(hasToolUseStart, "应该包含tool_use内容块的start事件");
        
        // 验证工具调用参数增量
        boolean hasInputJsonDelta = collectedEvents.stream()
                .anyMatch(event -> event instanceof AnthropicContentBlockDeltaEvent &&
                        ((AnthropicContentBlockDeltaEvent) event).getDelta().getType().equals("input_json_delta"));
        
        assertTrue(hasInputJsonDelta, "应该包含input_json_delta事件");
    }

    /**
     * 创建模拟的OpenAI流式响应片段
     * 基于docs/流式片段示例.md中的OpenAI响应
     */
    private List<UnifiedStreamChunk> createOpenAIStreamChunks() {
        return List.of(
            // 1. 包含推理内容的chunk
            createReasoningChunk("用户需要获取旧金山的天气信息。让我思考一下：\n\n1. 用户询问旧金山的天气情况\n2. 我有一个可用的获取天气工具\n3. 需要提供位置参数\n4. 位置应该格式化为\"San Francisco, CA\"\n5. 获取到天气数据后，需要清晰地呈现给用户"),
            
            // 2. 包含推理内容续的chunk
            createReasoningChunk("\n\n执行计划：\n- 使用获取天气工具，位置设置为\"San Francisco, CA\"\n- 等待工具返回结果\n- 整理并展示天气信息"),
            
            // 3. 包含文本内容的chunk
            createTextChunk("我来帮您查询旧金山当前的天气信息。"),
            
            // 4. 包含更多文本内容的chunk
            createTextChunk("让我为您查看一下。"),
            
            // 5. 工具调用开始的chunk
            createToolCallStartChunk("get_weather", "{\"location\":"),
            
            // 6. 工具调用参数增量的chunk
            createToolCallDeltaChunk(" \"San"),
            
            // 7. 更多工具调用参数增量的chunk
            createToolCallDeltaChunk(" Francisco"),
            
            // 8. 更多工具调用参数增量的chunk
            createToolCallDeltaChunk(", \"unit\": \"cels"),
            
            // 9. 工具调用参数完成的chunk
            createToolCallDeltaChunk("ius\"}"),
            
            // 10. 完成chunk
            createFinishChunk("tool_calls")
        );
    }

    /**
     * 创建包含推理内容的chunk
     */
    private UnifiedStreamChunk createReasoningChunk(String reasoningContent) {
        UnifiedStreamChunk chunk = new UnifiedStreamChunk();
        chunk.setId("chatcmpl-9abc123def456");
        chunk.setObject("chat.completion.chunk");
        chunk.setCreated(Instant.now().getEpochSecond());
        chunk.setModel("gpt-4o");
        chunk.setSystemFingerprint("fp_123456");
        
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
        chunk.setId("chatcmpl-9abc123def456");
        chunk.setObject("chat.completion.chunk");
        chunk.setCreated(Instant.now().getEpochSecond());
        chunk.setModel("gpt-4o");
        chunk.setSystemFingerprint("fp_123456");
        
        Choice choice = new Choice();
        choice.setIndex(0);
        
        Delta delta = new Delta();
        
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
        chunk.setId("chatcmpl-9abc123def456");
        chunk.setObject("chat.completion.chunk");
        chunk.setCreated(Instant.now().getEpochSecond());
        chunk.setModel("gpt-4o");
        chunk.setSystemFingerprint("fp_123456");
        
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
        chunk.setId("chatcmpl-9abc123def456");
        chunk.setObject("chat.completion.chunk");
        chunk.setCreated(Instant.now().getEpochSecond());
        chunk.setModel("gpt-4o");
        chunk.setSystemFingerprint("fp_123456");
        
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
        chunk.setId("chatcmpl-9abc123def456");
        chunk.setObject("chat.completion.chunk");
        chunk.setCreated(Instant.now().getEpochSecond());
        chunk.setModel("gpt-4o");
        chunk.setSystemFingerprint("fp_123456");
        
        Usage usage = new Usage();
        usage.setPromptTokens(156);
        usage.setCompletionTokens(156);
        usage.setTotalTokens(312);
        chunk.setUsage(usage);
        
        Choice choice = new Choice();
        choice.setIndex(0);
        choice.setFinishReason(finishReason);
        
        chunk.setChoices(List.of(choice));
        
        return chunk;
    }

    /**
     * 创建工具调用相关的chunks
     */
    private List<UnifiedStreamChunk> createToolCallChunks() {
        return List.of(
            createToolCallStartChunk("get_weather", "{\"location\":"),
            createToolCallDeltaChunk(" \"San Francisco"),
            createToolCallDeltaChunk(", CA\""),
            createFinishChunk("tool_calls")
        );
    }

    /**
     * 验证Anthropic事件序列的完整性
     */
    private void validateAnthropicEventSequence(List<Object> events) {
        // 验证基本的必需事件
        boolean hasMessageStart = events.stream()
                .anyMatch(event -> event instanceof AnthropicMessageStartEvent);
        
        boolean hasMessageStop = events.stream()
                .anyMatch(event -> event instanceof AnthropicMessageStopEvent);
        
        assertTrue(hasMessageStart, "应该包含message_start事件");
        assertTrue(hasMessageStop, "应该包含message_stop事件");
        
        // 验证内容块事件的完整性
        List<AnthropicContentBlockStartEvent> startEvents = events.stream()
                .filter(event -> event instanceof AnthropicContentBlockStartEvent)
                .map(event -> (AnthropicContentBlockStartEvent) event)
                .toList();
        
        List<AnthropicContentBlockStopEvent> stopEvents = events.stream()
                .filter(event -> event instanceof AnthropicContentBlockStopEvent)
                .map(event -> (AnthropicContentBlockStopEvent) event)
                .toList();
        
        // 每个start事件应该有对应的stop事件
        assertEquals(startEvents.size(), stopEvents.size(), 
                "每个content_block_start事件应该有对应的content_block_stop事件");
        
        // 验证内容块类型的正确性
        List<String> contentBlockTypes = startEvents.stream()
                .map(event -> event.getContentBlock().getType())
                .toList();
        
        // 验证是否存在预期的内容块类型
        assertTrue(contentBlockTypes.contains("thinking") || contentBlockTypes.contains("text") || contentBlockTypes.contains("tool_use"),
                "应该包含至少一种内容块类型");
    }

    /**
     * 打印事件序列用于调试
     */
    private void printEventSequence(List<Object> events) {
        log.info("=== Anthropic Event Sequence ===");
        for (int i = 0; i < events.size(); i++) {
            Object event = events.get(i);
            try {
                String json = objectMapper.writeValueAsString(event);
                log.info("Event {}: {}", i + 1, json);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize event {}", i + 1, e);
            }
        }
        log.info("=== End of Event Sequence ===");
    }
}
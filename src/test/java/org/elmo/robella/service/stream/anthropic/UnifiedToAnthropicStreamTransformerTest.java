package org.elmo.robella.service.stream.anthropic;

import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.anthropic.stream.*;
import org.elmo.robella.model.anthropic.content.*;
import org.elmo.robella.model.openai.core.Choice;
import org.elmo.robella.model.openai.core.Usage;
import org.elmo.robella.model.openai.stream.Delta;
import org.elmo.robella.model.openai.content.OpenAITextContent;
import org.elmo.robella.model.openai.tool.ToolCall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UnifiedToAnthropicStreamTransformer 单元测试
 */
@ExtendWith(MockitoExtension.class)
class UnifiedToAnthropicStreamTransformerTest {

    private UnifiedToAnthropicStreamTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new UnifiedToAnthropicStreamTransformer();
    }

    @Test
    void testSimpleTextMessage() {
        // 准备测试数据
        String sessionId = "test-session-1";
        
        // 第一个chunk：包含role
        UnifiedStreamChunk chunk1 = createChunkWithRole("assistant", "gpt-4");
        
        // 第二个chunk：包含文本内容
        UnifiedStreamChunk chunk2 = createChunkWithTextContent("Hello");
        
        // 第三个chunk：更多文本内容
        UnifiedStreamChunk chunk3 = createChunkWithTextContent(" world!");
        
        // 第四个chunk：结束
        UnifiedStreamChunk chunk4 = createChunkWithFinishReason("stop");

        Flux<UnifiedStreamChunk> inputStream = Flux.just(chunk1, chunk2, chunk3, chunk4);

        // 执行转换
        Flux<AnthropicStreamEvent> result = transformer.transform(inputStream, sessionId);

        // 验证结果
        StepVerifier.create(result)
                .assertNext(event -> {
                    // 验证 message_start 事件
                    assertThat(event).isInstanceOf(AnthropicMessageStartEvent.class);
                    AnthropicMessageStartEvent startEvent = (AnthropicMessageStartEvent) event;
                    assertThat(startEvent.getType()).isEqualTo("message_start");
                    assertThat(startEvent.getMessage()).isNotNull();
                    assertThat(startEvent.getMessage().getRole()).isEqualTo("assistant");
                    assertThat(startEvent.getMessage().getModel()).isEqualTo("gpt-4");
                })
                .assertNext(event -> {
                    // 验证 content_block_start 事件
                    assertThat(event).isInstanceOf(AnthropicContentBlockStartEvent.class);
                    AnthropicContentBlockStartEvent blockStart = (AnthropicContentBlockStartEvent) event;
                    assertThat(blockStart.getType()).isEqualTo("content_block_start");
                    assertThat(blockStart.getIndex()).isEqualTo(0);
                    assertThat(blockStart.getContentBlock()).isInstanceOf(AnthropicTextContent.class);
                })
                .assertNext(event -> {
                    // 验证第一个 content_block_delta 事件
                    assertThat(event).isInstanceOf(AnthropicContentBlockDeltaEvent.class);
                    AnthropicContentBlockDeltaEvent delta = (AnthropicContentBlockDeltaEvent) event;
                    assertThat(delta.getType()).isEqualTo("content_block_delta");
                    assertThat(delta.getIndex()).isEqualTo(0);
                    assertThat(delta.getDelta().getType()).isEqualTo("text_delta");
                    assertThat(delta.getDelta().getText()).isEqualTo("Hello");
                })
                .assertNext(event -> {
                    // 验证第二个 content_block_delta 事件
                    assertThat(event).isInstanceOf(AnthropicContentBlockDeltaEvent.class);
                    AnthropicContentBlockDeltaEvent delta = (AnthropicContentBlockDeltaEvent) event;
                    assertThat(delta.getDelta().getText()).isEqualTo(" world!");
                })
                .assertNext(event -> {
                    // 验证 content_block_stop 事件
                    assertThat(event).isInstanceOf(AnthropicContentBlockStopEvent.class);
                    AnthropicContentBlockStopEvent stopEvent = (AnthropicContentBlockStopEvent) event;
                    assertThat(stopEvent.getType()).isEqualTo("content_block_stop");
                    assertThat(stopEvent.getIndex()).isEqualTo(0);
                })
                .assertNext(event -> {
                    // 验证 message_delta 事件
                    assertThat(event).isInstanceOf(AnthropicMessageDeltaEvent.class);
                    AnthropicMessageDeltaEvent deltaEvent = (AnthropicMessageDeltaEvent) event;
                    assertThat(deltaEvent.getType()).isEqualTo("message_delta");
                    assertThat(deltaEvent.getDelta().getStopReason()).isEqualTo("end_turn");
                })
                .assertNext(event -> {
                    // 验证 message_stop 事件
                    assertThat(event).isInstanceOf(AnthropicMessageStopEvent.class);
                    AnthropicMessageStopEvent stopEvent = (AnthropicMessageStopEvent) event;
                    assertThat(stopEvent.getType()).isEqualTo("message_stop");
                })
                .verifyComplete();
    }

    @Test
    void testToolCallMessage() {
        // 准备测试数据
        String sessionId = "test-session-2";
        
        // 第一个chunk：包含role
        UnifiedStreamChunk chunk1 = createChunkWithRole("assistant", "gpt-4");
        
        // 第二个chunk：包含工具调用
        UnifiedStreamChunk chunk2 = createChunkWithToolCall(0, "call_123", "get_weather", "{\"city\":");
        
        // 第三个chunk：更多工具参数
        UnifiedStreamChunk chunk3 = createChunkWithToolCall(0, null, null, "\"Beijing\"}");
        
        // 第四个chunk：结束
        UnifiedStreamChunk chunk4 = createChunkWithFinishReason("tool_calls");

        Flux<UnifiedStreamChunk> inputStream = Flux.just(chunk1, chunk2, chunk3, chunk4);

        // 执行转换
        Flux<AnthropicStreamEvent> result = transformer.transform(inputStream, sessionId);

        // 验证结果
        StepVerifier.create(result)
                .assertNext(event -> {
                    // 验证 message_start 事件
                    assertThat(event).isInstanceOf(AnthropicMessageStartEvent.class);
                })
                .assertNext(event -> {
                    // 验证 content_block_start 事件（工具调用）
                    assertThat(event).isInstanceOf(AnthropicContentBlockStartEvent.class);
                    AnthropicContentBlockStartEvent blockStart = (AnthropicContentBlockStartEvent) event;
                    assertThat(blockStart.getContentBlock()).isInstanceOf(AnthropicToolUseContent.class);
                    AnthropicToolUseContent toolUse = (AnthropicToolUseContent) blockStart.getContentBlock();
                    assertThat(toolUse.getId()).isEqualTo("call_123");
                    assertThat(toolUse.getName()).isEqualTo("get_weather");
                })
                .assertNext(event -> {
                    // 验证 input_json_delta 事件
                    assertThat(event).isInstanceOf(AnthropicContentBlockDeltaEvent.class);
                    AnthropicContentBlockDeltaEvent delta = (AnthropicContentBlockDeltaEvent) event;
                    assertThat(delta.getDelta().getType()).isEqualTo("input_json_delta");
                    assertThat(delta.getDelta().getPartialJson()).isEqualTo("{\"city\":");
                })
                .assertNext(event -> {
                    // 验证第二个 input_json_delta 事件
                    assertThat(event).isInstanceOf(AnthropicContentBlockDeltaEvent.class);
                    AnthropicContentBlockDeltaEvent delta = (AnthropicContentBlockDeltaEvent) event;
                    assertThat(delta.getDelta().getPartialJson()).isEqualTo("\"Beijing\"}");
                })
                .assertNext(event -> {
                    // 验证 content_block_stop 事件
                    assertThat(event).isInstanceOf(AnthropicContentBlockStopEvent.class);
                })
                .assertNext(event -> {
                    // 验证 message_delta 事件
                    assertThat(event).isInstanceOf(AnthropicMessageDeltaEvent.class);
                    AnthropicMessageDeltaEvent deltaEvent = (AnthropicMessageDeltaEvent) event;
                    assertThat(deltaEvent.getDelta().getStopReason()).isEqualTo("tool_use");
                })
                .assertNext(event -> {
                    // 验证 message_stop 事件
                    assertThat(event).isInstanceOf(AnthropicMessageStopEvent.class);
                })
                .verifyComplete();
    }

    @Test
    void testReasoningContent() {
        // 准备测试数据
        String sessionId = "test-session-3";
        
        // 第一个chunk：包含role
        UnifiedStreamChunk chunk1 = createChunkWithRole("assistant", "gpt-4");
        
        // 第二个chunk：包含推理内容
        UnifiedStreamChunk chunk2 = createChunkWithReasoningContent("Let me think about this...");
        
        // 第三个chunk：结束
        UnifiedStreamChunk chunk3 = createChunkWithFinishReason("stop");

        Flux<UnifiedStreamChunk> inputStream = Flux.just(chunk1, chunk2, chunk3);

        // 执行转换
        Flux<AnthropicStreamEvent> result = transformer.transform(inputStream, sessionId);

        // 验证结果包含thinking相关事件
        StepVerifier.create(result)
                .assertNext(event -> {
                    // message_start
                    assertThat(event).isInstanceOf(AnthropicMessageStartEvent.class);
                })
                .assertNext(event -> {
                    // content_block_start (thinking)
                    assertThat(event).isInstanceOf(AnthropicContentBlockStartEvent.class);
                    AnthropicContentBlockStartEvent blockStart = (AnthropicContentBlockStartEvent) event;
                    assertThat(blockStart.getContentBlock()).isInstanceOf(AnthropicThinkingContent.class);
                })
                .assertNext(event -> {
                    // thinking_delta
                    assertThat(event).isInstanceOf(AnthropicContentBlockDeltaEvent.class);
                    AnthropicContentBlockDeltaEvent delta = (AnthropicContentBlockDeltaEvent) event;
                    assertThat(delta.getDelta().getType()).isEqualTo("thinking_delta");
                    assertThat(delta.getDelta().getThinking()).isEqualTo("Let me think about this...");
                })
                .assertNext(event -> {
                    // content_block_stop
                    assertThat(event).isInstanceOf(AnthropicContentBlockStopEvent.class);
                })
                .assertNext(event -> {
                    // message_delta
                    assertThat(event).isInstanceOf(AnthropicMessageDeltaEvent.class);
                })
                .assertNext(event -> {
                    // message_stop
                    assertThat(event).isInstanceOf(AnthropicMessageStopEvent.class);
                })
                .verifyComplete();
    }

    @Test
    void testEmptyChunk() {
        // 测试空chunk处理
        String sessionId = "test-session-4";
        
        UnifiedStreamChunk emptyChunk = new UnifiedStreamChunk();
        emptyChunk.setChoices(new ArrayList<>());

        Flux<UnifiedStreamChunk> inputStream = Flux.just(emptyChunk);

        // 执行转换
        Flux<AnthropicStreamEvent> result = transformer.transform(inputStream, sessionId);

        // 验证结果
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void testFinishReasonMapping() {
        // 测试不同finish reason的映射
        String sessionId = "test-session-5";
        
        // 测试不同的finish reason
        UnifiedStreamChunk chunk1 = createChunkWithRole("assistant", "gpt-4");
        UnifiedStreamChunk chunk2 = createChunkWithFinishReason("length");

        Flux<UnifiedStreamChunk> inputStream = Flux.just(chunk1, chunk2);

        Flux<AnthropicStreamEvent> result = transformer.transform(inputStream, sessionId);

        StepVerifier.create(result)
                .assertNext(event -> {
                    assertThat(event).isInstanceOf(AnthropicMessageStartEvent.class);
                })
                .assertNext(event -> {
                    assertThat(event).isInstanceOf(AnthropicMessageDeltaEvent.class);
                    AnthropicMessageDeltaEvent deltaEvent = (AnthropicMessageDeltaEvent) event;
                    assertThat(deltaEvent.getDelta().getStopReason()).isEqualTo("max_tokens");
                })
                .assertNext(event -> {
                    assertThat(event).isInstanceOf(AnthropicMessageStopEvent.class);
                })
                .verifyComplete();
    }

    @Test
    void testMessageIdFromUnifiedChunk() {
        // 测试使用 UnifiedStreamChunk 中的 ID 作为 messageId
        String sessionId = "test-session-messageid";
        String expectedMessageId = "chatcmpl-123456789";
        
        // 第一个chunk：包含role和id
        UnifiedStreamChunk chunk1 = createChunkWithRole("assistant", "gpt-4");
        chunk1.setId(expectedMessageId);  // 设置特定的ID
        
        // 第二个chunk：结束
        UnifiedStreamChunk chunk2 = createChunkWithFinishReason("stop");

        Flux<UnifiedStreamChunk> inputStream = Flux.just(chunk1, chunk2);

        // 执行转换
        Flux<AnthropicStreamEvent> result = transformer.transform(inputStream, sessionId);

        // 验证结果
        StepVerifier.create(result)
                .assertNext(event -> {
                    // 验证 message_start 事件使用了正确的 messageId
                    assertThat(event).isInstanceOf(AnthropicMessageStartEvent.class);
                    AnthropicMessageStartEvent startEvent = (AnthropicMessageStartEvent) event;
                    assertThat(startEvent.getMessage().getId()).isEqualTo(expectedMessageId);
                })
                .assertNext(event -> {
                    // message_delta 事件
                    assertThat(event).isInstanceOf(AnthropicMessageDeltaEvent.class);
                })
                .assertNext(event -> {
                    // message_stop 事件
                    assertThat(event).isInstanceOf(AnthropicMessageStopEvent.class);
                })
                .verifyComplete();
    }

    @Test
    void testMessageIdFallbackToGenerated() {
        // 测试当 UnifiedStreamChunk 中没有 ID 时，生成随机 messageId
        String sessionId = "test-session-fallback";
        
        // 第一个chunk：包含role但没有id
        UnifiedStreamChunk chunk1 = createChunkWithRole("assistant", "gpt-4");
        chunk1.setId(null);  // 确保没有ID
        
        // 第二个chunk：结束
        UnifiedStreamChunk chunk2 = createChunkWithFinishReason("stop");

        Flux<UnifiedStreamChunk> inputStream = Flux.just(chunk1, chunk2);

        // 执行转换
        Flux<AnthropicStreamEvent> result = transformer.transform(inputStream, sessionId);

        // 验证结果
        StepVerifier.create(result)
                .assertNext(event -> {
                    // 验证 message_start 事件使用了随机生成的 messageId
                    assertThat(event).isInstanceOf(AnthropicMessageStartEvent.class);
                    AnthropicMessageStartEvent startEvent = (AnthropicMessageStartEvent) event;
                    assertThat(startEvent.getMessage().getId()).isNotNull();
                    assertThat(startEvent.getMessage().getId()).startsWith("msg_");
                })
                .assertNext(event -> {
                    // message_delta 事件
                    assertThat(event).isInstanceOf(AnthropicMessageDeltaEvent.class);
                })
                .assertNext(event -> {
                    // message_stop 事件
                    assertThat(event).isInstanceOf(AnthropicMessageStopEvent.class);
                })
                .verifyComplete();
    }

    // ======================== 辅助方法 ========================

    private UnifiedStreamChunk createChunkWithRole(String role, String model) {
        UnifiedStreamChunk chunk = new UnifiedStreamChunk();
        chunk.setModel(model);
        chunk.setId("chunk_123");
        chunk.setCreated(System.currentTimeMillis() / 1000);
        
        Choice choice = new Choice();
        Delta delta = new Delta();
        delta.setRole(role);
        choice.setDelta(delta);
        choice.setIndex(0);
        
        chunk.setChoices(List.of(choice));
        return chunk;
    }

    private UnifiedStreamChunk createChunkWithTextContent(String text) {
        UnifiedStreamChunk chunk = new UnifiedStreamChunk();
        chunk.setModel("gpt-4");
        
        Choice choice = new Choice();
        Delta delta = new Delta();
        
        OpenAITextContent textContent = new OpenAITextContent();
        textContent.setType("text");  // 设置类型
        textContent.setText(text);
        delta.setContent(List.of(textContent));
        
        choice.setDelta(delta);
        choice.setIndex(0);
        
        chunk.setChoices(List.of(choice));
        return chunk;
    }

    private UnifiedStreamChunk createChunkWithToolCall(Integer index, String id, String name, String arguments) {
        UnifiedStreamChunk chunk = new UnifiedStreamChunk();
        chunk.setModel("gpt-4");
        
        Choice choice = new Choice();
        Delta delta = new Delta();
        
        ToolCall toolCall = new ToolCall();
        toolCall.setIndex(index);
        if (id != null) {
            toolCall.setId(id);
        }
        
        if (name != null || arguments != null) {
            ToolCall.Function function = new ToolCall.Function();
            if (name != null) {
                function.setName(name);
            }
            if (arguments != null) {
                function.setArguments(arguments);
            }
            toolCall.setFunction(function);
        }
        
        delta.setToolCalls(List.of(toolCall));
        choice.setDelta(delta);
        choice.setIndex(0);
        
        chunk.setChoices(List.of(choice));
        return chunk;
    }

    private UnifiedStreamChunk createChunkWithReasoningContent(String reasoning) {
        UnifiedStreamChunk chunk = new UnifiedStreamChunk();
        chunk.setModel("gpt-4");
        
        Choice choice = new Choice();
        Delta delta = new Delta();
        delta.setReasoningContent(reasoning);
        
        choice.setDelta(delta);
        choice.setIndex(0);
        
        chunk.setChoices(List.of(choice));
        return chunk;
    }

    private UnifiedStreamChunk createChunkWithFinishReason(String finishReason) {
        UnifiedStreamChunk chunk = new UnifiedStreamChunk();
        chunk.setModel("gpt-4");
        
        Choice choice = new Choice();
        choice.setFinishReason(finishReason);
        choice.setIndex(0);
        
        // 添加usage信息
        Usage usage = new Usage();
        usage.setPromptTokens(10);
        usage.setCompletionTokens(20);
        usage.setTotalTokens(30);
        chunk.setUsage(usage);
        
        chunk.setChoices(List.of(choice));
        return chunk;
    }
}
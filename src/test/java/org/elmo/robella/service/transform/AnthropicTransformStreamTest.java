package org.elmo.robella.service.transform;

import org.elmo.robella.model.anthropic.*;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AnthropicTransform 流式处理测试
 */
public class AnthropicTransformStreamTest {

    private final AnthropicTransform transform = new AnthropicTransform();

    @Test
    public void testTextDeltaConversion() {
        // 测试 text_delta 事件转换
        AnthropicContentBlockDeltaEvent deltaEvent = new AnthropicContentBlockDeltaEvent();
        deltaEvent.setType("content_block_delta");
        deltaEvent.setIndex(0);
        
        AnthropicDelta delta = new AnthropicDelta();
        delta.setType("text_delta");
        delta.setText("Hello");
        deltaEvent.setDelta(delta);
        
        UnifiedStreamChunk chunk = transform.vendorStreamEventToUnified(deltaEvent);
        
        assertNotNull(chunk);
        assertFalse(chunk.isFinished());
        assertEquals("Hello", chunk.getContentDelta());
        assertNotNull(chunk.getChoices());
        assertEquals(1, chunk.getChoices().size());
    }

    @Test
    public void testThinkingDeltaConversion() {
        // 测试 thinking_delta 事件转换
        AnthropicContentBlockDeltaEvent deltaEvent = new AnthropicContentBlockDeltaEvent();
        deltaEvent.setType("content_block_delta");
        deltaEvent.setIndex(0);
        
        AnthropicDelta delta = new AnthropicDelta();
        delta.setType("thinking_delta");
        delta.setThinking("Let me think...");
        deltaEvent.setDelta(delta);
        
        UnifiedStreamChunk chunk = transform.vendorStreamEventToUnified(deltaEvent);
        
        assertNotNull(chunk);
        assertFalse(chunk.isFinished());
        assertEquals("Let me think...", chunk.getReasoningDelta());
    }

    @Test
    public void testInputJsonDeltaConversion() {
        // 测试 input_json_delta 事件转换
        AnthropicContentBlockDeltaEvent deltaEvent = new AnthropicContentBlockDeltaEvent();
        deltaEvent.setType("content_block_delta");
        deltaEvent.setIndex(1);
        
        AnthropicDelta delta = new AnthropicDelta();
        delta.setType("input_json_delta");
        delta.setPartialJson("{\"query\":\"weather\"");
        deltaEvent.setDelta(delta);
        
        UnifiedStreamChunk chunk = transform.vendorStreamEventToUnified(deltaEvent);
        
        assertNotNull(chunk);
        assertFalse(chunk.isFinished());
        assertNotNull(chunk.getToolCallDeltas());
        assertEquals(1, chunk.getToolCallDeltas().size());
    }

    @Test
    public void testSignatureDeltaIsIgnored() {
        // 测试 signature_delta 事件被忽略
        AnthropicContentBlockDeltaEvent deltaEvent = new AnthropicContentBlockDeltaEvent();
        deltaEvent.setType("content_block_delta");
        deltaEvent.setIndex(0);
        
        AnthropicDelta delta = new AnthropicDelta();
        delta.setType("signature_delta");
        delta.setSignature("EqQBCgIYAhIM1g...");
        deltaEvent.setDelta(delta);
        
        UnifiedStreamChunk chunk = transform.vendorStreamEventToUnified(deltaEvent);
        
        // signature_delta 应该被忽略，返回 null
        assertNull(chunk);
    }

    @Test
    public void testPingEventIsIgnored() {
        // 测试 ping 事件被转换为空的 UnifiedStreamChunk
        AnthropicPingEvent pingEvent = new AnthropicPingEvent();
        pingEvent.setType("ping");
        
        UnifiedStreamChunk chunk = transform.vendorStreamEventToUnified(pingEvent);
        
        // ping 事件应该被转换为空的 UnifiedStreamChunk
        assertNotNull(chunk);
        assertFalse(chunk.isFinished());
        assertNull(chunk.getContentDelta());
        assertNull(chunk.getReasoningDelta());
        assertNull(chunk.getToolCallDeltas());
    }

    @Test
    public void testErrorEventIsIgnored() {
        // 测试 error 事件被转换为空的 UnifiedStreamChunk
        AnthropicErrorEvent errorEvent = new AnthropicErrorEvent();
        errorEvent.setType("error");
        
        AnthropicErrorEvent.AnthropicError error = new AnthropicErrorEvent.AnthropicError();
        error.setType("overloaded_error");
        error.setMessage("Overloaded");
        errorEvent.setError(error);
        
        UnifiedStreamChunk chunk = transform.vendorStreamEventToUnified(errorEvent);
        
        // error 事件应该被转换为空的 UnifiedStreamChunk（错误处理应该在其他层处理）
        assertNotNull(chunk);
        assertFalse(chunk.isFinished());
        assertNull(chunk.getContentDelta());
        assertNull(chunk.getReasoningDelta());
        assertNull(chunk.getToolCallDeltas());
    }

    @Test
    public void testMessageStopEvent() {
        // 测试 message_stop 事件转换
        AnthropicMessageStopEvent stopEvent = new AnthropicMessageStopEvent();
        stopEvent.setType("message_stop");
        
        UnifiedStreamChunk chunk = transform.vendorStreamEventToUnified(stopEvent);
        
        assertNotNull(chunk);
        assertTrue(chunk.isFinished());
        assertNotNull(chunk.getChoices());
        assertEquals("stop", chunk.getChoices().get(0).getFinishReason());
    }
    
    @Test
    public void testContentBlockStopEvent() {
        // 测试 content_block_stop 事件被转换为空的 UnifiedStreamChunk
        AnthropicContentBlockStopEvent stopEvent = new AnthropicContentBlockStopEvent();
        stopEvent.setType("content_block_stop");
        stopEvent.setIndex(0);
        
        UnifiedStreamChunk chunk = transform.vendorStreamEventToUnified(stopEvent);
        
        // content_block_stop 事件应该被转换为空的 UnifiedStreamChunk
        assertNotNull(chunk);
        assertFalse(chunk.isFinished());
        assertNull(chunk.getContentDelta());
        assertNull(chunk.getReasoningDelta());
        assertNull(chunk.getToolCallDeltas());
    }
}

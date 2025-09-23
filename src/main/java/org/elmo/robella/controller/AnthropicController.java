package org.elmo.robella.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.context.RequestContextHolder;
import org.elmo.robella.context.RequestContextHolder.RequestContext;
import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.anthropic.model.AnthropicModelInfo;
import org.elmo.robella.model.anthropic.model.AnthropicModelListResponse;
import org.elmo.robella.model.anthropic.stream.AnthropicStreamEvent;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.elmo.robella.service.UnifiedService;
import org.elmo.robella.service.stream.UnifiedToEndpointStreamTransformer;
import org.elmo.robella.service.transform.EndpointTransform;
import org.elmo.robella.util.JsonUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * Anthropic Messages API 控制器
 * 提供与 Anthropic 原生 API 兼容的接口
 */
@RestController
@RequestMapping("/anthropic/v1")
@RequiredArgsConstructor
@Slf4j
public class AnthropicController {

    private final UnifiedService unifiedService;
    private final EndpointTransform<AnthropicChatRequest, AnthropicMessage> anthropicEndpointTransform;
    private final UnifiedToEndpointStreamTransformer<AnthropicStreamEvent> unifiedToAnthropicStreamTransformer;
    private final JsonUtils jsonUtils;
    /**
     * Anthropic Messages API 端点
     *
     * @param request Anthropic 格式的聊天请求
     * @return 流式或非流式响应
     */
    @PostMapping(value = "/messages",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE},
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object createMessage(@RequestBody @Valid AnthropicChatRequest request, HttpServletResponse response) {

        RequestContext ctx = RequestContextHolder.getContext();
        String requestId = ctx.getRequestId();
        ctx.setEndpointType("anthropic");

        UnifiedChatRequest unifiedRequest = anthropicEndpointTransform.endpointToUnifiedRequest(request);
        unifiedRequest.setEndpointType("anthropic");

        if (Boolean.TRUE.equals(request.getStream())) {
            return handleStreamingResponse(unifiedRequest, requestId, response);
        } else {
            return handleNormalResponse(unifiedRequest);
        }
    }

    private ResponseEntity<AnthropicMessage> handleNormalResponse(UnifiedChatRequest unifiedRequest) {
        UnifiedChatResponse response = unifiedService.sendChatRequest(unifiedRequest);
        AnthropicMessage anthropicResponse = anthropicEndpointTransform.unifiedToEndpointResponse(response);
        return ResponseEntity.ok().body(anthropicResponse);
    }

    private SseEmitter handleStreamingResponse(UnifiedChatRequest unifiedRequest, String requestId, HttpServletResponse response) {
        SseEmitter emitter = new SseEmitter(30000000000L);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader(HttpHeaders.CONNECTION, "keep-alive");
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);

        // 捕获当前线程的上下文
        RequestContext currentContext = RequestContextHolder.getContext();

        // 使用虚拟线程异步处理SSE发送
        Thread.ofVirtual().name("sse-sender-" + requestId).start(() -> {
            try {
                // 在虚拟线程中设置上下文
                if (currentContext != null) {
                    RequestContextHolder.setContext(currentContext);
                }

                // 获取流式响应数据
                Stream<AnthropicStreamEvent> anthropicStream = unifiedToAnthropicStreamTransformer.transform(unifiedService.sendStreamRequest(unifiedRequest), requestId);
                
                // 异步发送SSE数据
                sendAnthropicSseDataAsync(emitter, anthropicStream);

            } catch (Exception e) {
                log.error("Error in streaming response", e);
                try {
                    emitter.send(SseEmitter.event().data("[ERROR]"));
                    emitter.completeWithError(e);
                } catch (IOException ioException) {
                    log.error("Error sending error event", ioException);
                }
            } finally {
                // 清理虚拟线程中的上下文
                RequestContextHolder.clear();
            }
        });

        return emitter;
    }

    /**
     * 异步发送Anthropic SSE数据
     */
    private void sendAnthropicSseDataAsync(SseEmitter emitter, Stream<AnthropicStreamEvent> anthropicStream) {
        try {
            anthropicStream.forEach(event -> {
                try {
                    // 使用SseEmitter的专用API发送带事件类型的消息
                    String eventType = extractEventType(event);
                    String eventData = jsonUtils.toJson(event);
                    emitter.send(SseEmitter.event().name(eventType).data(eventData));
                } catch (IOException e) {
                    log.error("Error sending SSE event", e);
                    emitter.completeWithError(e);
                }
            });

            // 发送完成标记
            emitter.complete();

        } catch (Exception e) {
            log.error("Error in async SSE sending", e);
            emitter.completeWithError(e);
        }
    }

    /**
     * 获取可用模型列表
     */
    @GetMapping("/models")
    public ResponseEntity<AnthropicModelListResponse> listModels() {
        ModelListResponse openaiResponse = unifiedService.listModels();
        AnthropicModelListResponse response = convertToAnthropicModelList(openaiResponse);
        return ResponseEntity.ok().body(response);
    }

    /**
     * 将OpenAI格式的模型列表转换为Anthropic格式
     */
    private AnthropicModelListResponse convertToAnthropicModelList(ModelListResponse openaiResponse) {
        var anthropicModels = openaiResponse.getData().stream()
                .map(model -> new AnthropicModelInfo(model.getId(), model.getId()))
                .toList();

        return new AnthropicModelListResponse(anthropicModels);
    }

    /**
     * 从事件对象中提取事件类型
     */
    private String extractEventType(Object event) {
        if (event == null) {
            return "unknown";
        }

        String className = event.getClass().getSimpleName();
        // 将类名转换为事件类型
        switch (className) {
            case "AnthropicMessageStartEvent":
                return "message_start";
            case "AnthropicContentBlockStartEvent":
                return "content_block_start";
            case "AnthropicContentBlockDeltaEvent":
                return "content_block_delta";
            case "AnthropicContentBlockStopEvent":
                return "content_block_stop";
            case "AnthropicMessageDeltaEvent":
                return "message_delta";
            case "AnthropicMessageStopEvent":
                return "message_stop";
            case "AnthropicPingEvent":
                return "ping";
            case "AnthropicErrorEvent":
                return "error";
            default:
                // 尝试从对象属性中获取类型
                try {
                    var typeField = event.getClass().getDeclaredField("type");
                    typeField.setAccessible(true);
                    Object typeValue = typeField.get(event);
                    return typeValue != null ? typeValue.toString() : "unknown";
                } catch (Exception e) {
                    log.debug("无法从事件对象中提取类型: {}", className);
                    return "unknown";
                }
        }
    }
}
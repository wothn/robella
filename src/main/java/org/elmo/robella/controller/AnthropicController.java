package org.elmo.robella.controller;

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
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.elmo.robella.service.UnifiedService;
import org.elmo.robella.service.stream.UnifiedToEndpointStreamTransformer;
import org.elmo.robella.service.transform.EndpointTransform;
import org.elmo.robella.util.JsonUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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

    /**
     * Anthropic Messages API 端点
     *
     * @param request Anthropic 格式的聊天请求
     * @return 流式或非流式响应
     */
    @PostMapping(value = "/messages",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE},
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createMessage(@RequestBody @Valid AnthropicChatRequest request) {

        RequestContext ctx = RequestContextHolder.getContext();
        String requestId = ctx.getRequestId();
        ctx.setEndpointType("anthropic");

        UnifiedChatRequest unifiedRequest = anthropicEndpointTransform.endpointToUnifiedRequest(request);
        unifiedRequest.setEndpointType("anthropic");

        if (Boolean.TRUE.equals(request.getStream())) {
            Stream<UnifiedStreamChunk> unifiedStream = unifiedService.sendStreamRequest(unifiedRequest);

            Stream<String> sseStream = unifiedToAnthropicStreamTransformer.transform(unifiedStream, requestId)
                    .map(event -> {
                        String eventType = extractEventType(event);
                        String eventData = JsonUtils.toJson(event);
                        return "event: " + eventType + "\ndata: " + eventData + "\n\n";
                    });

            StreamingResponseBody body = os -> {
                sseStream.forEach(s -> {
                    try {
                        os.write(s.getBytes());
                        os.flush();
                    } catch (IOException e) {
                        log.error("Error writing to output stream", e);
                    }
                });
            };

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .body(body);

        } else {
            UnifiedChatResponse response = unifiedService.sendChatRequest(unifiedRequest);
            AnthropicMessage anthropicResponse = anthropicEndpointTransform.unifiedToEndpointResponse(response);
            return ResponseEntity.ok().body(anthropicResponse);
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

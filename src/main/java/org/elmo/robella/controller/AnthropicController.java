package org.elmo.robella.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.anthropic.model.AnthropicModelInfo;
import org.elmo.robella.model.anthropic.model.AnthropicModelListResponse;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.elmo.robella.service.UnifiedService;
import org.elmo.robella.service.RoutingService;
import org.elmo.robella.service.stream.UnifiedToEndpointStreamTransformer;
import org.elmo.robella.service.transform.VendorTransform;
import org.elmo.robella.util.JsonUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    private final RoutingService routingService;
    private final VendorTransform<AnthropicChatRequest, AnthropicMessage> anthropicTransform;
    private final UnifiedToEndpointStreamTransformer<Object> unifiedToAnthropicStreamTransformer;

    /**
     * Anthropic Messages API 端点
     *
     * @param request Anthropic 格式的聊天请求
     * @return 流式或非流式响应
     */
    @PostMapping(value = "/messages",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE},
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> createMessage(@RequestBody @Valid AnthropicChatRequest request) {
        String originalModelName = request.getModel();

        // 首先进行模型映射，获取供应商模型名称
        return routingService.mapToVendorModelName(originalModelName)
                .flatMap(vendorModelName -> {
                    // 更新请求中的模型名称为供应商模型名称
                    request.setModel(vendorModelName);

                    // 转换请求为统一格式
                    UnifiedChatRequest unifiedRequest = anthropicTransform.endpointToUnifiedRequest(request);

                    if (Boolean.TRUE.equals(request.getStream())) {
                        // 处理流式响应
                        String uuid = UUID.randomUUID().toString();
                        Flux<ServerSentEvent<String>> sseStream = unifiedToAnthropicStreamTransformer.transform(
                                        unifiedService.sendStreamRequest(unifiedRequest), uuid)
                                .mapNotNull(event -> {
                                    String eventType = extractEventType(event);
                                    String eventData = JsonUtils.toJson(event);
                                    return ServerSentEvent.<String>builder()
                                            .event(eventType)
                                            .data(eventData)
                                            .build();
                                });
                        return Mono.just(ResponseEntity.ok()
                                .contentType(MediaType.TEXT_EVENT_STREAM)
                                .body(sseStream)
                        );
                    } else {
                        // 处理非流式响应
                        return unifiedService.sendChatRequest(unifiedRequest)
                                .map(response -> ResponseEntity.ok().body(response));
                    }
                });
    }

    /**
     * 获取可用模型列表
     */
    @GetMapping("/models")
    public Mono<ResponseEntity<AnthropicModelListResponse>> listModels() {
        return unifiedService.listModels()
                .map(this::convertToAnthropicModelList)
                .map(response -> ResponseEntity.ok().body(response));
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

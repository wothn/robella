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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
                        return Mono.just(ResponseEntity.ok()
                                .contentType(MediaType.TEXT_EVENT_STREAM)
                                .body(unifiedToAnthropicStreamTransformer.transform(unifiedService.sendStreamRequest(unifiedRequest), uuid))
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
}

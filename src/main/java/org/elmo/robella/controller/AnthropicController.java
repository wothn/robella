package org.elmo.robella.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.model.AnthropicModelInfo;
import org.elmo.robella.model.anthropic.model.AnthropicModelListResponse;
import org.elmo.robella.model.common.EndpointType;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.elmo.robella.service.UnifiedService;
import org.elmo.robella.service.RoutingService;
import org.elmo.robella.service.transform.VendorTransformFactory;
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

    private final UnifiedService forwardingService;
    private final RoutingService routingService;
    private final VendorTransformFactory vendorTransformFactory;

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
                    UnifiedChatRequest unifiedRequest = vendorTransformFactory.vendorRequestToUnified(EndpointType.Anthropic, request);
                    
                    if (Boolean.TRUE.equals(request.getStream())) {
                        // 处理流式响应
                        return Mono.just(ResponseEntity.ok()
                                .contentType(MediaType.TEXT_EVENT_STREAM)
                                .body(forwardingService.streamUnified(unifiedRequest, EndpointType.Anthropic)));
                    } else {
                        // 处理非流式响应
                        return forwardingService.forwardUnified(unifiedRequest, EndpointType.Anthropic)
                                .map(response -> ResponseEntity.ok().body(response));
                    }
                });
    }

    /**
     * 获取可用模型列表
     */
    @GetMapping("/models")
    public Mono<ResponseEntity<AnthropicModelListResponse>> listModels() {
        return forwardingService.listModels()
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
     * 从JSON数据中提取事件类型
     */
    private String extractEventType(String jsonData) {
        try {
            // 简单解析JSON获取type字段
            int typeIndex = jsonData.indexOf("\"type\"");
            if (typeIndex == -1) {
                return "message"; // 默认事件类型
            }

            // 找到type字段的值
            int colonIndex = jsonData.indexOf(":", typeIndex);
            if (colonIndex == -1) {
                return "message";
            }

            // 找到值的开始引号
            int startQuote = jsonData.indexOf("\"", colonIndex);
            if (startQuote == -1) {
                return "message";
            }

            // 找到值的结束引号
            int endQuote = jsonData.indexOf("\"", startQuote + 1);
            if (endQuote == -1) {
                return "message";
            }

            String eventType = jsonData.substring(startQuote + 1, endQuote);

            // 验证是否是有效的Anthropic事件类型
            if (isValidAnthropicEventType(eventType)) {
                return eventType;
            } else {
                return "message";
            }
        } catch (Exception e) {
            return "message"; // 默认事件类型
        }
    }

    /**
     * 验证是否是有效的Anthropic事件类型
     */
    private boolean isValidAnthropicEventType(String eventType) {
        return switch (eventType) {
            case "message_start",
                 "message_delta",
                 "message_stop",
                 "content_block_start",
                 "content_block_delta",
                 "content_block_stop",
                 "ping" -> true;
            default -> false;
        };
    }
}

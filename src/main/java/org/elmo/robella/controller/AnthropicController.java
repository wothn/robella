package org.elmo.robella.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.config.ProviderType;
import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.anthropic.model.AnthropicModelInfo;
import org.elmo.robella.model.anthropic.model.AnthropicModelListResponse;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.elmo.robella.model.openai.model.ModelInfo;
import org.elmo.robella.service.ForwardingService;
import org.elmo.robella.service.transform.TransformService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

/**
 * Anthropic Messages API 控制器
 * 提供与 Anthropic 原生 API 兼容的接口
 */
@Slf4j
@RestController
@RequestMapping("/anthropic/v1")
@RequiredArgsConstructor
public class AnthropicController {

    private final ForwardingService forwardingService;
    private final TransformService transformService;

    /**
     * Anthropic Messages API 端点
     *
     * @param request Anthropic 格式的聊天请求
     * @return 流式或非流式响应
     */
    @PostMapping(value = "/messages",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE},
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object createMessage(@RequestBody @Valid AnthropicChatRequest request) {
        log.debug("收到 Anthropic Messages API 请求: model={}, stream={}",
                request.getModel(), request.getStream());

        // 将 Anthropic 请求转换为内部统一格式（使用端点格式）
        UnifiedChatRequest unified = transformService.endpointRequestToUnified(request, ProviderType.Anthropic.getName());
        boolean stream = Boolean.TRUE.equals(request.getStream());

        if (stream) {
            // 使用带端点族转换的流式接口
            var rawFlux = forwardingService.streamUnified(unified, null, ProviderType.Anthropic.getName());

            Flux<ServerSentEvent<String>> sseFlux = rawFlux.map(eventData -> {
                // 解析eventData获取事件类型
                String eventType = extractEventType(eventData);
                if (isValidAnthropicEventType(eventType)) {
                    return ServerSentEvent.builder(eventData)
                            .event(eventType)
                            .build();
                }

                return ServerSentEvent.builder(eventData)
                        .event(eventType)
                        .build();
            });

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(sseFlux
                            .doOnNext(event -> log.trace("发送流式事件: {}",
                                    event.toString().length() > 200 ? event.toString().substring(0, 200) + "..." : event.toString()))
                            .doOnComplete(() -> log.debug("流式响应完成"))
                            .doOnError(error -> log.error("流式响应错误", error)));
        } else {
            // 非流式响应
            log.debug("处理非流式请求");
            return forwardingService.forwardUnified(unified, null)
                    .map(unifiedResponse -> {
                        // 将统一响应转换回 Anthropic 格式（无论后端provider是什么）
                        AnthropicMessage response = (AnthropicMessage) transformService
                                .unifiedToEndpointResponse(unifiedResponse, ProviderType.Anthropic.getName());
                        log.debug("返回 Anthropic 响应: id={}, model={}",
                                response.getId(), response.getModel());
                        return ResponseEntity.ok(response);
                    })
                    .doOnError(error -> log.error("处理 Anthropic 请求失败", error));
        }
    }

    /**
     * 获取可用模型列表
     */
    @GetMapping("/models")
    public Mono<ResponseEntity<AnthropicModelListResponse>> listModels() {
        log.debug("获取 Anthropic 模型列表");
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
     * 健康检查端点，用于验证 Anthropic 服务可用性
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<String>> health() {
        log.debug("Anthropic 健康检查");
        return Mono.just(ResponseEntity.ok("Anthropic API is healthy"));
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
                log.debug("无效的事件类型: {}, 使用默认值", eventType);
                return "message";
            }
        } catch (Exception e) {
            log.debug("提取事件类型失败，使用默认值: {}", e.getMessage());
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
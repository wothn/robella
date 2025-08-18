package org.elmo.robella.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.config.ProviderType;
import org.elmo.robella.model.anthropic.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.AnthropicChatResponse;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.service.ForwardingService;
import org.elmo.robella.service.TransformService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Anthropic Messages API 控制器
 * 提供与 Anthropic 原生 API 兼容的接口
 */
@Slf4j
@RestController
@RequestMapping("/v1")
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
            // 流式响应
            log.debug("处理流式请求");
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(
                            forwardingService.streamUnified(unified, null)
                                    .map(chunk -> {
                                        // 将统一流式片段转换回 Anthropic 格式（无论后端provider是什么）
                                        Object event = transformService.unifiedStreamChunkToEndpoint(
                                                chunk, ProviderType.Anthropic.getName());
                                        return event != null ? event : "";
                                    })
                                    .filter(event -> !"".equals(event)) // 过滤空事件
                                    .doOnNext(event -> log.trace("发送流式事件: {}", event))
                                    .doOnComplete(() -> log.debug("流式响应完成"))
                                    .doOnError(error -> log.error("流式响应错误", error))
                    );
        } else {
            // 非流式响应
            log.debug("处理非流式请求");
            return forwardingService.forwardUnified(unified, null)
                    .map(unifiedResponse -> {
                        // 将统一响应转换回 Anthropic 格式（无论后端provider是什么）
                        AnthropicChatResponse response = (AnthropicChatResponse) transformService
                                .unifiedToEndpointResponse(unifiedResponse, ProviderType.Anthropic.getName());
                        log.debug("返回 Anthropic 响应: id={}, model={}", 
                                 response.getId(), response.getModel());
                        return ResponseEntity.ok(response);
                    })
                    .doOnError(error -> log.error("处理 Anthropic 请求失败", error));
        }
    }

    /**
     * 健康检查端点，用于验证 Anthropic 服务可用性
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<String>> health() {
        log.debug("Anthropic 健康检查");
        return Mono.just(ResponseEntity.ok("Anthropic API is healthy"));
    }
}

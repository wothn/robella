package org.elmo.robella.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.elmo.robella.service.UnifiedService;
import org.elmo.robella.service.stream.UnifiedToEndpointTransformer;
import org.elmo.robella.service.RoutingService;
import org.elmo.robella.service.transform.OpenAITransform;
import org.elmo.robella.model.common.EndpointType;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OpenAIController {

    private final UnifiedService unifiedService;
    private final RoutingService routingService;
    private final VendorTransform openAITransform; 
    private final UnifiedToEndpointTransformer<String> endpointTransformer;

    @PostMapping(value = "/chat/completions", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE })
    public Mono<ResponseEntity<?>> chatCompletions(@RequestBody @Valid ChatCompletionRequest request) {
        String originalModelName = request.getModel();

        // 首先进行模型映射，获取供应商模型名称
        return routingService.mapToVendorModelName(originalModelName)
                .flatMap(vendorModelName -> {
                    // 更新请求中的模型名称为供应商模型名称
                    request.setModel(vendorModelName);
                    
                    // 直接使用OpenAI转换器进行统一处理
                    UnifiedChatRequest unifiedRequest = openAITransform.vendorRequestToUnified(request);

                    if (Boolean.TRUE.equals(request.getStream())) {
                        return Mono.just(ResponseEntity.ok()
                                .contentType(MediaType.TEXT_EVENT_STREAM)
                                .body(unifiedService.streamUnified(unifiedRequest, EndpointType.OpenAI)));
                    } else {
                        return unifiedService.forwardUnified(unifiedRequest, EndpointType.OpenAI)
                                .map(response -> ResponseEntity.ok().body(response));
                    }
                });
    }

    @GetMapping("/models")
    public Mono<ResponseEntity<ModelListResponse>> listModels() {
        return unifiedService.listModels()
                .map(response -> ResponseEntity.ok().body(response));
    }
}
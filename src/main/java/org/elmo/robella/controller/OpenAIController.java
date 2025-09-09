package org.elmo.robella.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.elmo.robella.service.ForwardingService;
import org.elmo.robella.service.transform.VendorTransformFactory;
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

    private final ForwardingService forwardingService;
    private final VendorTransformFactory vendorTransformFactory;

    @PostMapping(value = "/chat/completions", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Mono<ResponseEntity<?>> chatCompletions(@RequestBody @Valid ChatCompletionRequest request) {
        // 转换请求为统一格式
        UnifiedChatRequest unifiedRequest = vendorTransformFactory.vendorRequestToUnified(EndpointType.OpenAI, request);
        
        if (Boolean.TRUE.equals(request.getStream())) {
            // 处理流式响应
            return Mono.just(ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(forwardingService.streamUnified(unifiedRequest, EndpointType.OpenAI)));
        } else {
            // 处理非流式响应
            return forwardingService.forwardUnified(unifiedRequest, EndpointType.OpenAI)
                    .map(response -> ResponseEntity.ok().body(response));
        }
    }

    @GetMapping("/models")
    public Mono<ResponseEntity<ModelListResponse>> listModels() {
        return forwardingService.listModels()
                .map(response -> ResponseEntity.ok().body(response));
    }
}
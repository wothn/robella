package org.elmo.robella.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.elmo.robella.model.openai.stream.ChatCompletionChunk;
import org.elmo.robella.service.UnifiedService;
import org.elmo.robella.service.stream.UnifiedToEndpointStreamTransformer;
import org.elmo.robella.service.RoutingService;
import org.elmo.robella.service.transform.EndpointTransform;
import org.elmo.robella.util.JsonUtils;

import java.util.UUID;

import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OpenAIController {

    private final UnifiedService unifiedService;
    private final RoutingService routingService;
    private final EndpointTransform<ChatCompletionRequest, ChatCompletionResponse> openAIEndpointTransform;
    private final UnifiedToEndpointStreamTransformer<ChatCompletionChunk> unifiedToOpenAIStreamTransformer;

    @PostMapping(value = "/chat/completions", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE })
    public Mono<ResponseEntity<?>> chatCompletions(@RequestBody @Valid ChatCompletionRequest request) {
        String originalModelName = request.getModel();

        // 使用新的路由方法，一次性获取客户端和供应商信息
        return Mono.deferContextual(ctx -> {
            String requestId = ctx.getOrDefault("requestId", UUID.randomUUID().toString());

            return routingService.routeAndClient(originalModelName)
                    .flatMap(clientWithProvider -> {
                        // 直接使用OpenAI转换器进行统一处理
                        UnifiedChatRequest unifiedRequest = openAIEndpointTransform.endpointToUnifiedRequest(request);
                        unifiedRequest.setEndpointType("openai");
                        // 使用供应商模型调用标识
                        unifiedRequest.setModel(clientWithProvider.getVendorModel().getVendorModelKey());

                        if (Boolean.TRUE.equals(request.getStream())) {
                            Flux<String> sseStream = unifiedToOpenAIStreamTransformer.transform(
                                unifiedService.sendStreamRequestWithClient(unifiedRequest, clientWithProvider)
                                    .contextWrite(innerCtx -> innerCtx
                                        .put("modelKey", originalModelName)
                                        .put("providerId", clientWithProvider.getProvider().getId())
                                        .put("vendorModelKey", clientWithProvider.getVendorModel().getVendorModelKey())
                                        .put("endpointType", "openai")),
                                requestId)
                                .mapNotNull(chunk -> JsonUtils.toJson(chunk))
                                .concatWith(Flux.just("[DONE]"));
                            return Mono.just(ResponseEntity.ok()
                                    .contentType(MediaType.TEXT_EVENT_STREAM)
                                    .body(sseStream)
                            );
                        } else {
                            return unifiedService.sendChatRequestWithClient(unifiedRequest, clientWithProvider)
                                    .contextWrite(innerCtx -> innerCtx
                                        .put("modelKey", originalModelName)
                                        .put("providerId", clientWithProvider.getProvider().getId())
                                        .put("vendorModelKey", clientWithProvider.getVendorModel().getVendorModelKey()))
                                    .map(response -> ResponseEntity.ok().body(response));
                        }
                    });
        });
    }

    @GetMapping("/models")
    public Mono<ResponseEntity<ModelListResponse>> listModels() {
        return unifiedService.listModels()
                .map(response -> ResponseEntity.ok().body(response));
    }
}
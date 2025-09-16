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

        // 首先进行模型映射，获取供应商模型调用标识
        return routingService.mapToVendorModelKey(originalModelName)
                .flatMap(modelKey -> {
                    // 更新请求中的模型名称为供应商模型调用标识
                    request.setModel(modelKey);
                    
                    // 直接使用OpenAI转换器进行统一处理
                    UnifiedChatRequest unifiedRequest = openAIEndpointTransform.endpointToUnifiedRequest(request);

                    if (Boolean.TRUE.equals(request.getStream())) {
                        String uuid = UUID.randomUUID().toString();
                        Flux<String> sseStream = unifiedToOpenAIStreamTransformer.transform(
                            unifiedService.sendStreamRequest(unifiedRequest), uuid)
                            .mapNotNull(chunk -> JsonUtils.toJson(chunk))
                            .concatWith(Flux.just("[DONE]"));
                        return Mono.just(ResponseEntity.ok()
                                .contentType(MediaType.TEXT_EVENT_STREAM)
                                .body(sseStream)
                        );
                    } else {
                        return unifiedService.sendChatRequest(unifiedRequest)
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
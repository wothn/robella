package org.elmo.robella.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.config.ProviderType;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.elmo.robella.service.ForwardingService;
import org.elmo.robella.service.TransformService;
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

    private final ForwardingService forwardingService;
    private final TransformService transformService;

    @PostMapping(value = "/chat/completions", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Mono<ResponseEntity<?>> chatCompletions(@RequestBody @Valid ChatCompletionRequest request) {

        // 将请求转为内部格式（使用端点格式，而非provider类型）
        UnifiedChatRequest unified = transformService.endpointRequestToUnified(request, ProviderType.OpenAI.getName());
        boolean stream = Boolean.TRUE.equals(request.getStream());
        if (stream) {
            Flux<String> sseFlux = forwardingService.streamUnified(unified, null)
                    .mapNotNull(chunk -> {
                        Object event = transformService.unifiedStreamChunkToEndpoint(chunk, ProviderType.OpenAI.getName());
                        return event != null ? event.toString() : "";
                    })
                    .concatWith(Flux.just("[DONE]"));
            return Mono.just(ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(sseFlux));
        }
        return forwardingService.forwardUnified(unified, null)
                .map(u -> (ChatCompletionResponse) transformService.unifiedToEndpointResponse(u, ProviderType.OpenAI.getName()))
                .map(ResponseEntity.ok()::body);
    }

    @GetMapping("/models")
    public Mono<ResponseEntity<ModelListResponse>> listModels() {
        return forwardingService.listModels()
                .map(response -> ResponseEntity.ok().body(response));
    }
}
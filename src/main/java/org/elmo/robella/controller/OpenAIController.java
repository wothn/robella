package org.elmo.robella.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.config.ProviderType;
import org.elmo.robella.model.openai.ChatCompletionRequest;
import org.elmo.robella.model.openai.ModelListResponse;
import org.elmo.robella.model.openai.ChatCompletionResponse;
import org.elmo.robella.service.ForwardingService;
import org.elmo.robella.service.TransformService;
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
    private final TransformService transformService;

    @PostMapping(value = "/chat/completions", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Object chatCompletions(@RequestBody @Valid ChatCompletionRequest request) {

        // 将请求转为内部格式
        UnifiedChatRequest unified = transformService.vendorRequestToUnified(request, ProviderType.OpenAI.getName());
        boolean stream = Boolean.TRUE.equals(request.getStream());
        if (stream) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(
                            forwardingService.streamUnified(unified, null)
                                    .map(chunk -> {
                                        Object event = transformService.unifiedStreamChunkToVendor(chunk, ProviderType.OpenAI.getName());
                                        return event != null ? event : "";
                                    })
                    );
        }
        return forwardingService.forwardUnified(unified, null)
                .map(u -> (ChatCompletionResponse) transformService.getVendorTransform("OpenAI").unifiedToVendorResponse(u));
    }

    @GetMapping("/models")
    public Mono<ResponseEntity<ModelListResponse>> listModels() {
        return forwardingService.listModels()
                .map(response -> ResponseEntity.ok().body(response));
    }
}
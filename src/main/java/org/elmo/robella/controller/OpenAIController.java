package org.elmo.robella.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.request.OpenAIChatRequest;
import org.elmo.robella.model.response.OpenAIModelListResponse;
import org.elmo.robella.service.ForwardingService;
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

    @PostMapping(value = "/chat/completions", produces = { MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_EVENT_STREAM_VALUE })
    public Mono<ResponseEntity<?>> chatCompletions(
            @RequestBody @Valid OpenAIChatRequest request) {

        if (Boolean.TRUE.equals(request.getStream())) {
            // 流式响应
            return Mono.just(ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(forwardingService.streamChatCompletion(request)));
        } else {
            // 非流式响应
            return forwardingService.forwardChatCompletion(request)
                    .map(response -> ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response));
        }
    }

    @GetMapping("/models")
    public Mono<ResponseEntity<OpenAIModelListResponse>> listModels() {
        return forwardingService.listModels()
                .map(response -> ResponseEntity.ok().body(response));
    }
}
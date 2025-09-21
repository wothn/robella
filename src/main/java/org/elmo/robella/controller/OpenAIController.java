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
import org.elmo.robella.service.transform.EndpointTransform;
import org.elmo.robella.util.JsonUtils;

import java.io.IOException;
import java.util.stream.Stream;

import org.elmo.robella.context.RequestContextHolder;
import org.elmo.robella.context.RequestContextHolder.RequestContext;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OpenAIController {

    private final UnifiedService unifiedService;
    private final EndpointTransform<ChatCompletionRequest, ChatCompletionResponse> openAIEndpointTransform;
    private final UnifiedToEndpointStreamTransformer<ChatCompletionChunk> unifiedToOpenAIStreamTransformer;

    @PostMapping(value = "/chat/completions", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE })
    public Void chatCompletions(@RequestBody @Valid ChatCompletionRequest request) {
        RequestContext ctx = RequestContextHolder.getContext();
        String requestId = ctx.getRequestId();
        ctx.setEndpointType("openai");

        UnifiedChatRequest unifiedRequest = openAIEndpointTransform.endpointToUnifiedRequest(request);
        unifiedRequest.setEndpointType("openai");

        if (Boolean.TRUE.equals(request.getStream())) {
            Stream<UnifiedStreamChunk> unifiedStream = unifiedService.sendStreamRequest(unifiedRequest);

            Stream<String> sseStream = unifiedToOpenAIStreamTransformer.transform(unifiedStream, requestId)
                    .map(chunk -> "data: " + JsonUtils.toJson(chunk) + "\n\n");
            
            // 添加完成标识
            Stream<String> completeStream = Stream.concat(sseStream, Stream.of("data: [DONE]\n\n"));

            StreamingResponseBody body = os -> {
                completeStream.forEach(s -> {
                    try {
                        os.write(s.getBytes());
                        os.flush();
                    } catch (IOException e) {
                        log.error("Error writing to output stream", e);
                    }
                });
            };
            
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .body(body);

        } else {
            UnifiedChatResponse response = unifiedService.sendChatRequest(unifiedRequest);
            ChatCompletionResponse chatResponse = openAIEndpointTransform.unifiedToEndpointResponse(response);
            return ResponseEntity.ok().body(chatResponse);
        }
    }

    @GetMapping("/models")
    public ResponseEntity<ModelListResponse> listModels() {
        ModelListResponse response = unifiedService.listModels();
        return ResponseEntity.ok().body(response);
    }
}
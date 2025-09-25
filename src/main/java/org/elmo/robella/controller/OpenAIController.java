package org.elmo.robella.controller;

import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OpenAIController {

    private final UnifiedService unifiedService;
    private final EndpointTransform<ChatCompletionRequest, ChatCompletionResponse> openAIEndpointTransform;
    private final UnifiedToEndpointStreamTransformer<ChatCompletionChunk> unifiedToOpenAIStreamTransformer;
    private final JsonUtils jsonUtils;

    @PostMapping(value = "/chat/completions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object chatCompletions(@RequestBody @Valid ChatCompletionRequest request, HttpServletResponse response) {
        RequestContext ctx = RequestContextHolder.getContext();
        ctx.setEndpointType("openai");

        UnifiedChatRequest unifiedRequest = openAIEndpointTransform.endpointToUnifiedRequest(request);

        // 检查是否为流式请求
        if (Boolean.TRUE.equals(request.getStream())) {
            return handleStreamingResponse(unifiedRequest, ctx.getRequestId(), response);
        } else {
            return handleNormalResponse(unifiedRequest);
        }
    }

    private ResponseEntity<ChatCompletionResponse> handleNormalResponse(UnifiedChatRequest unifiedRequest) {
        UnifiedChatResponse responseObj = unifiedService.sendChatRequest(unifiedRequest);
        ChatCompletionResponse chatResponse = openAIEndpointTransform.unifiedToEndpointResponse(responseObj);
        return ResponseEntity.ok().body(chatResponse);
    }

    private SseEmitter handleStreamingResponse(UnifiedChatRequest unifiedRequest, String requestId, HttpServletResponse response) {
        SseEmitter emitter = new SseEmitter(30000000000L);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader(HttpHeaders.CONNECTION, "keep-alive");
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);

        // 捕获当前线程的上下文
        RequestContext currentContext = RequestContextHolder.getContext();

        // 使用虚拟线程异步处理SSE发送
        Thread.ofVirtual().name("sse-sender-" + requestId).start(() -> {
            try {
                // 在虚拟线程中设置上下文
                if (currentContext != null) {
                    RequestContextHolder.setContext(currentContext);
                }
                
                // 获取流式响应数据
                Stream<UnifiedStreamChunk> unifiedStream = unifiedService.sendStreamRequest(unifiedRequest);
                Stream<String> sseStream = unifiedToOpenAIStreamTransformer.transform(unifiedStream, requestId)
                        .map(jsonUtils::toJson);

                // 异步发送SSE数据
                sendSseDataAsync(emitter, sseStream);

            } catch (Exception e) {
                log.error("Error in streaming response", e);
                emitter.completeWithError(e);
            } finally {
                // 清理虚拟线程中的上下文
                RequestContextHolder.clear();
            }
        });

        return emitter;
    }

    /**
     * 异步发送SSE数据
     */
    private void sendSseDataAsync(SseEmitter emitter, Stream<String> sseStream) {
        try {
            sseStream.forEach(chunk -> {
                try {
                    emitter.send(SseEmitter.event().data(chunk));
                } catch (IOException e) {
                    log.error("Error sending SSE chunk", e);
                    
                    return;
                }
            });

            // 发送完成标记
            emitter.send(SseEmitter.event().data("[DONE]"));
            sseStream.close();
            emitter.complete();

        } catch (Exception e) {
            log.error("Error in async SSE sending", e);
            emitter.completeWithError(e);
        }
    }

    @GetMapping("/models")
    public ResponseEntity<ModelListResponse> listModels() {
        ModelListResponse response = unifiedService.listModels();
        return ResponseEntity.ok().body(response);
    }
}
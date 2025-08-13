package org.elmo.robella.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.anthropic.MessageRequest;
import org.elmo.robella.model.anthropic.MessageResponse;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatMessage;
import org.elmo.robella.model.internal.UnifiedContentPart;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.service.ForwardingService;
import org.elmo.robella.service.TransformService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Anthropic 原生 messages 接口。
 */
@Slf4j
@RestController
@RequestMapping("/anthropic/v1")
@RequiredArgsConstructor
public class AnthropicController {

    private final ForwardingService forwardingService;
    private final TransformService transformService;

    @PostMapping(value = "/messages", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Object messages(@RequestBody MessageRequest request) {
        boolean stream = Boolean.TRUE.equals(request.getStream());
        UnifiedChatRequest unified = fromAnthropicRequest(request);
        if (stream) {
            Flux<String> body = forwardingService.streamUnified(unified, "Anthropic")
                    .map(this::toAnthropicSseEvent)
                    .filter(s -> !s.isEmpty());
            return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(body);
        } else {
            Mono<MessageResponse> mono = forwardingService.forwardUnified(unified, "Anthropic")
                    .map(this::toAnthropicResponse);
            return mono;
        }
    }

    private UnifiedChatRequest fromAnthropicRequest(MessageRequest req) {
        UnifiedChatRequest.UnifiedChatRequestBuilder b = UnifiedChatRequest.builder()
                .model(req.getModel())
                .stream(Boolean.TRUE.equals(req.getStream()))
                .maxTokens(req.getMaxTokens())
                .temperature(req.getTemperature())
                .topP(req.getTopP());
        if (req.getMessages() != null) {
            req.getMessages().forEach(m -> {
                UnifiedChatMessage.UnifiedChatMessageBuilder mb = UnifiedChatMessage.builder().role(m.getRole());
                if (m.getContent()!=null) {
                    m.getContent().forEach(c -> {
                        if ("text".equals(c.getType()) && c.getText()!=null) {
                            mb.content(UnifiedContentPart.text(c.getText()));
                        }
                    });
                }
                b.message(mb.build());
            });
        }
        if (req.getSystem() != null) {
            b.message(UnifiedChatMessage.builder().role("system").content(UnifiedContentPart.text(req.getSystem())).build());
        }
        return b.build();
    }

    private MessageResponse toAnthropicResponse(org.elmo.robella.model.internal.UnifiedChatResponse unified) {
        MessageResponse resp = new MessageResponse();
        resp.setId(unified.getId() != null ? unified.getId() : "msg-" + System.nanoTime());
        resp.setModel(unified.getModel());
        resp.setStopReason(unified.getFinishReason());
        if (unified.getContent() != null) {
            var block = new org.elmo.robella.model.anthropic.MessageResponseContentBlock();
            block.setType("text");
            block.setText(unified.getContent());
            resp.setContent(java.util.List.of(block));
        }
        if (unified.getUsage() != null) {
            var usage = new org.elmo.robella.model.anthropic.MessageUsage();
            usage.setInputTokens(unified.getUsage().getPromptTokens());
            usage.setOutputTokens(unified.getUsage().getCompletionTokens());
            resp.setUsage(usage);
        }
        return resp;
    }

    private String toAnthropicSseEvent(UnifiedStreamChunk chunk) {
        if (chunk.isFinished()) {
            return "event: message_stop\ndata: {}\n";
        }
        if (chunk.getContentDelta() == null || chunk.getContentDelta().isEmpty()) return "";
        String textEscaped = chunk.getContentDelta().replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"");
        String dataJson = "{\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"" + textEscaped + "\"}}";
        return "event: content_block_delta\ndata: " + dataJson + "\n";
    }
}

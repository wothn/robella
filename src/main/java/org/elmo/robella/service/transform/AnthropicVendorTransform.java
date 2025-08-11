package org.elmo.robella.service.transform;

import org.elmo.robella.model.anthropic.*;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.service.VendorTransform;
import org.elmo.robella.util.JsonUtils;

import java.util.*;

/**
 * Anthropic Claude 转换实现。
 */
public class AnthropicVendorTransform implements VendorTransform {
    @Override
    public String type() { return "Anthropic"; }
    @Override
    public UnifiedChatRequest vendorRequestToUnified(Object vendorRequest) {
        if (!(vendorRequest instanceof MessageRequest req)) return null;
        UnifiedChatRequest.UnifiedChatRequestBuilder b = UnifiedChatRequest.builder()
                .model(req.getModel())
                .stream(Boolean.TRUE.equals(req.getStream()))
                .maxTokens(req.getMaxTokens())
                .temperature(req.getTemperature())
                .topP(req.getTopP());
        if (req.getMessages() != null) {
            req.getMessages().forEach(m -> {
                StringBuilder sb = new StringBuilder();
                if (m.getContent() != null) {
                    m.getContent().forEach(c -> {
                        if ("text".equals(c.getType()) && c.getText() != null) sb.append(c.getText());
                    });
                }
                b.message(UnifiedChatRequest.Message.builder().role(m.getRole()).content(sb.toString()).build());
            });
        }
        if (req.getSystem() != null) {
            b.message(UnifiedChatRequest.Message.builder().role("system").content(req.getSystem()).build());
        }
        return b.build();
    }

    @Override
    public Object unifiedToVendorRequest(UnifiedChatRequest unifiedRequest) {
        List<AnthropicMessage> msgs = new ArrayList<>();
        StringBuilder system = new StringBuilder();
        if (unifiedRequest.getMessages() != null) {
            for (UnifiedChatRequest.Message m : unifiedRequest.getMessages()) {
                if ("system".equalsIgnoreCase(m.getRole())) {
                    if (!system.isEmpty()) system.append("\n\n");
                    system.append(m.getContent());
                } else {
                    AnthropicMessage am = new AnthropicMessage();
                    am.setRole(mapRole(m.getRole()));
                    AnthropicContentBlock block = AnthropicContentBlock.builder().type("text").text(m.getContent()).build();
                    am.setContent(List.of(block));
                    msgs.add(am);
                }
            }
        }
        return MessageRequest.builder()
                .model(unifiedRequest.getModel())
                .messages(msgs)
                .system(system.isEmpty() ? null : system.toString())
                .maxTokens(Optional.ofNullable(unifiedRequest.getMaxTokens()).orElse(1024))
                .stream(Boolean.TRUE.equals(unifiedRequest.getStream()))
                .temperature(unifiedRequest.getTemperature())
                .topP(unifiedRequest.getTopP())
                .build();
    }

    private String mapRole(String role) {
        return switch (role == null ? "" : role) {
            case "assistant", "user" -> role;
            default -> "user";
        };
    }

    @Override
    public UnifiedChatResponse vendorResponseToUnified(Object vendorResponse) {
        if (!(vendorResponse instanceof MessageResponse mr)) return null;
        StringBuilder sb = new StringBuilder();
        if (mr.getContent() != null) {
            for (MessageResponseContentBlock b : mr.getContent()) {
                if (b != null && "text".equals(b.getType()) && b.getText() != null) sb.append(b.getText());
            }
        }
        UnifiedChatResponse.Usage usage = null;
        if (mr.getUsage() != null) {
            usage = UnifiedChatResponse.Usage.builder()
                    .promptTokens(Optional.ofNullable(mr.getUsage().getInputTokens()).orElse(0))
                    .completionTokens(Optional.ofNullable(mr.getUsage().getOutputTokens()).orElse(0))
                    .totalTokens(Optional.ofNullable(mr.getUsage().getInputTokens()).orElse(0) + Optional.ofNullable(mr.getUsage().getOutputTokens()).orElse(0))
                    .build();
        }
        return UnifiedChatResponse.builder()
                .id(mr.getId())
                .model(mr.getModel())
                .content(sb.toString())
                .finishReason(mr.getStopReason())
                .usage(usage)
                .rawVendor(mr)
                .build();
    }

    @Override
    public Object unifiedToVendorResponse(UnifiedChatResponse unifiedResponse) {
        if (unifiedResponse == null) return null;
        MessageResponse resp = new MessageResponse();
        resp.setId(unifiedResponse.getId() != null ? unifiedResponse.getId() : "msg-" + System.nanoTime());
        resp.setModel(unifiedResponse.getModel());
        resp.setStopReason(unifiedResponse.getFinishReason());
        if (unifiedResponse.getContent() != null) {
            MessageResponseContentBlock b = new MessageResponseContentBlock();
            b.setType("text");
            b.setText(unifiedResponse.getContent());
            resp.setContent(List.of(b));
        }
        if (unifiedResponse.getUsage() != null) {
            MessageUsage u = new MessageUsage();
            u.setInputTokens(unifiedResponse.getUsage().getPromptTokens());
            u.setOutputTokens(unifiedResponse.getUsage().getCompletionTokens());
            resp.setUsage(u);
        }
        return resp;
    }

    @Override
    public UnifiedStreamChunk vendorStreamEventToUnified(Object vendorEvent) {
        if (vendorEvent == null) return null;
        String eventStr = vendorEvent.toString();
        try {
            String[] lines = eventStr.split("\n");
            String eventType = null;
            String dataJson = null;
            for (String line : lines) {
                if (line.startsWith("event:")) eventType = line.substring(6).trim();
                else if (line.startsWith("data:")) dataJson = line.substring(5).trim();
            }
            if (eventType == null) return null;
            if ("content_block_delta".equals(eventType) && dataJson != null) {
                Map<?, ?> map = JsonUtils.fromJson(dataJson, Map.class);
                if (map != null && map.get("delta") instanceof Map<?, ?> delta) {
                    Object text = delta.get("text");
                    if (text != null) return new UnifiedStreamChunk(text.toString(), false);
                }
            }
            if ("message_stop".equals(eventType)) return new UnifiedStreamChunk(null, true);
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public Object unifiedStreamChunkToVendor(UnifiedStreamChunk chunk) {
        if (chunk == null) return null;
        if (chunk.isFinished()) return "event: message_stop\ndata: {}\n";
        if (chunk.getContentDelta() == null || chunk.getContentDelta().isEmpty()) return "";
        String textEscaped = chunk.getContentDelta().replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"");
        String dataJson = "{\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"" + textEscaped + "\"}}";
        return "event: content_block_delta\ndata: " + dataJson + "\n";
    }
}

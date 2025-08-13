package org.elmo.robella.service.transform;

import org.elmo.robella.model.anthropic.*;
import org.elmo.robella.model.internal.*;
import org.elmo.robella.service.VendorTransform;
import org.elmo.robella.util.JsonUtils;

import java.util.*;

/**
 * Anthropic Claude 转换实现。
 */
public class AnthropicVendorTransform implements VendorTransform {
    @Override
    public String type() {
        return "Anthropic";
    }

    @Override
    public UnifiedChatRequest vendorRequestToUnified(Object vendorRequest) {
        if (!(vendorRequest instanceof MessageRequest req)) return null;
    UnifiedChatRequest.UnifiedChatRequestBuilder b = UnifiedChatRequest.builder()
        .model(req.getModel())
        .stream(Boolean.TRUE.equals(req.getStream()))
        .maxTokens(req.getMaxTokens())
        .temperature(req.getTemperature())
        .topP(req.getTopP()); // topK / minP / seed 暂无（Claude有top_k待扩展）
        if (req.getMessages() != null) {
            req.getMessages().forEach(m -> {
                // 修复 builder 类型
                    var mb = UnifiedChatMessage.builder().role(m.getRole());
                if (m.getContent() != null) {
                    m.getContent().forEach(c -> {
                        if ("text".equals(c.getType()) && c.getText() != null) {
                            mb.content(UnifiedContentPart.text(c.getText()));
                        }
                    });
                }
                b.message(mb.build());
            });
        }
        if (req.getSystem() != null) {
            b.message(UnifiedChatMessage.builder()
                    .role("system")
                    .content(UnifiedContentPart.text(req.getSystem()))
                    .build());
        }
        return b.build();
    }

    @Override
    public Object unifiedToVendorRequest(UnifiedChatRequest unifiedRequest) {
        List<AnthropicMessage> msgs = new ArrayList<>();
        StringBuilder system = new StringBuilder();
        if (unifiedRequest.getMessages() != null) {
            for (UnifiedChatMessage m : unifiedRequest.getMessages()) {
                if ("system".equalsIgnoreCase(m.getRole())) {
                    if (!system.isEmpty()) system.append("\n\n");
                    system.append(m.aggregatedText());
                } else {
                    AnthropicMessage am = new AnthropicMessage();
                    am.setRole(mapRole(m.getRole()));
                    String text = m.aggregatedText();
                    AnthropicContentBlock block = AnthropicContentBlock.builder().type("text").text(text).build();
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
        UnifiedChatMessage assistantMessage = null;
    List<UnifiedChatMessage> messages = new ArrayList<>();
        if (mr.getContent() != null) {
            var mb = UnifiedChatMessage.builder().role("assistant");
            for (MessageResponseContentBlock b : mr.getContent()) {
                if (b != null && "text".equals(b.getType()) && b.getText() != null) {
                    sb.append(b.getText());
                    mb.content(UnifiedContentPart.text(b.getText()));
                }
            }
            assistantMessage = mb.build();
            messages.add(assistantMessage);
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
                .assistantMessage(assistantMessage)
                .messages(messages.isEmpty()?null:messages)
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
                    if (text != null)
                        return UnifiedStreamChunk.builder().contentDelta(text.toString()).finished(false).build();
                }
            }
            if ("message_stop".equals(eventType)) return UnifiedStreamChunk.builder().finished(true).build();
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

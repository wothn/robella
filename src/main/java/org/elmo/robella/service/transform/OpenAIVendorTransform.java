package org.elmo.robella.service.transform;

import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.openai.*;
import org.elmo.robella.service.VendorTransform;
import org.elmo.robella.util.JsonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI 及 OpenAI 兼容（DeepSeek、OpenRouter、Azure OpenAI）转换实现。
 */
public class OpenAIVendorTransform implements VendorTransform {
    @Override
    public String type() { return "OpenAI"; }
    @Override
    public UnifiedChatRequest vendorRequestToUnified(Object vendorRequest) {
        if (!(vendorRequest instanceof ChatCompletionRequest req)) return null;
        UnifiedChatRequest.UnifiedChatRequestBuilder b = UnifiedChatRequest.builder()
                .model(req.getModel())
                .stream(Boolean.TRUE.equals(req.getStream()))
                .maxTokens(req.getMaxTokens())
                .temperature(req.getTemperature())
                .topP(req.getTopP());
        if (req.getMessages() != null) {
            for (ChatMessage m : req.getMessages()) {
                StringBuilder txt = new StringBuilder();
                if (m.getContent() != null) {
                    m.getContent().forEach(p -> {
                        if (p.getText() != null) txt.append(p.getText());
                    });
                }
                b.message(UnifiedChatRequest.Message.builder().role(m.getRole()).content(txt.toString()).build());
            }
        }
        return b.build();
    }

    @Override
    public Object unifiedToVendorRequest(UnifiedChatRequest unifiedRequest) {
        ChatCompletionRequest req = new ChatCompletionRequest();
        req.setModel(unifiedRequest.getModel());
        req.setStream(unifiedRequest.getStream());
        req.setMaxTokens(unifiedRequest.getMaxTokens());
        req.setTemperature(unifiedRequest.getTemperature());
        req.setTopP(unifiedRequest.getTopP());
        if (unifiedRequest.getMessages() != null) {
            List<ChatMessage> list = new ArrayList<>();
            unifiedRequest.getMessages().forEach(m -> list.add(ChatMessage.text(m.getRole(), m.getContent())));
            req.setMessages(list);
        }
        return req;
    }

    @Override
    public UnifiedChatResponse vendorResponseToUnified(Object vendorResponse) {
        if (!(vendorResponse instanceof ChatCompletionResponse resp)) return null;
        StringBuilder content = new StringBuilder();
        if (resp.getChoices() != null) {
            resp.getChoices().forEach(c -> {
                if (c.getMessage() != null && c.getMessage().getContent() != null) {
                    c.getMessage().getContent().forEach(p -> {
                        if (p.getText() != null) content.append(p.getText());
                    });
                }
            });
        }
        UnifiedChatResponse.Usage usage = null;
        if (resp.getUsage() != null) {
            usage = UnifiedChatResponse.Usage.builder()
                    .promptTokens(resp.getUsage().getPromptTokens())
                    .completionTokens(resp.getUsage().getCompletionTokens())
                    .totalTokens(resp.getUsage().getTotalTokens())
                    .build();
        }
        return UnifiedChatResponse.builder()
                .id(resp.getId())
                .model(resp.getModel())
                .content(content.toString())
                .finishReason(resp.getChoices() != null && !resp.getChoices().isEmpty() ? resp.getChoices().get(0).getFinishReason() : null)
                .usage(usage)
                .rawVendor(resp)
                .build();
    }

    @Override
    public Object unifiedToVendorResponse(UnifiedChatResponse unifiedResponse) {
        if (unifiedResponse == null) return null;
        ChatCompletionResponse resp = new ChatCompletionResponse();
        resp.setId(unifiedResponse.getId());
        resp.setModel(unifiedResponse.getModel());
        resp.setObject("chat.completion");
        resp.setCreated(System.currentTimeMillis() / 1000);
        ChatMessage msg = ChatMessage.text("assistant", unifiedResponse.getContent());
        Choice choice = new Choice();
        choice.setIndex(0);
        choice.setMessage(msg);
        choice.setFinishReason(unifiedResponse.getFinishReason());
        resp.setChoices(List.of(choice));
        if (unifiedResponse.getUsage() != null) {
            Usage u = new Usage();
            u.setPromptTokens(unifiedResponse.getUsage().getPromptTokens());
            u.setCompletionTokens(unifiedResponse.getUsage().getCompletionTokens());
            u.setTotalTokens(unifiedResponse.getUsage().getTotalTokens());
            resp.setUsage(u);
        }
        return resp;
    }

    @Override
    public UnifiedStreamChunk vendorStreamEventToUnified(Object vendorEvent) {
        if (vendorEvent == null) return null;
        String json = vendorEvent.toString();
        if ("[DONE]".equals(json)) return new UnifiedStreamChunk(null, true);
        try {
            ChatCompletionChunk chunk = JsonUtils.fromJson(json, ChatCompletionChunk.class);
            if (chunk != null && chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                var delta = chunk.getChoices().get(0).getDelta();
                StringBuilder sb = new StringBuilder();
                if (delta != null && delta.getContent() != null) {
                    delta.getContent().forEach(p -> {
                        if (p.getText() != null) sb.append(p.getText());
                    });
                }
                boolean finished = chunk.getChoices().get(0).getFinishReason() != null;
                return new UnifiedStreamChunk(sb.toString(), finished);
            }
        } catch (Exception ignored) {
        }
        return new UnifiedStreamChunk(json, false);
    }

    @Override
    public Object unifiedStreamChunkToVendor(UnifiedStreamChunk chunk) {
        if (chunk == null) return null;
        if (chunk.isFinished()) return "[DONE]";
        ChatCompletionChunk response = new ChatCompletionChunk();
        response.setId("chatcmpl-" + System.nanoTime());
        response.setObject("chat.completion.chunk");
        response.setCreated(System.currentTimeMillis() / 1000);
        response.setModel("robella-proxy");
        Choice choice = new Choice();
        choice.setIndex(0);
        if (chunk.getContentDelta() != null) {
            Delta d = new Delta();
            d.setContent(List.of(ContentPart.ofText(chunk.getContentDelta())));
            choice.setDelta(d);
        }
        response.setChoices(List.of(choice));
        return JsonUtils.toJson(response);
    }
}

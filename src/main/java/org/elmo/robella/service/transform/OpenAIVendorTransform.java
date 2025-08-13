package org.elmo.robella.service.transform;

import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.config.ProviderType;
import org.elmo.robella.model.internal.*;
import org.elmo.robella.model.openai.*;
import org.elmo.robella.service.VendorTransform;
import org.elmo.robella.util.JsonUtils;

import java.util.*;

/**
 * OpenAI 及 OpenAI 兼容（DeepSeek、ModelScope、AIHubMix、Azure OpenAI）转换实现。
 * 完全重写，支持最新的统一模型格式和 OpenAI 格式。
 */
@Slf4j
public class OpenAIVendorTransform implements VendorTransform {
    @Override
    public String type() {
        return ProviderType.OpenAI.getName();
    }

    @Override
    public UnifiedChatRequest vendorRequestToUnified(Object vendorRequest) {
        if (!(vendorRequest instanceof ChatCompletionRequest req)) {
            return null;
        }

        UnifiedChatRequest.UnifiedChatRequestBuilder builder = UnifiedChatRequest.builder()
                .model(req.getModel())
                .stream(req.getStream())
                .maxTokens(req.getMaxTokens() != null ? req.getMaxTokens() : req.getMaxCompletionTokens())
                .temperature(req.getTemperature())
                .topP(req.getTopP())
                .frequencyPenalty(req.getFrequencyPenalty())
                .presencePenalty(req.getPresencePenalty())
                .logprobs(req.getLogprobs())
                .topLogprobs(req.getTopLogprobs())
                .responseFormat(req.getResponseFormat())
                .thinking(req.getThinking())
                .parallelToolCalls(req.getParallelToolCalls())
                .modalities(req.getModalities())
                .n(req.getN())
                .promptCacheKey(req.getPromptCacheKey())
                .audio(req.getAudio())
                .reasoningEffort(req.getReasoningEffort())
                .textOptions(req.getText());

        // 处理停止序列
        List<String> stops = normalizeStop(req.getStop());
        if (stops != null) {
            builder.stop(stops);
        }

        // 处理厂商特定参数
        Map<String, Object> vendorExtras = new HashMap<>();
        if (req.getStreamOptions() != null) {
            vendorExtras.put("openai.stream_options", req.getStreamOptions());
        }
        if (req.getExtraBody() != null) {
            vendorExtras.put("openai.extra_body", req.getExtraBody());
        }
        if (!vendorExtras.isEmpty()) {
            builder.vendorExtras(vendorExtras);
        }

        // 转换消息列表
        if (req.getMessages() != null) {
            for (ChatMessage openaiMsg : req.getMessages()) {
                UnifiedChatMessage unifiedMsg = convertOpenAIMessageToUnified(openaiMsg);
                if (unifiedMsg != null) {
                    builder.message(unifiedMsg);
                }
            }
        }

        // 转换工具列表
        if (req.getTools() != null && !req.getTools().isEmpty()) {
            for (Tool openaiTool : req.getTools()) {
                UnifiedTool unifiedTool = convertOpenAIToolToUnified(openaiTool);
                if (unifiedTool != null) {
                    builder.tool(unifiedTool);
                }
            }
        }

        // 转换工具选择
        builder.toolChoice(req.getToolChoice());

        return builder.build();
    }

    @Override
    public Object unifiedToVendorRequest(UnifiedChatRequest unifiedRequest) {
        ChatCompletionRequest req = new ChatCompletionRequest();

        // 基本参数
        req.setModel(unifiedRequest.getModel());
        req.setStream(unifiedRequest.getStream());
        req.setMaxTokens(unifiedRequest.getMaxTokens());
        req.setTemperature(unifiedRequest.getTemperature());
        req.setTopP(unifiedRequest.getTopP());
        req.setFrequencyPenalty(unifiedRequest.getFrequencyPenalty());
        req.setPresencePenalty(unifiedRequest.getPresencePenalty());
        req.setLogprobs(unifiedRequest.getLogprobs());
        req.setTopLogprobs(unifiedRequest.getTopLogprobs());
        req.setResponseFormat((ResponseFormat) unifiedRequest.getResponseFormat());
        req.setThinking((Thinking) unifiedRequest.getThinking());
        req.setParallelToolCalls(unifiedRequest.getParallelToolCalls());
        req.setModalities(unifiedRequest.getModalities());
        req.setN(unifiedRequest.getN());
        req.setPromptCacheKey(unifiedRequest.getPromptCacheKey());
        req.setAudio((Audio) unifiedRequest.getAudio());
        req.setReasoningEffort(unifiedRequest.getReasoningEffort());
        req.setText((TextOptions) unifiedRequest.getTextOptions());

        // 处理停止序列
        if (unifiedRequest.getStop() != null && !unifiedRequest.getStop().isEmpty()) {
            if (unifiedRequest.getStop().size() == 1) {
                req.setStop(unifiedRequest.getStop().get(0));
            } else {
                req.setStop(unifiedRequest.getStop());
            }
        }

        // 转换消息列表
        if (unifiedRequest.getMessages() != null) {
            List<ChatMessage> openaiMessages = new ArrayList<>();
            for (UnifiedChatMessage unifiedMsg : unifiedRequest.getMessages()) {
                ChatMessage openaiMsg = convertUnifiedMessageToOpenAI(unifiedMsg);
                if (openaiMsg != null) {
                    openaiMessages.add(openaiMsg);
                }
            }
            req.setMessages(openaiMessages);
        }

        // 转换工具列表
        if (unifiedRequest.getTools() != null && !unifiedRequest.getTools().isEmpty()) {
            List<Tool> openaiTools = new ArrayList<>();
            for (UnifiedTool unifiedTool : unifiedRequest.getTools()) {
                Tool openaiTool = convertUnifiedToolToOpenAI(unifiedTool);
                if (openaiTool != null) {
                    openaiTools.add(openaiTool);
                }
            }
            req.setTools(openaiTools);
        }

        // 转换工具选择
        req.setToolChoice(convertToolChoice(unifiedRequest.getToolChoice()));

        // 处理厂商特定参数和额外参数
        Map<String, Object> extraBody = new HashMap<>();

        // 添加统一模型中没有直接映射的参数
        if (unifiedRequest.getTopK() != null) {
            extraBody.put("top_k", unifiedRequest.getTopK());
        }
        if (unifiedRequest.getMinP() != null) {
            extraBody.put("min_p", unifiedRequest.getMinP());
        }
        if (unifiedRequest.getSeed() != null) {
            extraBody.put("seed", unifiedRequest.getSeed());
        }

        // 恢复厂商特定参数
        if (unifiedRequest.getVendorExtras() != null) {
            if (unifiedRequest.getVendorExtras().containsKey("openai.stream_options")) {
                req.setStreamOptions((StreamOptions) unifiedRequest.getVendorExtras().get("openai.stream_options"));
            }

            if (unifiedRequest.getVendorExtras().containsKey("openai.extra_body")) {
                Object existingExtra = unifiedRequest.getVendorExtras().get("openai.extra_body");
                if (existingExtra instanceof Map<?, ?> existingMap) {
                    existingMap.forEach((k, v) -> extraBody.put(String.valueOf(k), v));
                }
            }
        }

        if (!extraBody.isEmpty()) {
            req.setExtraBody(extraBody);
        }

        return req;
    }

    @Override
    public UnifiedChatResponse vendorResponseToUnified(Object vendorResponse) {
        if (!(vendorResponse instanceof ChatCompletionResponse resp)) {
            return null;
        }

        UnifiedChatResponse.UnifiedChatResponseBuilder builder = UnifiedChatResponse.builder()
                .id(resp.getId())
                .model(resp.getModel())
                .rawVendor(resp);

        StringBuilder content = new StringBuilder();
        List<UnifiedChatMessage> messages = new ArrayList<>();
        List<UnifiedToolCall> toolCalls = new ArrayList<>();
        String reasoningAll = null;
        UnifiedChatMessage assistantMessage = null;

        if (resp.getChoices() != null) {
            for (Choice choice : resp.getChoices()) {
                if (choice.getMessage() == null) {
                    continue;
                }

                ChatMessage openaiMsg = choice.getMessage();
                UnifiedChatMessage unifiedMsg = convertOpenAIMessageToUnified(openaiMsg);

                if (unifiedMsg != null) {
                    messages.add(unifiedMsg);
                    assistantMessage = unifiedMsg;

                    // 聚合文本内容
                    String msgText = unifiedMsg.aggregatedText();
                    if (msgText != null) {
                        content.append(msgText);
                    }

                    // 聚合推理内容
                    if (unifiedMsg.getReasoningContent() != null) {
                        reasoningAll = append(reasoningAll, unifiedMsg.getReasoningContent());
                    }

                    // 聚合工具调用
                    if (unifiedMsg.getToolCalls() != null) {
                        toolCalls.addAll(unifiedMsg.getToolCalls());
                    }
                }

                // 设置完成原因
                if (choice.getFinishReason() != null) {
                    builder.finishReason(choice.getFinishReason());
                }
            }
        }

        builder.content(content.length() > 0 ? content.toString() : null)
                .assistantMessage(assistantMessage)
                .messages(messages.isEmpty() ? null : messages)
                .toolCalls(toolCalls.isEmpty() ? null : toolCalls)
                .reasoningContent(reasoningAll);

        // 处理 usage 信息
        if (resp.getUsage() != null) {
            UnifiedChatResponse.Usage usage = convertOpenAIUsageToUnified(resp.getUsage());
            builder.usage(usage);
        }

        return builder.build();
    }

    @Override
    public Object unifiedToVendorResponse(UnifiedChatResponse unifiedResponse) {
        if (unifiedResponse == null) {
            return null;
        }

        ChatCompletionResponse resp = new ChatCompletionResponse();
        resp.setId(unifiedResponse.getId());
        resp.setModel(unifiedResponse.getModel());
        resp.setObject("chat.completion");
        resp.setCreated(System.currentTimeMillis() / 1000);

        // 构建选择
        Choice choice = new Choice();
        choice.setIndex(0);
        choice.setFinishReason(unifiedResponse.getFinishReason());

        // 构建消息
        ChatMessage message;
        if (unifiedResponse.getAssistantMessage() != null) {
            message = convertUnifiedMessageToOpenAI(unifiedResponse.getAssistantMessage());
        } else {
            // 回退到简单文本消息
            message = ChatMessage.builder()
                    .role("assistant")
                    .content(unifiedResponse.getContent() != null ?
                            List.of(ContentPart.ofText(unifiedResponse.getContent())) : null)
                    .build();
        }
        choice.setMessage(message);
        resp.setChoices(List.of(choice));

        // 转换 usage
        if (unifiedResponse.getUsage() != null) {
            Usage usage = convertUnifiedUsageToOpenAI(unifiedResponse.getUsage());
            resp.setUsage(usage);
        }

        return resp;
    }

    @Override
    public UnifiedStreamChunk vendorStreamEventToUnified(Object vendorEvent) {
        if (vendorEvent == null) {
            return null;
        }

        String json = vendorEvent.toString();

        // 处理结束标记
        if ("[DONE]".equals(json)) {
            return UnifiedStreamChunk.builder().finished(true).build();
        }

        // 处理 SSE 前缀
        boolean hadDataPrefix = false;
        if (json.startsWith("data:")) {
            hadDataPrefix = true;
            json = json.substring(5).trim();
            if ("[DONE]".equals(json)) {
                return UnifiedStreamChunk.builder().finished(true).build();
            }
        }

        // 解析流块
        UnifiedStreamChunk chunk = parseOpenAIStreamChunk(json);
        if (chunk != null) {
            return chunk;
        }

        // 回退处理
        if (hadDataPrefix) {
            return UnifiedStreamChunk.builder().contentDelta(json).finished(false).build();
        }
        return UnifiedStreamChunk.builder().contentDelta(json).finished(false).build();
    }

    @Override
    public Object unifiedStreamChunkToVendor(UnifiedStreamChunk chunk) {
        if (chunk == null) {
            return null;
        }

        // 如果没有任何内容且只是结束标志，返回 [DONE]
        if (chunk.isFinished() && !hasAnyDelta(chunk) && chunk.getFinishReason() == null) {
            return "[DONE]";
        }

        ChatCompletionChunk response = new ChatCompletionChunk();
        response.setId("chatcmpl-" + System.nanoTime());
        response.setObject("chat.completion.chunk");
        response.setCreated(System.currentTimeMillis() / 1000);
        response.setModel("robella-proxy");

        Choice choice = new Choice();
        choice.setIndex(0);

        // 构建 delta
        if (hasAnyDelta(chunk)) {
            Delta delta = new Delta();

            if (chunk.getContentDelta() != null) {
                delta.setContent(List.of(ContentPart.ofText(chunk.getContentDelta())));
            }

            if (chunk.getReasoningDelta() != null) {
                delta.setReasoningContent(chunk.getReasoningDelta());
            }

            if (chunk.getToolCallDeltas() != null && !chunk.getToolCallDeltas().isEmpty()) {
                List<ToolCall> toolCalls = new ArrayList<>();
                for (UnifiedToolCallDelta tcd : chunk.getToolCallDeltas()) {
                    ToolCall.FunctionToolCall fc = ToolCall.FunctionToolCall.builder()
                            .id(tcd.getId())
                            .type("function")
                            .function(ToolCall.Function.builder()
                                    .name(tcd.getName())
                                    .augument(tcd.getArgumentsDelta())
                                    .build())
                            .build();
                    toolCalls.add(ToolCall.builder().function(fc).build());
                }
                delta.setToolCalls(toolCalls);
            }

            choice.setDelta(delta);
        }

        if (chunk.getFinishReason() != null) {
            choice.setFinishReason(chunk.getFinishReason());
        }

        response.setChoices(List.of(choice));

        // 添加 usage（如果有）
        if (chunk.getUsage() != null) {
            Usage usage = convertUnifiedUsageToOpenAI(chunk.getUsage());
            response.setUsage(usage);
        }

        // 返回纯JSON格式，不包装SSE
        return JsonUtils.toJson(response);
    }

    // -------------------- 辅助方法 --------------------

    /**
     * 转换 OpenAI 消息到统一格式
     */
    private UnifiedChatMessage convertOpenAIMessageToUnified(ChatMessage openaiMsg) {
        if (openaiMsg == null) {
            return null;
        }

        UnifiedChatMessage.UnifiedChatMessageBuilder builder = UnifiedChatMessage.builder()
                .role(openaiMsg.getRole())
                .name(openaiMsg.getName())
                .toolCallId(openaiMsg.getToolCallId())
                .reasoningContent(openaiMsg.getReasoningContent())
                .refusal(openaiMsg.getRefusal())
                .audio(openaiMsg.getAudio());

        // 转换内容部分
        if (openaiMsg.getContent() != null) {
            for (ContentPart part : openaiMsg.getContent()) {
                UnifiedContentPart unifiedPart = convertOpenAIContentPartToUnified(part);
                if (unifiedPart != null) {
                    builder.content(unifiedPart);
                }
            }
        }

        // 转换工具调用
        if (openaiMsg.getToolCalls() != null) {
            for (ToolCall toolCall : openaiMsg.getToolCalls()) {
                UnifiedToolCall unifiedToolCall = convertOpenAIToolCallToUnified(toolCall);
                if (unifiedToolCall != null) {
                    builder.toolCall(unifiedToolCall);
                }
            }
        }

        // 转换注释
        if (openaiMsg.getAnnotations() != null) {
            for (MessageAnnotation annotation : openaiMsg.getAnnotations()) {
                UnifiedMessageAnnotation unifiedAnnotation = convertOpenAIAnnotationToUnified(annotation);
                if (unifiedAnnotation != null) {
                    builder.annotation(unifiedAnnotation);
                }
            }
        }

        return builder.build();
    }

    /**
     * 转换统一消息到 OpenAI 格式
     */
    private ChatMessage convertUnifiedMessageToOpenAI(UnifiedChatMessage unifiedMsg) {
        if (unifiedMsg == null) {
            return null;
        }

        ChatMessage.ChatMessageBuilder builder = ChatMessage.builder()
                .role(unifiedMsg.getRole())
                .name(unifiedMsg.getName())
                .toolCallId(unifiedMsg.getToolCallId())
                .reasoningContent(unifiedMsg.getReasoningContent())
                .refusal(unifiedMsg.getRefusal())
                .audio((AudioData) unifiedMsg.getAudio());

        // 转换内容部分
        if (unifiedMsg.getContents() != null) {
            List<ContentPart> contentParts = new ArrayList<>();
            for (UnifiedContentPart unifiedPart : unifiedMsg.getContents()) {
                ContentPart openaiPart = convertUnifiedContentPartToOpenAI(unifiedPart);
                if (openaiPart != null) {
                    contentParts.add(openaiPart);
                }
            }
            if (!contentParts.isEmpty()) {
                builder.content(contentParts);
            }
        }

        // 转换工具调用
        if (unifiedMsg.getToolCalls() != null) {
            List<ToolCall> toolCalls = new ArrayList<>();
            for (UnifiedToolCall unifiedToolCall : unifiedMsg.getToolCalls()) {
                ToolCall openaiToolCall = convertUnifiedToolCallToOpenAI(unifiedToolCall);
                if (openaiToolCall != null) {
                    toolCalls.add(openaiToolCall);
                }
            }
            if (!toolCalls.isEmpty()) {
                builder.toolCalls(toolCalls);
            }
        }

        // 转换注释
        if (unifiedMsg.getAnnotations() != null) {
            List<MessageAnnotation> annotations = new ArrayList<>();
            for (UnifiedMessageAnnotation unifiedAnnotation : unifiedMsg.getAnnotations()) {
                MessageAnnotation openaiAnnotation = convertUnifiedAnnotationToOpenAI(unifiedAnnotation);
                if (openaiAnnotation != null) {
                    annotations.add(openaiAnnotation);
                }
            }
            if (!annotations.isEmpty()) {
                builder.annotations(annotations);
            }
        }

        return builder.build();
    }

    /**
     * 转换 OpenAI 工具到统一格式
     */
    private UnifiedTool convertOpenAIToolToUnified(Tool openaiTool) {
        if (openaiTool == null) {
            return null;
        }

        UnifiedTool.UnifiedToolBuilder builder = UnifiedTool.builder()
                .type(openaiTool.getType());

        if (openaiTool.getFunction() != null) {
            Function func = openaiTool.getFunction();
            builder.function(UnifiedTool.Function.builder()
                    .name(func.getName())
                    .description(func.getDescription())
                    .parameters(func.getParameters())
                    .strict(func.getStrict())
                    .build());
        }

        return builder.build();
    }

    /**
     * 转换统一工具到 OpenAI 格式
     */
    private Tool convertUnifiedToolToOpenAI(UnifiedTool unifiedTool) {
        if (unifiedTool == null) {
            return null;
        }

        Tool tool = new Tool();
        tool.setType(unifiedTool.getType());

        if (unifiedTool.getFunction() != null) {
            Function function = new Function();
            function.setName(unifiedTool.getFunction().getName());
            function.setDescription(unifiedTool.getFunction().getDescription());
            function.setParameters(unifiedTool.getFunction().getParameters());
            function.setStrict(unifiedTool.getFunction().getStrict());
            tool.setFunction(function);
        }

        return tool;
    }

    /**
     * 转换 OpenAI 内容部分到统一格式
     */
    private UnifiedContentPart convertOpenAIContentPartToUnified(ContentPart openaiPart) {
        if (openaiPart == null) {
            return null;
        }

        if (openaiPart.getText() != null) {
            return UnifiedContentPart.text(openaiPart.getText());
        }

        if (openaiPart.getImageUrl() != null) {
            return UnifiedContentPart.image(
                    openaiPart.getImageUrl().getUrl(),
                    openaiPart.getImageUrl().getDetail());
        }

        if (openaiPart.getInputAudio() != null) {
            return UnifiedContentPart.audio(
                    openaiPart.getInputAudio().getData(),
                    openaiPart.getInputAudio().getFormat());
        }

        return null;
    }

    /**
     * 转换统一内容部分到 OpenAI 格式
     */
    private ContentPart convertUnifiedContentPartToOpenAI(UnifiedContentPart unifiedPart) {
        if (unifiedPart == null || unifiedPart.getType() == null) {
            return null;
        }

        switch (unifiedPart.getType()) {
            case "text":
                return ContentPart.ofText(unifiedPart.getText());
            case "image":
                return ContentPart.ofImage(unifiedPart.getUrl(), unifiedPart.getDetail());
            case "audio":
                return ContentPart.ofAudio(unifiedPart.getData(), unifiedPart.getFormat());
            case "refusal":
                return ContentPart.ofText(unifiedPart.getRefusal());
            default:
                return null;
        }
    }

    /**
     * 转换 OpenAI 工具调用到统一格式
     */
    private UnifiedToolCall convertOpenAIToolCallToUnified(ToolCall openaiToolCall) {
        if (openaiToolCall == null) {
            return null;
        }

        if (openaiToolCall.getFunction() != null) {
            ToolCall.FunctionToolCall func = openaiToolCall.getFunction();
            return UnifiedToolCall.builder()
                    .id(func.getId())
                    .type(func.getType())
                    .function(UnifiedToolCall.FunctionCall.builder()
                            .name(func.getFunction() != null ? func.getFunction().getName() : null)
                            .arguments(func.getFunction() != null ? func.getFunction().getAugument() : null)
                            .build())
                    .build();
        }

        if (openaiToolCall.getCustom() != null) {
            ToolCall.CustomToolCall custom = openaiToolCall.getCustom();
            return UnifiedToolCall.builder()
                    .id(custom.getId())
                    .type(custom.getType())
                    .function(UnifiedToolCall.FunctionCall.builder()
                            .name(custom.getCustom() != null ? custom.getCustom().getName() : null)
                            .arguments(custom.getCustom() != null ? custom.getCustom().getInput() : null)
                            .build())
                    .build();
        }

        return null;
    }

    /**
     * 转换统一工具调用到 OpenAI 格式
     */
    private ToolCall convertUnifiedToolCallToOpenAI(UnifiedToolCall unifiedToolCall) {
        if (unifiedToolCall == null || unifiedToolCall.getFunction() == null) {
            return null;
        }

        ToolCall.FunctionToolCall functionToolCall = ToolCall.FunctionToolCall.builder()
                .id(unifiedToolCall.getId())
                .type(unifiedToolCall.getType())
                .function(ToolCall.Function.builder()
                        .name(unifiedToolCall.getFunction().getName())
                        .augument(unifiedToolCall.getFunction().getArguments())
                        .build())
                .build();

        return ToolCall.builder().function(functionToolCall).build();
    }

    /**
     * 转换 OpenAI 注释到统一格式
     */
    private UnifiedMessageAnnotation convertOpenAIAnnotationToUnified(MessageAnnotation openaiAnnotation) {
        if (openaiAnnotation == null) {
            return null;
        }

        UnifiedMessageAnnotation.UnifiedMessageAnnotationBuilder builder = UnifiedMessageAnnotation.builder()
                .type(openaiAnnotation.getType());

        // 转换文本范围
        if (openaiAnnotation.getStartIndex() != null || openaiAnnotation.getEndIndex() != null) {
            builder.range(UnifiedMessageAnnotation.TextRange.builder()
                    .start(openaiAnnotation.getStartIndex())
                    .end(openaiAnnotation.getEndIndex())
                    .type("character")
                    .build());
        }

        // 转换 URL 引用
        if ("url_citation".equals(openaiAnnotation.getType()) && openaiAnnotation.getUrlCitation() != null) {
            MessageAnnotation.UrlCitation urlCitation = openaiAnnotation.getUrlCitation();
            builder.citation(UnifiedMessageAnnotation.Citation.builder()
                    .url(urlCitation.getUrl())
                    .text(urlCitation.getTitle())
                    .build());
        }

        return builder.build();
    }

    /**
     * 转换统一注释到 OpenAI 格式
     */
    private MessageAnnotation convertUnifiedAnnotationToOpenAI(UnifiedMessageAnnotation unifiedAnnotation) {
        if (unifiedAnnotation == null) {
            return null;
        }

        MessageAnnotation.MessageAnnotationBuilder builder = MessageAnnotation.builder()
                .type(unifiedAnnotation.getType());

        // 转换文本范围
        if (unifiedAnnotation.getRange() != null) {
            builder.startIndex(unifiedAnnotation.getRange().getStart())
                    .endIndex(unifiedAnnotation.getRange().getEnd());
        }

        // 转换引用信息
        if ("citation".equals(unifiedAnnotation.getType()) && unifiedAnnotation.getCitation() != null) {
            UnifiedMessageAnnotation.Citation citation = unifiedAnnotation.getCitation();
            builder.type("url_citation")
                    .urlCitation(MessageAnnotation.UrlCitation.builder()
                            .url(citation.getUrl())
                            .title(citation.getText())
                            .build());
        }

        return builder.build();
    }

    /**
     * 转换 OpenAI Usage 到统一格式
     */
    private UnifiedChatResponse.Usage convertOpenAIUsageToUnified(Usage openaiUsage) {
        if (openaiUsage == null) {
            return null;
        }

        UnifiedChatResponse.Usage.UsageBuilder builder = UnifiedChatResponse.Usage.builder()
                .promptTokens(openaiUsage.getPromptTokens())
                .completionTokens(openaiUsage.getCompletionTokens())
                .totalTokens(openaiUsage.getTotalTokens());

        Map<String, Object> extra = new HashMap<>();

        // 处理缓存相关 tokens
        if (openaiUsage.getPromptCacheHitTokens() != null) {
            extra.put("prompt_cache_hit_tokens", openaiUsage.getPromptCacheHitTokens());
        }
        if (openaiUsage.getPromptCacheMissTokens() != null) {
            extra.put("prompt_cache_miss_tokens", openaiUsage.getPromptCacheMissTokens());
        }

        // 处理详细 token 信息
        if (openaiUsage.getPromptTokensDetails() != null) {
            PromptTokensDetails ptd = openaiUsage.getPromptTokensDetails();
            if (ptd.getAudioTokens() != null) {
                extra.put("prompt_audio_tokens", ptd.getAudioTokens());
            }
            if (ptd.getCachedTokens() != null) {
                builder.cachedTokens(ptd.getCachedTokens());
            }
        }

        if (openaiUsage.getCompletionTokensDetails() != null) {
            CompletionTokensDetails ctd = openaiUsage.getCompletionTokensDetails();
            if (ctd.getReasoningTokens() != null) {
                builder.reasoningTokens(ctd.getReasoningTokens());
            }
            if (ctd.getAcceptedPredictionTokens() != null) {
                extra.put("accepted_prediction_tokens", ctd.getAcceptedPredictionTokens());
            }
            if (ctd.getRejectedPredictionTokens() != null) {
                extra.put("rejected_prediction_tokens", ctd.getRejectedPredictionTokens());
            }
            if (ctd.getAudioTokens() != null) {
                builder.audioTokens(ctd.getAudioTokens());
            }
        }

        if (!extra.isEmpty()) {
            builder.extra(extra);
        }

        return builder.build();
    }

    /**
     * 转换统一 Usage 到 OpenAI 格式
     */
    private Usage convertUnifiedUsageToOpenAI(UnifiedChatResponse.Usage unifiedUsage) {
        if (unifiedUsage == null) {
            return null;
        }

        Usage usage = new Usage();
        usage.setPromptTokens(unifiedUsage.getPromptTokens());
        usage.setCompletionTokens(unifiedUsage.getCompletionTokens());
        usage.setTotalTokens(unifiedUsage.getTotalTokens());

        // 处理详细信息（如果OpenAI模型支持）
        if (unifiedUsage.getReasoningTokens() != null || unifiedUsage.getAudioTokens() != null ||
                (unifiedUsage.getExtra() != null && !unifiedUsage.getExtra().isEmpty())) {

            CompletionTokensDetails ctd = new CompletionTokensDetails();
            if (unifiedUsage.getReasoningTokens() != null) {
                ctd.setReasoningTokens(unifiedUsage.getReasoningTokens());
            }
            if (unifiedUsage.getAudioTokens() != null) {
                ctd.setAudioTokens(unifiedUsage.getAudioTokens());
            }

            if (unifiedUsage.getExtra() != null) {
                if (unifiedUsage.getExtra().containsKey("accepted_prediction_tokens")) {
                    ctd.setAcceptedPredictionTokens((Integer) unifiedUsage.getExtra().get("accepted_prediction_tokens"));
                }
                if (unifiedUsage.getExtra().containsKey("rejected_prediction_tokens")) {
                    ctd.setRejectedPredictionTokens((Integer) unifiedUsage.getExtra().get("rejected_prediction_tokens"));
                }
            }

            usage.setCompletionTokensDetails(ctd);
        }

        if (unifiedUsage.getCachedTokens() != null) {
            PromptTokensDetails ptd = new PromptTokensDetails();
            ptd.setCachedTokens(unifiedUsage.getCachedTokens());
            usage.setPromptTokensDetails(ptd);
        }

        return usage;
    }

    /**
     * 解析 OpenAI 流式响应
     */
    private UnifiedStreamChunk parseOpenAIStreamChunk(String json) {
        try {
            // 格式不对
            if (json == null || json.isEmpty() || json.charAt(0) != '{') {
                return null;
            }

            // 流事件序列化为 ChatCompletionChunk 对象
            ChatCompletionChunk chunk = JsonUtils.fromJson(json, ChatCompletionChunk.class);
            if (chunk == null || chunk.getChoices() == null || chunk.getChoices().isEmpty()) {
                return null;
            }

            Choice choice = chunk.getChoices().get(0);
            Delta delta = choice.getDelta();

            StringBuilder content = new StringBuilder();
            List<UnifiedToolCallDelta> toolCallDeltas = new ArrayList<>();
            String reasoningDelta = null;

            if (delta != null) {
                // 处理内容增量
                if (delta.getContent() != null) {
                    for (ContentPart part : delta.getContent()) {
                        if (part.getText() != null) {
                            content.append(part.getText());
                        }
                    }
                }

                // 处理推理增量
                if (delta.getReasoningContent() != null) {
                    reasoningDelta = delta.getReasoningContent();
                }

                // 处理工具调用增量
                if (delta.getToolCalls() != null) {
                    for (ToolCall toolCall : delta.getToolCalls()) {
                        UnifiedToolCallDelta toolCallDelta = UnifiedToolCallDelta.builder()
                                .id(toolCall.getFunction() != null ? toolCall.getFunction().getId() :
                                        toolCall.getCustom() != null ? toolCall.getCustom().getId() : null)
                                .name(toolCall.getFunction() != null && toolCall.getFunction().getFunction() != null ?
                                        toolCall.getFunction().getFunction().getName() :
                                        toolCall.getCustom() != null && toolCall.getCustom().getCustom() != null ?
                                                toolCall.getCustom().getCustom().getName() : null)
                                .argumentsDelta(toolCall.getFunction() != null && toolCall.getFunction().getFunction() != null ?
                                        toolCall.getFunction().getFunction().getAugument() :
                                        toolCall.getCustom() != null && toolCall.getCustom().getCustom() != null ?
                                                toolCall.getCustom().getCustom().getInput() : null)
                                .build();
                        toolCallDeltas.add(toolCallDelta);
                    }
                }
            }

            boolean finished = choice.getFinishReason() != null;

            UnifiedChatResponse.Usage usage = null;
            if (chunk.getUsage() != null) {
                usage = convertOpenAIUsageToUnified(chunk.getUsage());
            }

            return UnifiedStreamChunk.builder()
                    .contentDelta(content.length() > 0 ? content.toString() : null)
                    .reasoningDelta(reasoningDelta)
                    .toolCallDeltas(toolCallDeltas.isEmpty() ? null : toolCallDeltas)
                    .finished(finished)
                    .finishReason(choice.getFinishReason())
                    .usage(usage)
                    .build();

        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("[StreamTransform] parse exception json='{}' ex={}", truncate(json), e.toString());
            }
            return null;
        }
    }

    /**
     * 检查流块是否有任何增量内容
     */
    private boolean hasAnyDelta(UnifiedStreamChunk chunk) {
        return chunk.getContentDelta() != null ||
                chunk.getReasoningDelta() != null ||
                (chunk.getToolCallDeltas() != null && !chunk.getToolCallDeltas().isEmpty()) ||
                (chunk.getContentParts() != null && !chunk.getContentParts().isEmpty());
    }

    /**
     * 将不同类型的停止序列参数标准化为字符串列表
     */
    private List<String> normalizeStop(Object stop) {
        if (stop == null) {
            return null;
        }
        if (stop instanceof String s) {
            return List.of(s);
        }
        if (stop instanceof Collection<?> c) {
            List<String> result = new ArrayList<>();
            c.forEach(o -> {
                if (o != null) {
                    result.add(String.valueOf(o));
                }
            });
            return result;
        }
        return null;
    }

    /**
     * 将新增内容追加到基础字符串后面
     */
    private String append(String base, String add) {
        if (add == null || add.isEmpty()) {
            return base;
        }
        return base == null ? add : base + add;
    }

    /**
     * 转换工具选择从Object到ToolChoice
     */
    private ToolChoice convertToolChoice(Object toolChoice) {
        if (toolChoice == null) {
            return null;
        }

        if (toolChoice instanceof String s) {
            return ToolChoice.of(s);
        }

        if (toolChoice instanceof ToolChoice tc) {
            return tc;
        }

        // 如果是Map或其他对象，尝试通过Jackson转换
        if (toolChoice instanceof Map<?, ?> map) {
            String type = (String) map.get("type");
            if ("function".equals(type)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> functionMap = (Map<String, Object>) map.get("function");
                if (functionMap != null) {
                    String functionName = (String) functionMap.get("name");
                    return ToolChoice.function(functionName);
                }
            }

            if (type != null) {
                return ToolChoice.of(type);
            }
        }

        return null;
    }

    /**
     * 截断字符串用于日志输出
     */
    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        if (s.length() <= 200) {
            return s;
        }
        return s.substring(0, 200) + "...";
    }
}
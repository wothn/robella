package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.model.common.OpenAIMessage;
import org.elmo.robella.model.common.UnifiedMessage;
import org.elmo.robella.model.request.ClaudeChatRequest;
import org.elmo.robella.model.request.ClaudeMessage;
import org.elmo.robella.model.request.GeminiChatRequest;
import org.elmo.robella.model.request.GeminiContent;
import org.elmo.robella.model.request.GeminiGenerationConfig;
import org.elmo.robella.model.request.OpenAIChatRequest;
import org.elmo.robella.model.request.UnifiedChatRequest;
import org.elmo.robella.model.response.openai.OpenAIChatResponse;
import org.elmo.robella.model.response.openai.OpenAIChoice;
import org.elmo.robella.model.response.openai.OpenAIUsage;
import org.elmo.robella.model.response.UnifiedChatResponse;
import org.elmo.robella.model.response.openai.OpenAIStreamResponse;
import org.elmo.robella.model.response.openai.OpenAIStreamChoice;
import org.elmo.robella.model.response.openai.OpenAIStreamDelta;
import org.elmo.robella.util.JsonUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransformServiceImpl implements TransformService {

    private final ProviderConfig providerConfig;

    // 将OpenAI聊天请求转换为统一聊天请求格式
    @Override
    public UnifiedChatRequest toUnified(OpenAIChatRequest openaiRequest) {

        UnifiedChatRequest unifiedRequest = new UnifiedChatRequest();
        unifiedRequest.setModel(openaiRequest.getModel());
        unifiedRequest.setMessages(openaiRequest.getMessages().stream()
                .map(msg -> {
                    UnifiedMessage unifiedMsg = new UnifiedMessage();
                    unifiedMsg.setRole(msg.getRole());
                    unifiedMsg.setContent(msg.getContent());
                    return unifiedMsg;
                })
                .collect(Collectors.toList()));
        unifiedRequest.setTemperature(openaiRequest.getTemperature());
        unifiedRequest.setMaxTokens(openaiRequest.getMaxTokens());
        unifiedRequest.setStream(openaiRequest.getStream());
        log.info("OpenAI转Unified消息");
        return unifiedRequest;
    }

    // 将统一聊天响应转换为OpenAI聊天响应格式
    @Override
    public OpenAIChatResponse toOpenAI(UnifiedChatResponse unifiedResponse) {
        OpenAIChatResponse openAIResponse = new OpenAIChatResponse();
        openAIResponse.setId(unifiedResponse.getId());
        openAIResponse.setObject("chat.completion");
        openAIResponse.setCreated(System.currentTimeMillis() / 1000);
        openAIResponse.setModel(unifiedResponse.getModel());

        OpenAIChoice choice = new OpenAIChoice();
        choice.setIndex(0);
        OpenAIMessage message = new OpenAIMessage();
        message.setRole("assistant");
        message.setContent(unifiedResponse.getMessage().getContent());
        choice.setMessage(message);
        choice.setFinishReason("stop");

        openAIResponse.setChoices(Collections.singletonList(choice));

        OpenAIUsage usage = new OpenAIUsage();
        usage.setPromptTokens(unifiedResponse.getUsage().getPromptTokens());
        usage.setCompletionTokens(unifiedResponse.getUsage().getCompletionTokens());
        usage.setTotalTokens(unifiedResponse.getUsage().getTotalTokens());
        openAIResponse.setUsage(usage);
        log.info("Unified转OpenAI消息");
        return openAIResponse;
    }

    // 将厂商响应转换为统一响应格式
    @Override
    public Object toVendor(UnifiedChatRequest unifiedRequest, String providerName) {
        // 获取厂商配置
        ProviderConfig.Provider providerConfig = this.providerConfig.getProviders().get(providerName);

        // 将用户模型名称映射到厂商模型名称
        String vendorModel = mapToVendorModel(unifiedRequest.getModel(), providerConfig);

        // 获取提供商类型
        String providerType = providerConfig != null ? providerConfig.getType() : null;
        if (providerType == null) {
            // 如果类型为空，默认使用OpenAI格式
            return toOpenAIRequest(unifiedRequest, vendorModel);
        }

        // 根据提供商类型转换为特定格式
        return switch (providerType) {
            case "OpenAI", "AzureOpenAI" -> toOpenAIRequest(unifiedRequest, vendorModel);
            case "Anthropic" -> toClaudeRequest(unifiedRequest, vendorModel);
            case "Gemini" -> toGeminiRequest(unifiedRequest, vendorModel);
            default ->
                // 默认使用OpenAI格式
                    toOpenAIRequest(unifiedRequest, vendorModel);
        };
    }

    /**
     * 将用户请求的模型名称映射到厂商的实际模型名称
     */
    private String mapToVendorModel(String userModel, ProviderConfig.Provider providerConfig) {
        if (providerConfig != null && providerConfig.getModels() != null) {
            for (ProviderConfig.Model model : providerConfig.getModels()) {
                if (model.getName().equals(userModel)) {
                    return model.getVendorModel();
                }
            }
        }
        // 如果没有找到映射，返回原模型名称
        return userModel;
    }

    // 将统一聊天请求转换为OpenAI格式
    private Object toOpenAIRequest(UnifiedChatRequest unifiedRequest, String vendorModel) {
        OpenAIChatRequest openAIRequest = new OpenAIChatRequest();
        openAIRequest.setModel(vendorModel); // 使用厂商模型名称
        openAIRequest.setMessages(unifiedRequest.getMessages().stream()
                .map(msg -> {
                    OpenAIMessage openAIMsg = new OpenAIMessage();
                    openAIMsg.setRole(msg.getRole());
                    openAIMsg.setContent(msg.getContent());
                    return openAIMsg;
                })
                .collect(Collectors.toList()));
        openAIRequest.setTemperature(unifiedRequest.getTemperature());
        openAIRequest.setStream(unifiedRequest.getStream());
        return openAIRequest;
    }

    private Object toClaudeRequest(UnifiedChatRequest unifiedRequest, String vendorModel) {
        ClaudeChatRequest claudeRequest = new ClaudeChatRequest();
        claudeRequest.setModel(vendorModel);
        claudeRequest.setMessages(unifiedRequest.getMessages().stream()
                .map(msg -> {
                    ClaudeMessage claudeMsg = new ClaudeMessage();
                    claudeMsg.setRole(msg.getRole());
                    claudeMsg.setContent(msg.getContent());
                    return claudeMsg;
                })
                .collect(Collectors.toList()));
        claudeRequest.setTemperature(unifiedRequest.getTemperature());
        claudeRequest.setMaxTokens(unifiedRequest.getMaxTokens());
        claudeRequest.setStream(unifiedRequest.getStream());
        return claudeRequest;
    }

    private Object toGeminiRequest(UnifiedChatRequest unifiedRequest, String vendorModel) {
        GeminiChatRequest geminiRequest = new GeminiChatRequest();

        // 转换消息
        geminiRequest.setContents(unifiedRequest.getMessages().stream()
                .map(msg -> {
                    GeminiContent content = new GeminiContent();
                    content.setRole(msg.getRole());
                    content.setParts(msg.getContent());
                    return content;
                })
                .collect(Collectors.toList()));

        // 设置生成配置
        GeminiGenerationConfig config = new GeminiGenerationConfig();
        config.setTemperature(unifiedRequest.getTemperature());
        config.setMaxOutputTokens(unifiedRequest.getMaxTokens());
        geminiRequest.setGenerationConfig(config);

        return geminiRequest;
    }


    // 将厂商的流事件转换为OpenAI格式的JSON数据
    @Override
    public String toOpenAIStreamEvent(Object vendorStreamEvent, String providerName) {
        if (vendorStreamEvent == null) {
            return "";
        }

        // 获取厂商配置
        ProviderConfig.Provider providerConfig = this.providerConfig.getProviders().get(providerName);
        String providerType = providerConfig != null ? providerConfig.getType() : null;

        try {
            // 根据提供商类型转换流事件
            return switch (providerType != null ? providerType : "OpenAI") {
                case "OpenAI", "AzureOpenAI" -> {
                    // OpenAI 兼容格式，提取JSON数据
                    log.info("处理openai类型流事件");
                    yield extractJsonFromOpenAIEvent(vendorStreamEvent);
                }
                
                case "Anthropic" ->
                    // Claude 格式需要转换为 OpenAI 格式
                        convertClaudeStreamEvent(vendorStreamEvent);
                case "Gemini" ->
                    // Gemini 格式需要转换为 OpenAI 格式
                        convertGeminiStreamEvent(vendorStreamEvent);
                default ->
                    // 未知类型默认按 OpenAI 格式处理
                        extractJsonFromOpenAIEvent(vendorStreamEvent);
            };
        } catch (Exception e) {
            log.error("Failed to convert stream event for provider: {}", providerName, e);
            return "";
        }
    }

    /**
     * 从OpenAI格式的SSE事件中提取JSON数据
     */
    private String extractJsonFromOpenAIEvent(Object event) {
        String eventStr = event.toString();
        log.info("看看流事件：{}", eventStr);
        // 如果是 SSE 格式: "data: {...}"
        if (eventStr.startsWith("data: ")) {
            String jsonPart = eventStr.substring(6).trim();
            log.info("除去data: ");
            // 移除可能的换行符
            return jsonPart.replaceAll("\n", "");
        }

        // 如果已经是纯 JSON
        if (eventStr.trim().startsWith("{")) {
            return eventStr.trim();
        }

        // 特殊情况：[DONE]
        if (eventStr.contains("[DONE]")) {
            return "[DONE]";
        }

        // 其他格式直接返回
        return eventStr;
    }

    /**
     * 转换 Claude 格式的流事件为 OpenAI 格式JSON
     */
    private String convertClaudeStreamEvent(Object event) {
        String eventStr = event.toString();

        try {
            // Claude 使用 SSE 格式: event: xxx\ndata: {...}
            if (eventStr.contains("event:") && eventStr.contains("data:")) {
                String[] lines = eventStr.split("\n");
                String eventType = null;
                String dataContent = null;

                for (String line : lines) {
                    if (line.startsWith("event:")) {
                        eventType = line.substring(6).trim();
                    } else if (line.startsWith("data:")) {
                        dataContent = line.substring(5).trim();
                    }
                }

                // 处理不同的事件类型
                if ("content_block_delta".equals(eventType) && dataContent != null) {
                    var deltaData = JsonUtils.fromJson(dataContent, java.util.Map.class);
                    if (deltaData instanceof java.util.Map) {
                        Object delta = ((java.util.Map<?, ?>) deltaData).get("delta");
                        if (delta instanceof java.util.Map) {
                            Object text = ((java.util.Map<?, ?>) delta).get("text");
                            if (text != null) {
                                return createOpenAIStreamChunkJson(text.toString(), false);
                            }
                        }
                    }
                } else if ("message_stop".equals(eventType)) {
                    // Claude 流结束
                    return "[DONE]";
                }
            }

            // 简单格式处理
            if (eventStr.startsWith("data: ")) {
                String jsonData = eventStr.substring(6).trim();

                if ("[DONE]".equals(jsonData) || jsonData.isEmpty()) {
                    return "[DONE]";
                }

                String content = extractContentFromClaude(jsonData);
                if (content != null && !content.isEmpty()) {
                    return createOpenAIStreamChunkJson(content, false);
                }
            }

            return "";
        } catch (Exception e) {
            log.warn("Failed to convert Claude stream event: {}", eventStr, e);
            return "";
        }
    }

    /**
     * 转换 Gemini 格式的流事件为 OpenAI 格式JSON
     */
    private String convertGeminiStreamEvent(Object event) {
        String eventStr = event.toString();

        try {
            if (eventStr.startsWith("data: ")) {
                String jsonData = eventStr.substring(6).trim();

                if (jsonData.isEmpty() || "{}".equals(jsonData)) {
                    return "";
                }

                // Gemini 响应格式: {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}
                var geminiResponse = JsonUtils.fromJson(jsonData, java.util.Map.class);
                if (geminiResponse instanceof java.util.Map) {
                    Object candidates = ((java.util.Map<?, ?>) geminiResponse).get("candidates");
                    if (candidates instanceof java.util.List && !((java.util.List<?>) candidates).isEmpty()) {
                        Object firstCandidate = ((java.util.List<?>) candidates).get(0);
                        if (firstCandidate instanceof java.util.Map) {
                            Object content = ((java.util.Map<?, ?>) firstCandidate).get("content");
                            if (content instanceof java.util.Map) {
                                Object parts = ((java.util.Map<?, ?>) content).get("parts");
                                if (parts instanceof java.util.List && !((java.util.List<?>) parts).isEmpty()) {
                                    Object firstPart = ((java.util.List<?>) parts).get(0);
                                    if (firstPart instanceof java.util.Map) {
                                        Object text = ((java.util.Map<?, ?>) firstPart).get("text");
                                        if (text != null) {
                                            return createOpenAIStreamChunkJson(text.toString(), false);
                                        }
                                    }
                                }
                            }

                            // 检查是否完成
                            Object finishReason = ((java.util.Map<?, ?>) firstCandidate).get("finishReason");
                            if (finishReason != null) {
                                return "[DONE]";
                            }
                        }
                    }
                }
            }

            return "";
        } catch (Exception e) {
            log.warn("Failed to convert Gemini stream event: {}", eventStr, e);
            return "";
        }
    }

    /**
     * 创建标准的 OpenAI 流式响应块（纯JSON格式）
     */
    private String createOpenAIStreamChunkJson(String content, boolean finished) {
        OpenAIStreamResponse response = createOpenAIStreamResponse();

        OpenAIStreamChoice choice = new OpenAIStreamChoice();
        choice.setIndex(0);

        if (finished) {
            choice.setFinishReason("stop");
            choice.setDelta(new OpenAIStreamDelta()); // 空 delta
        } else {
            OpenAIStreamDelta delta = new OpenAIStreamDelta();
            delta.setContent(content);
            choice.setDelta(delta);
        }

        response.setChoices(List.of(choice));

        return JsonUtils.toJson(response);
    }

    /**
     * 创建基础的 OpenAI 流响应对象
     */
    private OpenAIStreamResponse createOpenAIStreamResponse() {
        OpenAIStreamResponse response = new OpenAIStreamResponse();
        response.setId("chatcmpl-" + System.nanoTime());
        response.setObject("chat.completion.chunk");
        response.setCreated(System.currentTimeMillis() / 1000);
        response.setModel("robella-proxy");
        return response;
    }

    /**
     * 从 Claude 响应中提取内容
     */
    private String extractContentFromClaude(String jsonData) {
        try {
            var jsonNode = JsonUtils.fromJson(jsonData, java.util.Map.class);
            if (jsonNode instanceof java.util.Map) {
                java.util.Map<?, ?> jsonMap = (java.util.Map<?, ?>) jsonNode;

                // Claude 可能的响应格式
                // 1. {"delta": {"text": "..."}}
                Object delta = jsonMap.get("delta");
                if (delta instanceof java.util.Map) {
                    Object text = ((java.util.Map<?, ?>) delta).get("text");
                    if (text != null) {
                        return text.toString();
                    }
                }

                // 2. {"text": "..."}
                Object text = jsonMap.get("text");
                if (text != null) {
                    return text.toString();
                }

                // 3. {"content": "..."}
                Object content = jsonMap.get("content");
                if (content != null) {
                    return content.toString();
                }

                // 4. {"message": {"content": "..."}}
                Object message = jsonMap.get("message");
                if (message instanceof java.util.Map) {
                    Object messageContent = ((java.util.Map<?, ?>) message).get("content");
                    if (messageContent != null) {
                        return messageContent.toString();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.debug("Failed to extract content from Claude response", e);
            return null;
        }
    }
}
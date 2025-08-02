package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.model.openai.*;
import org.elmo.robella.util.JsonUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransformServiceImpl implements TransformService {

    private final ProviderConfig providerConfig;

    @Override
    public Object toVendor(ChatCompletionRequest request, String providerName) {
        // 获取厂商配置
        ProviderConfig.Provider provider = providerConfig.getProviders().get(providerName);
        
        // 将用户模型名称映射到厂商模型名称
        String vendorModel = mapToVendorModel(request.getModel(), provider);
        
        // 获取提供商类型
        String providerType = provider != null ? provider.getType() : null;
        if (providerType == null) {
            // 如果类型为空，默认使用OpenAI格式
            return toOpenAIRequest(request, vendorModel);
        }

        // 根据提供商类型转换为特定格式
        return switch (providerType) {
            case "OpenAI", "AzureOpenAI" -> toOpenAIRequest(request, vendorModel);
            case "Anthropic" -> toClaudeRequest(request, vendorModel);
            case "Gemini" -> toGeminiRequest(request, vendorModel);
            default -> toOpenAIRequest(request, vendorModel);
        };
    }

    @Override
    public ChatCompletionResponse toOpenAI(Object vendorResponse, String providerName) {
        // 获取厂商配置
        ProviderConfig.Provider provider = providerConfig.getProviders().get(providerName);
        String providerType = provider != null ? provider.getType() : null;

        try {
            // 根据提供商类型转换响应
            return switch (providerType != null ? providerType : "OpenAI") {
                case "OpenAI", "AzureOpenAI" -> 
                    // OpenAI 兼容格式，直接返回
                    (ChatCompletionResponse) vendorResponse;
                case "Anthropic" -> 
                    // Claude 格式需要转换为 OpenAI 格式
                    convertClaudeResponse(vendorResponse);
                case "Gemini" -> 
                    // Gemini 格式需要转换为 OpenAI 格式
                    convertGeminiResponse(vendorResponse);
                default -> 
                    // 未知类型默认按 OpenAI 格式处理
                    (ChatCompletionResponse) vendorResponse;
            };
        } catch (Exception e) {
            log.error("Failed to convert response for provider: {}", providerName, e);
            throw new RuntimeException("Response conversion failed", e);
        }
    }

    @Override
    public String toOpenAIStreamEvent(Object vendorStreamEvent, String providerName) {
        if (vendorStreamEvent == null) {
            return "";
        }

        // 获取厂商配置
        ProviderConfig.Provider provider = providerConfig.getProviders().get(providerName);
        String providerType = provider != null ? provider.getType() : null;

        try {
            // 根据提供商类型转换流事件
            return switch (providerType != null ? providerType : "OpenAI") {
                case "OpenAI", "AzureOpenAI" ->
                    extractJsonFromOpenAIEvent(vendorStreamEvent);
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
     * 将用户请求的模型名称映射到厂商的实际模型名称
     */
    private String mapToVendorModel(String userModel, ProviderConfig.Provider provider) {
        if (provider == null || provider.getModels() == null) {
            return userModel;
        }
        
        // 查找用户模型对应的厂商模型
        for (ProviderConfig.Model model : provider.getModels()) {
            if (model.getName().equals(userModel)) {
                return model.getVendorModel() != null ? model.getVendorModel() : userModel;
            }
        }
        
        return userModel;
    }

    /**
     * 转换为OpenAI格式（主要用于模型名称映射）
     */
    private Object toOpenAIRequest(ChatCompletionRequest request, String vendorModel) {
        ChatCompletionRequest vendorRequest = new ChatCompletionRequest();
        BeanUtils.copyProperties(request, vendorRequest);
        vendorRequest.setModel(vendorModel); // 使用厂商模型名称
        return vendorRequest;
    }

    /**
     * 转换为Claude格式
     */
    private Object toClaudeRequest(ChatCompletionRequest request, String vendorModel) {
        // 这里需要实现转换为Claude特定格式的逻辑
        // 暂时返回一个简化的实现
        return new Object(); // TODO: 实现Claude请求格式转换
    }

    /**
     * 转换为Gemini格式
     */
    private Object toGeminiRequest(ChatCompletionRequest request, String vendorModel) {
        // 这里需要实现转换为Gemini特定格式的逻辑
        // 暂时返回一个简化的实现
        return new Object(); // TODO: 实现Gemini请求格式转换
    }

    /**
     * 转换Claude响应为OpenAI格式
     */
    private ChatCompletionResponse convertClaudeResponse(Object vendorResponse) {
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setId("claude-" + System.nanoTime());
        response.setObject("chat.completion");
        response.setCreated(System.currentTimeMillis() / 1000);
        response.setModel("claude-model");

        Choice choice = new Choice();
        choice.setIndex(0);
        ChatMessage message = new ChatMessage();
        message.setRole("assistant");
        message.setContent("Claude response (conversion needed)");
        choice.setMessage(message);
        choice.setFinishReason("stop");

        response.setChoices(Collections.singletonList(choice));

        Usage usage = new Usage();
        usage.setPromptTokens(0);
        usage.setCompletionTokens(0);
        usage.setTotalTokens(0);
        response.setUsage(usage);

        return response;
    }

    /**
     * 转换Gemini响应为OpenAI格式
     */
    private ChatCompletionResponse convertGeminiResponse(Object vendorResponse) {
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setId("gemini-" + System.nanoTime());
        response.setObject("chat.completion");
        response.setCreated(System.currentTimeMillis() / 1000);
        response.setModel("gemini-model");

        Choice choice = new Choice();
        choice.setIndex(0);
        ChatMessage message = new ChatMessage();
        message.setRole("assistant");
        message.setContent("Gemini response (conversion needed)");
        choice.setMessage(message);
        choice.setFinishReason("stop");

        response.setChoices(Collections.singletonList(choice));

        Usage usage = new Usage();
        usage.setPromptTokens(0);
        usage.setCompletionTokens(0);
        usage.setTotalTokens(0);
        response.setUsage(usage);

        return response;
    }

    /**
     * 从OpenAI格式的SSE事件中提取JSON数据
     */
    private String extractJsonFromOpenAIEvent(Object event) {
        // 其他格式直接返回
        return event.toString();
    }

    /**
     * 转换Claude格式的流事件为OpenAI格式JSON
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
                    // 解析数据并转换为OpenAI格式
                    return createOpenAIStreamChunkJson("Claude content", false);
                } else if ("message_stop".equals(eventType)) {
                    return "[DONE]";
                }
            }

            // 简单格式处理
            if (eventStr.startsWith("data: ")) {
                String jsonData = eventStr.substring(6).trim();

                if ("[DONE]".equals(jsonData) || jsonData.isEmpty()) {
                    return "[DONE]";
                }

                return createOpenAIStreamChunkJson("Claude content", false);
            }

            return "";
        } catch (Exception e) {
            log.warn("Failed to convert Claude stream event: {}", eventStr, e);
            return "";
        }
    }

    /**
     * 转换Gemini格式的流事件为OpenAI格式JSON
     */
    private String convertGeminiStreamEvent(Object event) {
        String eventStr = event.toString();

        try {
            if (eventStr.startsWith("data: ")) {
                String jsonData = eventStr.substring(6).trim();

                if (jsonData.isEmpty() || "{}".equals(jsonData)) {
                    return "";
                }

                return createOpenAIStreamChunkJson("Gemini content", false);
            }

            return "";
        } catch (Exception e) {
            log.warn("Failed to convert Gemini stream event: {}", eventStr, e);
            return "";
        }
    }

    /**
     * 创建标准的OpenAI流式响应块（纯JSON格式）
     */
    private String createOpenAIStreamChunkJson(String content, boolean finished) {
        ChatCompletionChunk response = new ChatCompletionChunk();
        response.setId("chatcmpl-" + System.nanoTime());
        response.setObject("chat.completion.chunk");
        response.setCreated(System.currentTimeMillis() / 1000);
        response.setModel("robella-proxy");

        Choice choice = new Choice();
        choice.setIndex(0);

        if (finished) {
            choice.setFinishReason("stop");
            choice.setDelta(new Delta()); // 空 delta
        } else {
            Delta delta = new Delta();
            delta.setContent(content);
            choice.setDelta(delta);
        }

        response.setChoices(List.of(choice));

        return JsonUtils.toJson(response);
    }
}

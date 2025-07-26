package org.elmo.robella.service;

import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.model.common.OpenAIMessage;
import org.elmo.robella.model.common.UnifiedMessage;
import org.elmo.robella.model.request.ClaudeChatRequest;
import org.elmo.robella.model.request.GeminiChatRequest;
import org.elmo.robella.model.request.OpenAIChatRequest;
import org.elmo.robella.model.request.UnifiedChatRequest;
import org.elmo.robella.model.response.OpenAIChatResponse;
import org.elmo.robella.model.response.OpenAIChoice;
import org.elmo.robella.model.response.OpenAIUsage;
import org.elmo.robella.model.response.UnifiedChatResponse;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransformServiceImpl implements TransformService {

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
        return unifiedRequest;
    }

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
        
        return openAIResponse;
    }

    @Override
    public Object toVendor(UnifiedChatRequest unifiedRequest, String providerName) {
        // 根据提供商名称转换为特定格式
        switch (providerName) {
            case "openai":
            case "deepseek":
            case "openrouter":
            case "qwen":
                return toOpenAIRequest(unifiedRequest);
            case "claude":
                return toClaudeRequest(unifiedRequest);
            case "gemini":
                return toGeminiRequest(unifiedRequest);
            default:
                // 默认使用OpenAI格式
                return toOpenAIRequest(unifiedRequest);
        }
    }

    private Object toOpenAIRequest(UnifiedChatRequest unifiedRequest) {
        OpenAIChatRequest openAIRequest = new OpenAIChatRequest();
        openAIRequest.setModel(unifiedRequest.getModel());
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

    private Object toClaudeRequest(UnifiedChatRequest unifiedRequest) {
        ClaudeChatRequest claudeRequest = new ClaudeChatRequest();
        // 转换逻辑
        return claudeRequest;
    }

    private Object toGeminiRequest(UnifiedChatRequest unifiedRequest) {
        GeminiChatRequest geminiRequest = new GeminiChatRequest();
        // 转换逻辑
        return geminiRequest;
    }

    @Override
    public Object toOpenAIStreamEvent(Object vendorStreamEvent) {
        // 实现流事件转换逻辑
        return vendorStreamEvent.toString();
    }
}
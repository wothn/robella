package org.elmo.robella.service.transform;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.config.ProviderType;
import org.elmo.robella.model.internal.*;
import org.elmo.robella.util.ConfigUtils;

import org.elmo.robella.model.anthropic.core.*;
import org.elmo.robella.model.anthropic.stream.*;
import org.elmo.robella.model.anthropic.content.*;
import org.elmo.robella.util.AnthropicTransformUtils;

import org.elmo.robella.model.anthropic.content.AnthropicThinkingContent;
import org.elmo.robella.model.openai.core.Choice;
import org.elmo.robella.model.openai.core.OpenAIMessage;
import org.elmo.robella.model.openai.core.Usage;
import org.elmo.robella.model.openai.stream.Delta;
import org.elmo.robella.model.openai.tool.ToolCall;
import org.elmo.robella.model.openai.content.ImageUrl;
import org.elmo.robella.model.openai.content.OpenAIContent;
import org.elmo.robella.model.openai.content.OpenAIImageContent;
import org.elmo.robella.model.openai.content.OpenAITextContent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


/**
 * Anthropic Messages API 转换实现，不处理流式转换。
 */
@Slf4j
@RequiredArgsConstructor
public class AnthropicTransform implements VendorTransform {
    
    private final ConfigUtils configUtils;


    @Override
    public String type() {
        return ProviderType.Anthropic.getName();
    }

    @Override
    public UnifiedChatRequest vendorRequestToUnified(Object vendorRequest) {
        if (!(vendorRequest instanceof AnthropicChatRequest req)) {
            return null;
        }
        // 设置基础字段
        UnifiedChatRequest unifiedRequest = new UnifiedChatRequest();
        AnthropicTransformUtils.convertBaseToUnified(req, unifiedRequest);

        // 设置tools
        AnthropicTransformUtils.convertToolsToUnified(req, unifiedRequest);

        // 设置tool_choice
        AnthropicTransformUtils.convertToolChoiceToUnified(req, unifiedRequest);

        // 设置思考字段
        AnthropicTransformUtils.convertThinkingToUnified(req, unifiedRequest);

        // 设置配置思考字段
        unifiedRequest.getTempFields().put("config_thinking", configUtils.getThinkingField(unifiedRequest.getProviderName(), unifiedRequest.getModel()));

        // 转换messages
        AnthropicTransformUtils.convertMessagesToUnified(req, unifiedRequest);

        // 处理系统消息（Anthropic的system字段转换为OpenAI格式的系统消息）
        AnthropicTransformUtils.convertSystemToUnified(req, unifiedRequest);

        return unifiedRequest;
    }

    @Override
    public Object unifiedToVendorRequest(UnifiedChatRequest unifiedRequest) {
        AnthropicChatRequest anthropicRequest = new AnthropicChatRequest();
        
        // 设置基础字段
        AnthropicTransformUtils.convertBaseToAnthropic(unifiedRequest, anthropicRequest);

        // 设置tools
        AnthropicTransformUtils.convertToolsToAnthropic(unifiedRequest, anthropicRequest);

        // 设置tool_choice
        AnthropicTransformUtils.convertToolChoiceToAnthropic(unifiedRequest, anthropicRequest);

        // 设置思考字段
        AnthropicTransformUtils.convertThinkingToAnthropic(unifiedRequest, anthropicRequest);

        // 转换messages
        AnthropicTransformUtils.convertMessagesToAnthropic(unifiedRequest, anthropicRequest);

        // 处理系统消息（从OpenAI格式的系统消息转换为Anthropic的system字段）
        AnthropicTransformUtils.convertSystemToAnthropic(unifiedRequest, anthropicRequest);

        return anthropicRequest;
    }


    @Override
    public UnifiedChatResponse vendorResponseToUnified(Object vendorResponse) {
        if (!(vendorResponse instanceof AnthropicMessage message)) {
            return null;
        }

        UnifiedChatResponse unifiedResponse = new UnifiedChatResponse();
        
        // 设置基础字段
        unifiedResponse.setId(message.getId());
        unifiedResponse.setModel(message.getModel());
        unifiedResponse.setObject("chat.completion");
        unifiedResponse.setCreated(System.currentTimeMillis() / 1000); // Unix timestamp in seconds

        // 转换内容
        Choice choice = new Choice();
        choice.setIndex(0);
        choice.setFinishReason(convertStopReasonToFinishReason(message.getStopReason()));
        
        OpenAIMessage openAIMessage = new OpenAIMessage();
        openAIMessage.setRole(message.getRole());
        
        // 转换内容块
        List<OpenAIContent> openAIContents = new ArrayList<>();
        StringBuilder reasoningContent = new StringBuilder();
        
        if (message.getContent() != null) {
            for (AnthropicContent content : message.getContent()) {
                if (content instanceof AnthropicThinkingContent thinkingContent) {
                    // 处理思考内容
                    reasoningContent.append(thinkingContent.getThinking());
                } else {
                    OpenAIContent openAIContent = AnthropicTransformUtils.convertAnthropicContentToOpenAI(content);
                    if (openAIContent != null) {
                        openAIContents.add(openAIContent);
                    }
                }
            }
        }
        
        openAIMessage.setContent(openAIContents);
        if (!reasoningContent.isEmpty()) {
            openAIMessage.setReasoningContent(reasoningContent.toString());
        }
        
        choice.setMessage(openAIMessage);
        unifiedResponse.setChoices(Collections.singletonList(choice));

        // 转换使用量统计
        if (message.getUsage() != null) {
            Usage usage = new Usage();
            usage.setPromptTokens(message.getUsage().getInputTokens());
            usage.setCompletionTokens(message.getUsage().getOutputTokens());
            usage.setTotalTokens(
                (message.getUsage().getInputTokens() != null ? message.getUsage().getInputTokens() : 0) +
                (message.getUsage().getOutputTokens() != null ? message.getUsage().getOutputTokens() : 0)
            );
            unifiedResponse.setUsage(usage);
        }

        return unifiedResponse;
    }

    /**
     * 将Anthropic的停止原因转换为OpenAI的完成原因
     * @param stopReason Anthropic的停止原因
     * @return OpenAI的完成原因
     */
    private String convertStopReasonToFinishReason(String stopReason) {
        if (stopReason == null) {
            return null;
        }
        
        return switch (stopReason) {
            case "end_turn" -> "stop";
            case "max_tokens" -> "length";
            case "stop_sequence" -> "stop";
            case "tool_use" -> "tool_calls";
            default -> stopReason;
        };
    }

    @Override
    public Object unifiedToVendorResponse(UnifiedChatResponse unifiedResponse) {
        if (unifiedResponse == null) {
            return null;
        }

        AnthropicMessage anthropicMessage = new AnthropicMessage();
        
        // 设置基础字段
        anthropicMessage.setId(unifiedResponse.getId());
        anthropicMessage.setModel(unifiedResponse.getModel());
        anthropicMessage.setType("message");
        
        // 设置使用量统计
        if (unifiedResponse.getUsage() != null) {
            AnthropicUsage anthropicUsage = new AnthropicUsage();
            anthropicUsage.setInputTokens(unifiedResponse.getUsage().getPromptTokens());
            anthropicUsage.setOutputTokens(unifiedResponse.getUsage().getCompletionTokens());
            anthropicMessage.setUsage(anthropicUsage);
        }

        // 处理choices
        if (unifiedResponse.getChoices() != null && !unifiedResponse.getChoices().isEmpty()) {
            // 使用第一个choice作为主要响应
            Choice choice = unifiedResponse.getChoices().get(0);
            
            // 设置角色
            if (choice.getMessage() != null) {
                anthropicMessage.setRole(choice.getMessage().getRole());
                
                // 转换内容
                List<AnthropicContent> anthropicContents = new ArrayList<>();
                
                // 处理推理内容
                if (choice.getMessage().getReasoningContent() != null && !choice.getMessage().getReasoningContent().isEmpty()) {
                    AnthropicThinkingContent thinkingContent = new AnthropicThinkingContent();
                    thinkingContent.setThinking(choice.getMessage().getReasoningContent());
                    anthropicContents.add(thinkingContent);
                }
                
                // 处理其他内容
                if (choice.getMessage().getContent() != null) {
                    for (OpenAIContent openAIContent : choice.getMessage().getContent()) {
                        AnthropicContent anthropicContent = AnthropicTransformUtils.convertOpenAIContentToAnthropic(openAIContent);
                        if (anthropicContent != null) {
                            anthropicContents.add(anthropicContent);
                        }
                    }
                }
                
                anthropicMessage.setContent(anthropicContents);
                
                // 设置停止原因
                anthropicMessage.setStopReason(convertFinishReasonToStopReason(choice.getFinishReason()));
            }
        }

        return anthropicMessage;
    }

    /**
     * 将OpenAI的完成原因转换为Anthropic的停止原因
     * @param finishReason OpenAI的完成原因
     * @return Anthropic的停止原因
     */
    private String convertFinishReasonToStopReason(String finishReason) {
        if (finishReason == null) {
            return null;
        }
        
        return switch (finishReason) {
            case "stop" -> "end_turn";
            case "length" -> "max_tokens";
            case "tool_calls" -> "tool_use";
            default -> finishReason;
        };
    }
}

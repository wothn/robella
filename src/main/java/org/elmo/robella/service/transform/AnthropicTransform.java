package org.elmo.robella.service.transform;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.model.internal.*;

import org.elmo.robella.model.anthropic.core.*;
import org.elmo.robella.model.common.EndpointType;
import org.elmo.robella.util.AnthropicTransformUtils;
import org.springframework.stereotype.Component;
import org.elmo.robella.model.openai.core.Choice;
import org.elmo.robella.model.openai.core.OpenAIMessage;
import org.elmo.robella.model.openai.content.OpenAIContent;
import org.elmo.robella.model.openai.content.OpenAITextContent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


/**
 * Anthropic Messages API 转换实现，不处理流式转换。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnthropicTransform implements VendorTransform<AnthropicChatRequest, AnthropicMessage> {


    @Override
    public EndpointType type() {
        return EndpointType.ANTHROPIC;
    }

    @Override
    public UnifiedChatRequest endpointToUnifiedRequest(AnthropicChatRequest vendorRequest) {
        if (vendorRequest == null) {
            return null;
        }
        AnthropicChatRequest req = vendorRequest;

        UnifiedChatRequest unifiedRequest = new UnifiedChatRequest();
        unifiedRequest.setModel(req.getModel());
        unifiedRequest.setStream(req.getStream());
        unifiedRequest.setMaxTokens(req.getMaxTokens());
        unifiedRequest.setTemperature(req.getTemperature());
        unifiedRequest.setTopP(req.getTopP());
        unifiedRequest.setTopK(req.getTopK());
        unifiedRequest.setStop(req.getStopSequences());

        // 转换消息
        if (req.getMessages() != null) {
            List<OpenAIMessage> openAiMessages = new ArrayList<>();
            for (AnthropicMessage anthropicMessage : req.getMessages()) {
                openAiMessages.add(AnthropicTransformUtils.anthropicToOpenAiMessage(anthropicMessage));
            }
            unifiedRequest.setMessages(openAiMessages);
        }

        // 转换工具
        unifiedRequest.setTools(AnthropicTransformUtils.anthropicToOpenAiTools(req.getTools()));
        unifiedRequest.setToolChoice(AnthropicTransformUtils.anthropicToOpenAiToolChoice(req.getToolChoice()));

        // 设置系统提示
        if (req.getSystem() != null) {
            OpenAIMessage systemMessage = new OpenAIMessage();
            systemMessage.setRole("system");
            OpenAITextContent textContent = new OpenAITextContent();
            textContent.setType("text");
            textContent.setText(req.getSystem());
            systemMessage.setContent(Collections.singletonList(textContent));

            // 将系统消息添加到消息列表的开头
            if (unifiedRequest.getMessages() == null) {
                unifiedRequest.setMessages(new ArrayList<>());
            }
            unifiedRequest.getMessages().add(0, systemMessage);
        }
        log.debug("转换后的请求：{}", unifiedRequest);
        return unifiedRequest;
    }

    @Override
    public AnthropicChatRequest unifiedToEndpointRequest(UnifiedChatRequest unifiedRequest) {
        AnthropicChatRequest anthropicRequest = new AnthropicChatRequest();
        anthropicRequest.setModel(unifiedRequest.getModel());
        anthropicRequest.setStream(unifiedRequest.getStream());
        anthropicRequest.setMaxTokens(unifiedRequest.getMaxTokens());
        anthropicRequest.setTemperature(unifiedRequest.getTemperature());
        anthropicRequest.setTopP(unifiedRequest.getTopP());
        anthropicRequest.setTopK(unifiedRequest.getTopK());
        anthropicRequest.setStopSequences(unifiedRequest.getStop());

        // 转换消息
        if (unifiedRequest.getMessages() != null) {
            List<AnthropicMessage> anthropicMessages = new ArrayList<>();
            for (OpenAIMessage openAiMessage : unifiedRequest.getMessages()) {
                // 处理系统消息的特殊情况
                if ("system".equals(openAiMessage.getRole())) {
                    if (openAiMessage.getContent() != null && !openAiMessage.getContent().isEmpty()) {
                        OpenAIContent firstContent = openAiMessage.getContent().get(0);
                        if (firstContent instanceof OpenAITextContent) {
                            anthropicRequest.setSystem(((OpenAITextContent) firstContent).getText());
                        }
                    }
                } else {
                    anthropicMessages.add(AnthropicTransformUtils.openAiToAnthropicMessage(openAiMessage));
                }
            }
            anthropicRequest.setMessages(anthropicMessages);
        }

        // 转换工具
        anthropicRequest.setTools(AnthropicTransformUtils.openAiToAnthropicTools(unifiedRequest.getTools()));

        // 转换工具选择策略
        anthropicRequest.setToolChoice(AnthropicTransformUtils.openAiToAnthropicToolChoice(unifiedRequest.getToolChoice()));

        return anthropicRequest;
    }


    @Override
    public UnifiedChatResponse endpointToUnifiedResponse(AnthropicMessage vendorResponse) {
        if (vendorResponse == null) {
            return null;
        }
        AnthropicMessage message = vendorResponse;

        UnifiedChatResponse unifiedResponse = new UnifiedChatResponse();
        unifiedResponse.setId(message.getId());
        unifiedResponse.setModel(message.getModel());
        unifiedResponse.setObject("chat.completion");
        unifiedResponse.setCreated(System.currentTimeMillis() / 1000);

        // 转换使用量统计
        unifiedResponse.setUsage(AnthropicTransformUtils.anthropicToOpenAiUsage(message.getUsage()));

        // 转换消息内容
        Choice choice = new Choice();
        choice.setIndex(0);
        choice.setMessage(AnthropicTransformUtils.anthropicToOpenAiMessage(message));

        // 设置结束原因
        if (message.getStopReason() != null) {
            switch (message.getStopReason()) {
                case "end_turn":
                    choice.setFinishReason("stop");
                    break;
                case "max_tokens":
                    choice.setFinishReason("length");
                    break;
                case "stop_sequence":
                    choice.setFinishReason("stop");
                    break;
                default:
                    choice.setFinishReason(message.getStopReason());
                    break;
            }
        }

        unifiedResponse.setChoices(Collections.singletonList(choice));

        return unifiedResponse;
    }


    @Override
    public AnthropicMessage unifiedToEndpointResponse(UnifiedChatResponse unifiedResponse) {
        if (unifiedResponse == null || unifiedResponse.getChoices() == null || unifiedResponse.getChoices().isEmpty()) {
            return null;
        }

        AnthropicMessage anthropicMessage = new AnthropicMessage();
        anthropicMessage.setType("message");
        anthropicMessage.setId(unifiedResponse.getId() != null ? unifiedResponse.getId() : "msg_" + UUID.randomUUID().toString().replace("-", ""));
        anthropicMessage.setModel(unifiedResponse.getModel());

        // 转换第一个选择项
        Choice choice = unifiedResponse.getChoices().get(0);
        if (choice.getMessage() != null) {
            // 转换消息内容
            AnthropicMessage convertedMessage = AnthropicTransformUtils.openAiToAnthropicMessage(choice.getMessage());
            if (convertedMessage != null) {
                anthropicMessage.setRole(convertedMessage.getRole());
                anthropicMessage.setContent(convertedMessage.getContent());
            }
        }

        // 转换使用量统计
        if (unifiedResponse.getUsage() != null) {
            AnthropicUsage anthropicUsage = new AnthropicUsage();
            anthropicUsage.setInputTokens(unifiedResponse.getUsage().getPromptTokens());
            anthropicUsage.setOutputTokens(unifiedResponse.getUsage().getCompletionTokens());
            if (unifiedResponse.getUsage().getPromptTokens() != null && unifiedResponse.getUsage().getCompletionTokens() != null) {
                // Anthropic没有total_tokens字段，但我们可以计算它
            }
            anthropicMessage.setUsage(anthropicUsage);
        }

        // 设置停止原因
        if (choice.getFinishReason() != null) {
            switch (choice.getFinishReason()) {
                case "stop":
                    anthropicMessage.setStopReason("end_turn");
                    break;
                case "length":
                    anthropicMessage.setStopReason("max_tokens");
                    break;
                default:
                    anthropicMessage.setStopReason(choice.getFinishReason());
                    break;
            }
        }

        return anthropicMessage;
    }

}

package org.elmo.robella.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.anthropic.core.*;
import org.elmo.robella.model.anthropic.tool.*;
import org.elmo.robella.model.anthropic.content.*;
import org.elmo.robella.model.internal.ThinkingOptions;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.openai.core.OpenAIMessage;
import org.elmo.robella.model.openai.core.StreamOptions;
import org.elmo.robella.model.openai.tool.Function;
import org.elmo.robella.model.openai.tool.Tool;
import org.elmo.robella.model.openai.tool.ToolCall;
import org.elmo.robella.model.openai.tool.ToolChoice;
import org.elmo.robella.model.openai.content.*;

import java.util.*;

/**
 * Anthropic 与 Unified 模型转换工具类
 */
@Slf4j
public class AnthropicTransformUtils {

    public static void convertBaseToUnified(AnthropicChatRequest req, UnifiedChatRequest unifiedRequest) {
        unifiedRequest.setModel(req.getModel());
        unifiedRequest.setStream(req.getStream());
        StreamOptions streamOptions = new StreamOptions();
        streamOptions.setIncludeUsage(true);
        unifiedRequest.setStreamOptions(streamOptions);
        unifiedRequest.setMaxTokens(req.getMaxTokens());
        unifiedRequest.setTemperature(req.getTemperature());
        unifiedRequest.setTopP(req.getTopP());
        unifiedRequest.setTopK(req.getTopK());
        unifiedRequest.setStop(req.getStopSequences());
    }

    public static void convertToolsToUnified(AnthropicChatRequest req, UnifiedChatRequest unifiedRequest) {
        List<Tool> tools = new ArrayList<>();
        for (AnthropicTool tool : req.getTools()) {
            if (tool instanceof AnthropicCustomTool customTool) {
                Tool openAITool = new Tool();
                openAITool.setType("function");
                Function function = new Function();
                function.setName(customTool.getName());
                function.setDescription(customTool.getDescription());
                function.setParameters(customTool.getInputSchema());
                openAITool.setFunction(function);
                tools.add(openAITool);
            }
            // TODO: Anthropic的服务端工具暂时不支持，如computer、bash、text_editor
        }
        unifiedRequest.setTools(tools);
    }

    public static void convertToolChoiceToUnified(AnthropicChatRequest req, UnifiedChatRequest unifiedRequest) {
        ToolChoice toolChoice = ToolChoice.AUTO; // 默认值

        AnthropicToolChoice anthropicToolChoice = req.getToolChoice();

        if (anthropicToolChoice != null) {
            String type = anthropicToolChoice.getType();
            if ("auto".equals(type)) {
                toolChoice = ToolChoice.AUTO;
            } else if ("any".equals(type)) {
                toolChoice = ToolChoice.REQUIRED;
            } else if ("tool".equals(type)) {
                // 如果是指定工具，设置工具名称
                if (anthropicToolChoice.getName() != null) {
                    toolChoice = ToolChoice.ofFunction(anthropicToolChoice.getName());
                }
                // 如果 name 为 null，保持默认的 AUTO
            } else if ("none".equals(type)) {
                toolChoice = ToolChoice.NONE;
            }
            // 其他未知类型保持默认的 AUTO
        }
        unifiedRequest.setToolChoice(toolChoice);
    }

    public static void convertThinkingToUnified(AnthropicChatRequest req, UnifiedChatRequest unifiedRequest) {
        AnthropicThinking thinking = req.getThinking();
        ThinkingOptions thinkingOptions = new ThinkingOptions();
        thinkingOptions.setType(thinking.getType());
        thinkingOptions.setThinkingBudget(thinking.getBudgetTokens());
        unifiedRequest.setThinkingOptions(thinkingOptions);
    }

    public static void convertMessagesToUnified(AnthropicChatRequest req, UnifiedChatRequest unifiedRequest) {
        List<AnthropicMessage> messages = req.getMessages();
        List<OpenAIMessage> unifiedMessages = new ArrayList<>();
        
        for (AnthropicMessage message : messages) {
            OpenAIMessage openAIMessage = new OpenAIMessage();

            // 转换角色，Anthropic只支持user和assistant角色
            String role = message.getRole();
            openAIMessage.setRole(role);

            // 转换内容
            List<OpenAIContent> openAIContents = new ArrayList<>();
            List<ToolCall> toolCalls = new ArrayList<>();
            
            for (AnthropicContent anthropicContent : message.getContent()) {
                if (anthropicContent instanceof AnthropicToolUseContent toolUseContent) {
                    // toolUseContent 转换为 ToolCall
                    ToolCall toolCall = convertAnthropicToolUseToToolCall(toolUseContent);
                    if (toolCall != null) {
                        toolCalls.add(toolCall);
                    }
                } else if (anthropicContent instanceof AnthropicToolResultContent toolResultContent) {
                    // ToolResultContent 应该转换为单独的 tool 角色消息
                    // 而不是作为当前消息的内容部分
                    OpenAIMessage toolMessage = convertToolResultToToolMessage(toolResultContent);
                    if (toolMessage != null) {
                        unifiedMessages.add(toolMessage);
                    }
                } else {
                    OpenAIContent openAIContent = convertAnthropicContentToOpenAI(anthropicContent);
                    if (openAIContent != null) {
                        openAIContents.add(openAIContent);
                    }
                }
            }
            openAIMessage.setContent(openAIContents);
            openAIMessage.setToolCalls(toolCalls.isEmpty() ? null : toolCalls);

            unifiedMessages.add(openAIMessage);
        }
        unifiedRequest.setMessages(unifiedMessages);
    }

    private static OpenAIContent convertAnthropicContentToOpenAI(AnthropicContent anthropicContent) {
        if (anthropicContent instanceof AnthropicTextContent textContent) {
            // 转换文本内容
            OpenAITextContent openAITextContent = new OpenAITextContent();
            openAITextContent.setType("text");
            openAITextContent.setText(textContent.getText());
            return openAITextContent;
        } else if (anthropicContent instanceof AnthropicImageContent imageContent) {
            // 转换图像内容
            OpenAIImageContent openAIImageContent = new OpenAIImageContent();
            openAIImageContent.setType("image_url");

            AnthropicImageSource source = imageContent.getSource();
            if (source != null) {
                ImageUrl imageUrl = new ImageUrl();
                if ("base64".equals(source.getType())) {
                    imageUrl.setUrl("data:" + source.getMediaType() + ";base64," + source.getData());
                } else if ("url".equals(source.getType())) {
                    imageUrl.setUrl(source.getUrl());
                }
                openAIImageContent.setImageUrl(imageUrl);
                return openAIImageContent;
            }
        } else if (anthropicContent instanceof AnthropicToolResultContent) {
            // Tool result content is now handled separately in convertMessagesToUnified
            // and should not reach this method
            return null;
        } else if (anthropicContent instanceof AnthropicThinkingContent) {
            // Thinking content is specific to Anthropic and doesn't have a direct OpenAI
            // equivalent
            return null;
        } else if (anthropicContent instanceof AnthropicDocumentContent) {
            // Document content is specific to Anthropic and doesn't have a direct OpenAI
            // equivalent
            return null;
        }

        return null;
    }

    private static ToolCall convertAnthropicToolUseToToolCall(AnthropicToolUseContent toolUseContent) {
        ToolCall toolCall = new ToolCall();
        toolCall.setId(toolUseContent.getId());
        toolCall.setType("function");
        
        ToolCall.Function function = new ToolCall.Function();
        function.setName(toolUseContent.getName());
        
        // 将input map转换为JSON字符串
        if (toolUseContent.getInput() != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                function.setArguments(objectMapper.writeValueAsString(toolUseContent.getInput()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize tool input to JSON: {}", e.getMessage());
                return null;
            }
        }
        
        toolCall.setFunction(function);
        return toolCall;
    }

    private static OpenAIMessage convertToolResultToToolMessage(AnthropicToolResultContent toolResultContent) {
        OpenAIMessage toolMessage = new OpenAIMessage();
        toolMessage.setRole("tool");
        toolMessage.setToolCallId(toolResultContent.getToolUseId());
        
        // 转换工具结果内容
        List<OpenAIContent> toolResultContents = new ArrayList<>();
        for (AnthropicContent content : toolResultContent.getContent()) {
            OpenAIContent openAIContent = convertAnthropicContentToOpenAI(content);
            if (openAIContent != null) {
                toolResultContents.add(openAIContent);
            }
        }
        toolMessage.setContent(toolResultContents);
        
        return toolMessage;
    }
}

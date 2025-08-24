package org.elmo.robella.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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

    public static void convertSystemToUnified(AnthropicChatRequest req, UnifiedChatRequest unifiedRequest) {
        // 将Anthropic的system字段转换为OpenAI格式的系统消息
        if (req.getSystem() != null && !req.getSystem().trim().isEmpty()) {
            OpenAIMessage systemMessage = new OpenAIMessage();
            systemMessage.setRole("system");
            
            OpenAITextContent textContent = new OpenAITextContent();
            textContent.setType("text");
            textContent.setText(req.getSystem());
            
            List<OpenAIContent> contents = new ArrayList<>();
            contents.add(textContent);
            systemMessage.setContent(contents);
            
            // 将系统消息插入到消息列表的开头
            List<OpenAIMessage> messages = unifiedRequest.getMessages();
            if (messages == null) {
                messages = new ArrayList<>();
            }
            messages.add(0, systemMessage);
            unifiedRequest.setMessages(messages);
        }
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
                } else if (anthropicContent instanceof AnthropicThinkingContent thinkingContent) {
                    // Anthropic思考内容转换为OpenAI的reasoningContent字段
                    // 仅对assistant角色的消息设置reasoningContent
                    if ("assistant".equals(role)) {
                        openAIMessage.setReasoningContent(thinkingContent.getThinking());
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
        } else if (anthropicContent instanceof AnthropicThinkingContent thinkingContent) {
            // Convert Anthropic thinking content to OpenAI reasoning content
            // This should be handled at the message level, not content level
            // Return null here and handle in convertMessagesToUnified
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

    // Unified to Anthropic conversion methods
    public static void convertBaseToAnthropic(UnifiedChatRequest unifiedRequest, AnthropicChatRequest anthropicRequest) {
        anthropicRequest.setModel(unifiedRequest.getModel());
        anthropicRequest.setStream(unifiedRequest.getStream());
        anthropicRequest.setMaxTokens(unifiedRequest.getMaxTokens());
        anthropicRequest.setTemperature(unifiedRequest.getTemperature());
        anthropicRequest.setTopP(unifiedRequest.getTopP());
        anthropicRequest.setTopK(unifiedRequest.getTopK());
        anthropicRequest.setStopSequences(unifiedRequest.getStop());
    }

    public static void convertToolsToAnthropic(UnifiedChatRequest unifiedRequest, AnthropicChatRequest anthropicRequest) {
        if (unifiedRequest.getTools() != null && !unifiedRequest.getTools().isEmpty()) {
            List<AnthropicTool> anthropicTools = new ArrayList<>();
            for (Tool tool : unifiedRequest.getTools()) {
                if (tool.getType().equals("function") && tool.getFunction() != null) {
                    AnthropicCustomTool anthropicTool = new AnthropicCustomTool();
                    anthropicTool.setName(tool.getFunction().getName());
                    anthropicTool.setDescription(tool.getFunction().getDescription());
                    anthropicTool.setInputSchema(tool.getFunction().getParameters());
                    anthropicTools.add(anthropicTool);
                }
            }
            anthropicRequest.setTools(anthropicTools);
        }
    }

    public static void convertToolChoiceToAnthropic(UnifiedChatRequest unifiedRequest, AnthropicChatRequest anthropicRequest) {
        ToolChoice toolChoice = unifiedRequest.getToolChoice();
        if (toolChoice != null) {
            AnthropicToolChoice anthropicToolChoice = new AnthropicToolChoice();
            
            if (toolChoice == ToolChoice.AUTO) {
                anthropicToolChoice.setType("auto");
            } else if (toolChoice == ToolChoice.REQUIRED) {
                anthropicToolChoice.setType("any");
            } else if (toolChoice == ToolChoice.NONE) {
                anthropicToolChoice.setType("none");
            } else if (toolChoice.getFunction() != null && toolChoice.getFunction().getName() != null) {
                anthropicToolChoice.setType("tool");
                anthropicToolChoice.setName(toolChoice.getFunction().getName());
            } else {
                // 默认值
                anthropicToolChoice.setType("auto");
            }
            
            anthropicRequest.setToolChoice(anthropicToolChoice);
        }
    }

    public static void convertThinkingToAnthropic(UnifiedChatRequest unifiedRequest, AnthropicChatRequest anthropicRequest) {
        ThinkingOptions thinkingOptions = unifiedRequest.getThinkingOptions();
        if (thinkingOptions != null) {
            AnthropicThinking thinking = new AnthropicThinking();
            thinking.setType(thinkingOptions.getType());
            thinking.setBudgetTokens(thinkingOptions.getThinkingBudget());
            anthropicRequest.setThinking(thinking);
        }
    }

    public static void convertSystemToAnthropic(UnifiedChatRequest unifiedRequest, AnthropicChatRequest anthropicRequest) {
        // 从OpenAI格式的系统消息中提取系统prompt
        if (unifiedRequest.getMessages() != null && !unifiedRequest.getMessages().isEmpty()) {
            OpenAIMessage firstMessage = unifiedRequest.getMessages().get(0);
            if ("system".equals(firstMessage.getRole()) && firstMessage.getContent() != null) {
                StringBuilder systemPrompt = new StringBuilder();
                for (OpenAIContent content : firstMessage.getContent()) {
                    if (content instanceof OpenAITextContent textContent) {
                        systemPrompt.append(textContent.getText()).append("\n");
                    }
                }
                if (systemPrompt.length() > 0) {
                    anthropicRequest.setSystem(systemPrompt.toString().trim());
                }
            }
        }
    }

    public static void convertMessagesToAnthropic(UnifiedChatRequest unifiedRequest, AnthropicChatRequest anthropicRequest) {
        List<OpenAIMessage> openAIMessages = unifiedRequest.getMessages();
        List<AnthropicMessage> anthropicMessages = new ArrayList<>();
        
        // 跳过系统消息（将在convertSystemToAnthropic中处理）
        int startIndex = 0;
        if (openAIMessages != null && !openAIMessages.isEmpty() && 
            "system".equals(openAIMessages.get(0).getRole())) {
            startIndex = 1;
        }
        
        for (int i = startIndex; i < openAIMessages.size(); i++) {
            OpenAIMessage openAIMessage = openAIMessages.get(i);
            
            // 转换角色
            String role = openAIMessage.getRole();
            
            if ("tool".equals(role)) {
                // 工具消息需要转换为Anthropic的ToolResultContent
                AnthropicMessage toolResultMessage = convertToolMessageToAnthropic(openAIMessage);
                if (toolResultMessage != null) {
                    anthropicMessages.add(toolResultMessage);
                }
                continue;
            }
            
            AnthropicMessage anthropicMessage = new AnthropicMessage();
            if ("user".equals(role) || "assistant".equals(role)) {
                anthropicMessage.setRole(role);
            } else {
                // 其他角色转换为user
                anthropicMessage.setRole("user");
            }
            
            // 转换内容
            List<AnthropicContent> anthropicContents = new ArrayList<>();

            // 处理思考内容
            if (openAIMessage.getReasoningContent() != null && "assistant".equals(role)) {
                AnthropicThinkingContent thinkingContent = new AnthropicThinkingContent();
                thinkingContent.setThinking(openAIMessage.getReasoningContent());
                anthropicContents.add(thinkingContent);
            }
            
            // 处理文本和图像内容
            if (openAIMessage.getContent() != null) {
                for (OpenAIContent openAIContent : openAIMessage.getContent()) {
                    AnthropicContent anthropicContent = convertOpenAIContentToAnthropic(openAIContent);
                    if (anthropicContent != null) {
                        anthropicContents.add(anthropicContent);
                    }
                }
            }
            
            // 处理工具调用
            if (openAIMessage.getToolCalls() != null) {
                for (ToolCall toolCall : openAIMessage.getToolCalls()) {
                    AnthropicToolUseContent toolUseContent = convertToolCallToAnthropicToolUse(toolCall);
                    if (toolUseContent != null) {
                        anthropicContents.add(toolUseContent);
                    }
                }
            }
            
            anthropicMessage.setContent(anthropicContents);
            anthropicMessages.add(anthropicMessage);
        }
        
        anthropicRequest.setMessages(anthropicMessages);
    }

    private static AnthropicContent convertOpenAIContentToAnthropic(OpenAIContent openAIContent) {
        if (openAIContent instanceof OpenAITextContent textContent) {
            AnthropicTextContent anthropicTextContent = new AnthropicTextContent();
            anthropicTextContent.setType("text");
            anthropicTextContent.setText(textContent.getText());
            return anthropicTextContent;
        } else if (openAIContent instanceof OpenAIImageContent imageContent) {
            if (imageContent.getImageUrl() != null) {
                AnthropicImageContent anthropicImageContent = new AnthropicImageContent();
                anthropicImageContent.setType("image");
                
                AnthropicImageSource source = new AnthropicImageSource();
                String url = imageContent.getImageUrl().getUrl();
                if (url.startsWith("data:")) {
                    // Base64 encoded image
                    String[] parts = url.split(",");
                    if (parts.length == 2) {
                        String[] mediaTypeParts = parts[0].split(";");
                        source.setType("base64");
                        source.setMediaType(mediaTypeParts[0].replace("data:", ""));
                        source.setData(parts[1]);
                    }
                } else {
                    // URL image
                    source.setType("url");
                    source.setUrl(url);
                }
                
                anthropicImageContent.setSource(source);
                return anthropicImageContent;
            }
        }
        return null;
    }

    private static AnthropicToolUseContent convertToolCallToAnthropicToolUse(ToolCall toolCall) {
        if (toolCall.getType().equals("function") && toolCall.getFunction() != null) {
            AnthropicToolUseContent toolUseContent = new AnthropicToolUseContent();
            toolUseContent.setType("tool_use");
            toolUseContent.setId(toolCall.getId());
            toolUseContent.setName(toolCall.getFunction().getName());
            
            // 解析JSON参数为Map
            if (toolCall.getFunction().getArguments() != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> input = objectMapper.readValue(
                        toolCall.getFunction().getArguments(), 
                        new TypeReference<Map<String, Object>>() {}
                    );
                    toolUseContent.setInput(input);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to parse tool arguments JSON: {}", e.getMessage());
                }
            }
            
            return toolUseContent;
        }
        return null;
    }

    private static AnthropicMessage convertToolMessageToAnthropic(OpenAIMessage toolMessage) {
        if (toolMessage.getToolCallId() == null) {
            return null;
        }
        
        AnthropicMessage anthropicMessage = new AnthropicMessage();
        anthropicMessage.setRole("user"); // Anthropic中工具结果作为user消息
        
        List<AnthropicContent> anthropicContents = new ArrayList<>();
        
        // 创建ToolResultContent
        AnthropicToolResultContent toolResultContent = new AnthropicToolResultContent();
        toolResultContent.setType("tool_result");
        toolResultContent.setToolUseId(toolMessage.getToolCallId());
        
        // 转换工具结果内容
        if (toolMessage.getContent() != null) {
            List<AnthropicContent> resultContents = new ArrayList<>();
            for (OpenAIContent openAIContent : toolMessage.getContent()) {
                AnthropicContent anthropicContent = convertOpenAIContentToAnthropic(openAIContent);
                if (anthropicContent != null) {
                    resultContents.add(anthropicContent);
                }
            }
            toolResultContent.setContent(resultContents);
        }
        
        anthropicContents.add(toolResultContent);
        anthropicMessage.setContent(anthropicContents);
        
        return anthropicMessage;
    }
}

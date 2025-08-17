package org.elmo.robella.util;

import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.anthropic.*;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.openai.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Anthropic 与 Unified 模型转换工具类
 */
@Slf4j
public class AnthropicTransformUtils {

    /**
     * 将 UnifiedChatRequest 的基础字段转换为 AnthropicChatRequest
     */
    public static AnthropicChatRequest convertUnifiedToBase(UnifiedChatRequest unifiedRequest) {
        AnthropicChatRequest request = new AnthropicChatRequest();
        request.setModel(unifiedRequest.getModel());
        request.setMaxTokens(unifiedRequest.getMaxTokens());
        request.setTemperature(unifiedRequest.getTemperature());
        request.setTopP(unifiedRequest.getTopP());
        request.setTopK(unifiedRequest.getTopK());
        request.setStream(unifiedRequest.getStream());

        // 处理 stop 序列
        if (unifiedRequest.getStop() != null && !unifiedRequest.getStop().isEmpty()) {
            request.setStopSequences(unifiedRequest.getStop());
        }

        // 处理系统消息
        if (unifiedRequest.getSystemMessage() != null) {
            request.setSystem(unifiedRequest.getSystemMessage());
        }

        return request;
    }

    /**
     * 将 AnthropicChatRequest 的基础字段转换为 UnifiedChatRequest.Builder
     */
    public static UnifiedChatRequest.UnifiedChatRequestBuilder convertBaseToUnified(AnthropicChatRequest anthropicRequest) {
        UnifiedChatRequest.UnifiedChatRequestBuilder builder = UnifiedChatRequest.builder()
                .model(anthropicRequest.getModel())
                .maxTokens(anthropicRequest.getMaxTokens())
                .temperature(anthropicRequest.getTemperature())
                .topP(anthropicRequest.getTopP())
                .topK(anthropicRequest.getTopK())
                .stream(anthropicRequest.getStream())
                .systemMessage(anthropicRequest.getSystem());

        // 处理停止序列
        if (anthropicRequest.getStopSequences() != null && !anthropicRequest.getStopSequences().isEmpty()) {
            builder.stop(anthropicRequest.getStopSequences());
        }

        return builder;
    }

    /**
     * 转换 Anthropic 消息到 OpenAI 格式
     */
    public static List<ChatMessage> convertAnthropicMessagesToOpenAI(List<AnthropicMessage> anthropicMessages) {
        if (anthropicMessages == null || anthropicMessages.isEmpty()) {
            return new ArrayList<>();
        }

        return anthropicMessages.stream()
                .map(AnthropicTransformUtils::convertAnthropicMessageToOpenAI)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个 Anthropic 消息到 OpenAI 格式
     */
    public static ChatMessage convertAnthropicMessageToOpenAI(AnthropicMessage anthropicMessage) {
        if (anthropicMessage == null) {
            return null;
        }

        ChatMessage.ChatMessageBuilder builder = ChatMessage.builder()
                .role(anthropicMessage.getRole());

        // 处理内容 - Anthropic 的 content 是 List<AnthropicContent>，需要转换为 List<ContentPart>
        if (anthropicMessage.getContent() != null && !anthropicMessage.getContent().isEmpty()) {
            List<ContentPart> openAIContent = convertAnthropicContentListToOpenAI(anthropicMessage.getContent());
            builder.content(openAIContent);
        }

        return builder.build();
    }

    /**
     * 转换 OpenAI 消息到 Anthropic 格式
     */
    public static List<AnthropicMessage> convertOpenAIMessagesToAnthropic(List<ChatMessage> openAIMessages) {
        if (openAIMessages == null || openAIMessages.isEmpty()) {
            return new ArrayList<>();
        }

        return openAIMessages.stream()
                .filter(msg -> !"system".equals(msg.getRole())) // 系统消息在 Anthropic 中单独处理
                .map(AnthropicTransformUtils::convertOpenAIMessageToAnthropic)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个 OpenAI 消息到 Anthropic 格式
     */
    public static AnthropicMessage convertOpenAIMessageToAnthropic(ChatMessage openAIMessage) {
        if (openAIMessage == null) {
            return null;
        }

        AnthropicMessage.AnthropicMessageBuilder builder = AnthropicMessage.builder()
                .role(openAIMessage.getRole());

        // 处理内容 - OpenAI 的 content 是 List<ContentPart>，需要转换为 List<AnthropicContent>
        if (openAIMessage.getContent() != null && !openAIMessage.getContent().isEmpty()) {
            List<AnthropicContent> anthropicContent = convertOpenAIContentListToAnthropic(openAIMessage.getContent());
            builder.content(anthropicContent);
        }

        return builder.build();
    }

    /**
     * 转换 Anthropic 内容列表到 OpenAI 格式
     */
    private static List<ContentPart> convertAnthropicContentListToOpenAI(List<AnthropicContent> anthropicContentList) {
        if (anthropicContentList == null || anthropicContentList.isEmpty()) {
            return new ArrayList<>();
        }

        return anthropicContentList.stream()
                .map(AnthropicTransformUtils::convertAnthropicContentToOpenAI)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个 Anthropic 内容到 OpenAI 格式
     */
    private static ContentPart convertAnthropicContentToOpenAI(AnthropicContent anthropicContent) {
        if (anthropicContent == null) {
            return null;
        }

        if (anthropicContent instanceof AnthropicTextContent textContent) {
            return ContentPart.builder()
                    .type("text")
                    .text(textContent.getText())
                    .build();
        }

        if (anthropicContent instanceof AnthropicImageContent imageContent) {
            if (imageContent.getSource() != null) {
                // Anthropic 图像格式转换为 OpenAI 格式
                String url = imageContent.getSource().getData() != null
                        ? "data:" + imageContent.getSource().getMediaType() + ";base64," + imageContent.getSource().getData()
                        : null;
                if (url != null) {
                    return ContentPart.builder()
                            .type("image_url")
                            .imageUrl(ImageUrl.builder().url(url).build())
                            .build();
                }
            }
        }

        if (anthropicContent instanceof AnthropicToolUseContent toolUseContent) {
            // 工具使用内容暂时不转换，因为 OpenAI 中工具调用在不同位置处理
            log.debug("Tool use content skipped in content conversion: {}", toolUseContent.getName());
            return null;
        }

        if (anthropicContent instanceof AnthropicToolResultContent) {
            // 工具结果内容暂时不转换，因为 OpenAI 中工具结果在不同位置处理
            log.debug("Tool result content skipped in content conversion");
            return null;
        }

        if (anthropicContent instanceof AnthropicDocumentContent) {
            // 文档内容目前 OpenAI 不直接支持，记录警告
            log.warn("Document content is not supported in OpenAI format, skipping");
            return null;
        }

        log.warn("Unsupported Anthropic content type: {}", anthropicContent.getClass().getSimpleName());
        return null;
    }

    /**
     * 转换 OpenAI 内容列表到 Anthropic 格式
     */
    private static List<AnthropicContent> convertOpenAIContentListToAnthropic(List<ContentPart> openAIContentList) {
        if (openAIContentList == null || openAIContentList.isEmpty()) {
            return new ArrayList<>();
        }

        return openAIContentList.stream()
                .map(AnthropicTransformUtils::convertOpenAIContentToAnthropic)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个 OpenAI 内容到 Anthropic 格式
     */
    private static AnthropicContent convertOpenAIContentToAnthropic(ContentPart openAIContent) {
        if (openAIContent == null) {
            return null;
        }

        if ("text".equals(openAIContent.getType()) && openAIContent.getText() != null) {
            AnthropicTextContent anthropicTextContent = new AnthropicTextContent();
            anthropicTextContent.setType("text");
            anthropicTextContent.setText(openAIContent.getText());
            return anthropicTextContent;
        }

        if ("image_url".equals(openAIContent.getType()) && openAIContent.getImageUrl() != null) {
            AnthropicImageContent anthropicImageContent = new AnthropicImageContent();
            anthropicImageContent.setType("image");
            
            String url = openAIContent.getImageUrl().getUrl();
            if (url != null) {
                AnthropicImageSource source = new AnthropicImageSource();
                
                // 处理 base64 格式的图像
                if (url.startsWith("data:")) {
                    String[] parts = url.split(",", 2);
                    if (parts.length == 2) {
                        String mimeType = parts[0].substring(5).split(";")[0]; // 去掉 "data:" 前缀
                        source.setType("base64");
                        source.setMediaType(mimeType);
                        source.setData(parts[1]);
                    }
                } else {
                    // URL 格式的图像暂不支持，Anthropic 主要使用 base64
                    log.warn("URL-based images are not supported in Anthropic format, skipping: {}", url);
                    return null;
                }
                
                anthropicImageContent.setSource(source);
                return anthropicImageContent;
            }
        }

        if ("input_audio".equals(openAIContent.getType())) {
            // 音频内容目前 Anthropic 不支持
            log.warn("Audio content is not supported in Anthropic format, skipping");
            return null;
        }

        log.warn("Unsupported OpenAI content type: {}", openAIContent.getType());
        return null;
    }

    /**
     * 转换 OpenAI 工具到 Anthropic 格式
     */
    public static List<AnthropicTool> convertOpenAIToolsToAnthropic(List<Tool> openAITools) {
        if (openAITools == null || openAITools.isEmpty()) {
            return new ArrayList<>();
        }

        return openAITools.stream()
                .map(AnthropicTransformUtils::convertOpenAIToolToAnthropic)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个 OpenAI 工具到 Anthropic 格式
     */
    private static AnthropicTool convertOpenAIToolToAnthropic(Tool openAITool) {
        if (openAITool == null || openAITool.getFunction() == null) {
            return null;
        }

        AnthropicCustomTool anthropicTool = new AnthropicCustomTool();
        anthropicTool.setName(openAITool.getFunction().getName());
        anthropicTool.setDescription(openAITool.getFunction().getDescription());
        
        // 处理参数 schema
        if (openAITool.getFunction().getParameters() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) openAITool.getFunction().getParameters();
            anthropicTool.setInputSchema(parameters);
        }

        return anthropicTool;
    }

    /**
     * 转换 Anthropic 工具到 OpenAI 格式
     */
    public static List<Tool> convertAnthropicToolsToOpenAI(List<AnthropicTool> anthropicTools) {
        if (anthropicTools == null || anthropicTools.isEmpty()) {
            return new ArrayList<>();
        }

        return anthropicTools.stream()
                .map(AnthropicTransformUtils::convertAnthropicToolToOpenAI)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个 Anthropic 工具到 OpenAI 格式
     */
    private static Tool convertAnthropicToolToOpenAI(AnthropicTool anthropicTool) {
        if (anthropicTool instanceof AnthropicCustomTool customTool) {
            Function function = new Function();
            function.setName(customTool.getName());
            function.setDescription(customTool.getDescription());
            function.setParameters(customTool.getInputSchema());

            Tool tool = new Tool();
            tool.setType("function");
            tool.setFunction(function);
            return tool;
        }

        if (anthropicTool instanceof AnthropicComputerTool) {
            // Anthropic Computer Tool 在 OpenAI 中没有直接对应，暂不转换
            log.warn("Computer tool is not supported in OpenAI format, skipping");
            return null;
        }

        if (anthropicTool instanceof AnthropicBashTool) {
            // Anthropic Bash Tool 在 OpenAI 中没有直接对应，暂不转换
            log.warn("Bash tool is not supported in OpenAI format, skipping");
            return null;
        }

        if (anthropicTool instanceof AnthropicTextEditorTool) {
            // Anthropic Text Editor Tool 在 OpenAI 中没有直接对应，暂不转换
            log.warn("Text editor tool is not supported in OpenAI format, skipping");
            return null;
        }

        log.warn("Unsupported Anthropic tool type: {}", anthropicTool.getClass().getSimpleName());
        return null;
    }

    /**
     * 转换 OpenAI 工具选择到 Anthropic 格式
     */
    public static AnthropicToolChoice convertOpenAIToolChoiceToAnthropic(Object openAIToolChoice) {
        if (openAIToolChoice == null) {
            return null;
        }

        if (openAIToolChoice instanceof String choice) {
            return switch (choice) {
                case "none" -> null; // Anthropic 没有明确的 "none"，使用 null
                case "auto" -> new AnthropicAutoToolChoice();
                case "any" -> new AnthropicAnyToolChoice();
                default -> new AnthropicAutoToolChoice(); // 默认为 auto
            };
        }

        // 处理特定工具选择 - OpenAI 格式: {"type": "function", "function": {"name": "tool_name"}}
        if (openAIToolChoice instanceof Map<?, ?> choiceMap) {
            if ("function".equals(choiceMap.get("type")) && choiceMap.get("function") instanceof Map<?, ?> functionMap) {
                Object toolName = functionMap.get("name");
                if (toolName instanceof String name) {
                    AnthropicSpecificToolChoice specificChoice = new AnthropicSpecificToolChoice();
                    specificChoice.setType("tool");
                    specificChoice.setName(name);
                    return specificChoice;
                }
            }
        }

        return new AnthropicAutoToolChoice();
    }

    /**
     * 转换 Anthropic 工具选择到 OpenAI 格式
     */
    public static Object convertAnthropicToolChoiceToOpenAI(AnthropicToolChoice anthropicToolChoice) {
        if (anthropicToolChoice == null) {
            return "none";
        }

        if (anthropicToolChoice instanceof AnthropicAutoToolChoice) {
            return "auto";
        } else if (anthropicToolChoice instanceof AnthropicAnyToolChoice) {
            return "any";
        } else if (anthropicToolChoice instanceof AnthropicSpecificToolChoice specificChoice) {
            Map<String, Object> choice = new HashMap<>();
            choice.put("type", "function");
            choice.put("function", Map.of("name", specificChoice.getName()));
            return choice;
        }

        return "auto"; // 默认
    }

    /**
     * 转换 Anthropic 使用统计到 OpenAI 格式
     */
    public static Usage convertAnthropicUsageToOpenAI(AnthropicUsage anthropicUsage) {
        if (anthropicUsage == null) {
            return null;
        }

        Usage usage = new Usage();
        usage.setPromptTokens(anthropicUsage.getInputTokens());
        usage.setCompletionTokens(anthropicUsage.getOutputTokens());
        usage.setTotalTokens((anthropicUsage.getInputTokens() != null ? anthropicUsage.getInputTokens() : 0) +
                (anthropicUsage.getOutputTokens() != null ? anthropicUsage.getOutputTokens() : 0));

        return usage;
    }

    /**
     * 转换 OpenAI 使用统计到 Anthropic 格式
     */
    public static AnthropicUsage convertOpenAIUsageToAnthropic(Usage openAIUsage) {
        if (openAIUsage == null) {
            return null;
        }

        AnthropicUsage usage = new AnthropicUsage();
        usage.setInputTokens(openAIUsage.getPromptTokens());
        usage.setOutputTokens(openAIUsage.getCompletionTokens());

        return usage;
    }

    /**
     * 提取系统消息
     */
    public static String extractSystemMessage(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        return messages.stream()
                .filter(msg -> "system".equals(msg.getRole()))
                .findFirst()
                .map(msg -> {
                    if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                        // 获取第一个文本内容
                        return msg.getContent().stream()
                                .filter(part -> "text".equals(part.getType()))
                                .map(ContentPart::getText)
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElse(null);
                    }
                    return null;
                })
                .orElse(null);
    }

    /**
     * 过滤掉系统消息
     */
    public static List<ChatMessage> filterOutSystemMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        return messages.stream()
                .filter(msg -> !"system".equals(msg.getRole()))
                .collect(Collectors.toList());
    }
}

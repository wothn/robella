package org.elmo.robella.util;

import org.elmo.robella.model.anthropic.content.*;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.anthropic.core.AnthropicUsage;
import org.elmo.robella.model.anthropic.tool.AnthropicCustomTool;
import org.elmo.robella.model.anthropic.tool.AnthropicTool;
import org.elmo.robella.model.anthropic.tool.AnthropicToolChoice;
import org.elmo.robella.model.openai.content.*;
import org.elmo.robella.model.openai.core.OpenAIMessage;
import org.elmo.robella.model.openai.core.Usage;
import org.elmo.robella.model.openai.tool.Tool;
import org.elmo.robella.model.openai.tool.ToolCall;
import org.elmo.robella.model.openai.tool.ToolChoice;
import org.elmo.robella.model.openai.tool.Function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 转换Anthropic的工具类
 */
public class AnthropicTransformUtils {

    /**
     * 将OpenAI格式的消息转换为Anthropic格式的消息
     */
    public static AnthropicMessage openAiToAnthropicMessage(OpenAIMessage openAiMessage) {
        if (openAiMessage == null) {
            return null;
        }

        // tool 角色的特殊处理
        if ("tool".equals(openAiMessage.getRole())) {
            return buildToolResultMessageFromOpenAI(openAiMessage);
        }

        AnthropicMessage anthropicMessage = new AnthropicMessage();
        anthropicMessage.setRole(openAiMessage.getRole());

        // 内容转换
        if (openAiMessage.getContent() != null && !openAiMessage.getContent().isEmpty()) {
            List<AnthropicContent> anthropicContents = convertOpenAIContentsToAnthropic(openAiMessage.getContent());
            if (!anthropicContents.isEmpty()) {
                anthropicMessage.setContent(anthropicContents);
            }
        }

        // tool_calls → tool_use
        if (openAiMessage.getToolCalls() != null && !openAiMessage.getToolCalls().isEmpty()) {
            List<AnthropicContent> anthropicContents = anthropicMessage.getContent() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(anthropicMessage.getContent());
            for (ToolCall toolCall : openAiMessage.getToolCalls()) {
                AnthropicToolUseContent toolUse = convertToolCallToAnthropic(toolCall);
                if (toolUse != null) {
                    anthropicContents.add(toolUse);
                }
            }
            if (!anthropicContents.isEmpty()) {
                anthropicMessage.setContent(anthropicContents);
            }
        }

        return anthropicMessage;
    }

    private static AnthropicMessage buildToolResultMessageFromOpenAI(OpenAIMessage openAiMessage) {
        AnthropicMessage anthropicMessage = new AnthropicMessage();
        anthropicMessage.setRole("user"); // 工具结果在Anthropic侧通常视为user消息

        AnthropicToolResultContent toolResultContent = new AnthropicToolResultContent();
        toolResultContent.setType("tool_result");
        toolResultContent.setToolUseId(openAiMessage.getToolCallId());

        List<AnthropicContent> resultContents = new ArrayList<>();
        if (openAiMessage.getContent() != null && !openAiMessage.getContent().isEmpty()) {
            for (OpenAIContent openAiContent : openAiMessage.getContent()) {
                if (openAiContent instanceof OpenAITextContent text) {
                    resultContents.add(createAnthropicTextContent(text.getText()));
                }
                // 如有需要，后续可扩展其它类型（例如图像）
            }
        }
        toolResultContent.setContent(resultContents);
        anthropicMessage.setContent(List.of(toolResultContent));
        return anthropicMessage;
    }

    private static List<AnthropicContent> convertOpenAIContentsToAnthropic(List<OpenAIContent> openAiContents) {
        List<AnthropicContent> anthropicContents = new ArrayList<>();
        for (OpenAIContent openAiContent : openAiContents) {
            if (openAiContent instanceof OpenAITextContent text) {
                anthropicContents.add(createAnthropicTextContent(text.getText()));
            } else if (openAiContent instanceof OpenAIImageContent image) {
                AnthropicImageContent imgContent = buildAnthropicImageContent(image);
                anthropicContents.add(imgContent);
            }
        }
        return anthropicContents;
    }

    private static AnthropicTextContent createAnthropicTextContent(String text) {
        AnthropicTextContent textContent = new AnthropicTextContent();
        textContent.setType("text");
        textContent.setText(text);
        return textContent;
    }

    private static AnthropicImageContent buildAnthropicImageContent(OpenAIImageContent openAiImageContent) {
        AnthropicImageContent imageContent = new AnthropicImageContent();
        imageContent.setType("image");
        AnthropicImageSource imageSource = buildAnthropicImageSourceFromImageUrl(openAiImageContent.getImageUrl());
        imageContent.setSource(imageSource);
        return imageContent;
    }

    private static AnthropicImageSource buildAnthropicImageSourceFromImageUrl(ImageUrl imageUrl) {
        AnthropicImageSource imageSource = new AnthropicImageSource();
        if (imageUrl != null) {
            if (imageUrl.getUrl() != null && imageUrl.getUrl().startsWith("data:")) {
                // Base64编码的图片 dataURL
                String[] parts = imageUrl.getUrl().split(",");
                if (parts.length == 2) {
                    imageSource.setType("base64");
                    String mediaType = parts[0].substring(5); // 移除 "data:"
                    if (mediaType.contains(";")) {
                        mediaType = mediaType.substring(0, mediaType.indexOf(";"));
                    }
                    imageSource.setMediaType(mediaType);
                    imageSource.setData(parts[1]);
                }
            } else if (imageUrl.getUrl() != null) {
                imageSource.setType("url");
                imageSource.setUrl(imageUrl.getUrl());
            }
        }
        return imageSource;
    }

    private static AnthropicToolUseContent convertToolCallToAnthropic(ToolCall toolCall) {
        if (!"function".equals(toolCall.getType()) || toolCall.getFunction() == null) {
            return null;
        }
        AnthropicToolUseContent toolUseContent = new AnthropicToolUseContent();
        toolUseContent.setType("tool_use");
        toolUseContent.setId(toolCall.getId());
        toolUseContent.setName(toolCall.getFunction().getName());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> input = JsonUtils.fromJson(toolCall.getFunction().getArguments(), Map.class);
            toolUseContent.setInput(input);
        } catch (Exception e) {
            Map<String, Object> input = new HashMap<>();
            input.put("raw_arguments", toolCall.getFunction().getArguments());
            toolUseContent.setInput(input);
        }
        return toolUseContent;
    }

    /**
     * 将Anthropic格式的消息转换为OpenAI格式的消息
     */
    public static OpenAIMessage anthropicToOpenAiMessage(AnthropicMessage anthropicMessage) {
        if (anthropicMessage == null) {
            return null;
        }

        // 特殊处理：如果消息包含工具结果，需要转换为tool角色的消息
        if (anthropicMessage.getContent() != null && !anthropicMessage.getContent().isEmpty()) {
            for (AnthropicContent content : anthropicMessage.getContent()) {
                if (content instanceof AnthropicToolResultContent toolResult) {
                    // 这是一个工具结果消息，需要转换为OpenAI的tool消息
                    OpenAIMessage toolMessage = new OpenAIMessage();
                    toolMessage.setRole("tool");
                    toolMessage.setToolCallId(toolResult.getToolUseId());
                    List<OpenAIContent> resultContents = collectToolResultContentsToOpenAI(toolResult);
                    toolMessage.setContent(resultContents);
                    return toolMessage;
                }
            }
        }

        OpenAIMessage openAiMessage = new OpenAIMessage();
        openAiMessage.setRole(anthropicMessage.getRole());

        if (anthropicMessage.getContent() != null && !anthropicMessage.getContent().isEmpty()) {
            List<OpenAIContent> openAiContents =
                    fillOpenAIMessageFromAnthropicContents(anthropicMessage.getContent(), openAiMessage);
            openAiMessage.setContent(openAiContents);
        }

        return openAiMessage;
    }

    private static List<OpenAIContent> fillOpenAIMessageFromAnthropicContents(List<AnthropicContent> anthropicContents,
                                                                              OpenAIMessage target) {
        List<OpenAIContent> openAiContents = new ArrayList<>();
        for (AnthropicContent anthropicContent : anthropicContents) {
            if (anthropicContent instanceof AnthropicTextContent text) {
                openAiContents.add(toOpenAITextContent(text));
            } else if (anthropicContent instanceof AnthropicImageContent image) {
                openAiContents.add(toOpenAIImageContent(image));
            } else if (anthropicContent instanceof AnthropicToolUseContent toolUse) {
                ToolCall toolCall = toOpenAIToolCall(toolUse);
                if (toolCall != null) {
                    if (target.getToolCalls() == null) {
                        target.setToolCalls(new ArrayList<>());
                    }
                    target.getToolCalls().add(toolCall);
                }
            }
            // 移除AnthropicToolResultContent的处理，因为已经在上层方法中处理
        }
        return openAiContents;
    }

    private static OpenAITextContent toOpenAITextContent(AnthropicTextContent text) {
        OpenAITextContent content = new OpenAITextContent();
        content.setType("text");
        content.setText(text.getText());
        return content;
    }

    private static OpenAIImageContent toOpenAIImageContent(AnthropicImageContent anthropicImageContent) {
        OpenAIImageContent imageContent = new OpenAIImageContent();
        imageContent.setType("image_url");
        imageContent.setImageUrl(buildImageUrlFromAnthropicSource(anthropicImageContent.getSource()));
        return imageContent;
    }

    private static ImageUrl buildImageUrlFromAnthropicSource(AnthropicImageSource imageSource) {
        ImageUrl imageUrl = new ImageUrl();
        if (imageSource != null) {
            if ("base64".equals(imageSource.getType())) {
                imageUrl.setUrl("data:" + imageSource.getMediaType() + ";base64," + imageSource.getData());
            } else if ("url".equals(imageSource.getType())) {
                imageUrl.setUrl(imageSource.getUrl());
            }
        }
        return imageUrl;
    }

    private static ToolCall toOpenAIToolCall(AnthropicToolUseContent toolUseContent) {
        ToolCall toolCall = new ToolCall();
        toolCall.setId(toolUseContent.getId());
        toolCall.setType("function");

        ToolCall.Function function = new ToolCall.Function();
        function.setName(toolUseContent.getName());
        String arguments = JsonUtils.toJson(toolUseContent.getInput());
        function.setArguments(arguments);

        toolCall.setFunction(function);
        return toolCall;
    }

    private static List<OpenAIContent> collectToolResultContentsToOpenAI(AnthropicToolResultContent toolResultContent) {
        List<OpenAIContent> resultContents = new ArrayList<>();
        if (toolResultContent.getContent() != null && !toolResultContent.getContent().isEmpty()) {
            for (AnthropicContent resultContent : toolResultContent.getContent()) {
                if (resultContent instanceof AnthropicTextContent text) {
                    OpenAITextContent textContent = new OpenAITextContent();
                    textContent.setType("text");
                    textContent.setText(text.getText());
                    resultContents.add(textContent);
                } else if (resultContent instanceof AnthropicImageContent) {
                    // 如需，后续可扩展对图像结果的处理
                }
            }
        }
        return resultContents;
    }

    /**
     * 将Anthropic使用量统计转换为OpenAI使用量统计
     */
    public static Usage anthropicToOpenAiUsage(AnthropicUsage anthropicUsage) {
        if (anthropicUsage == null) {
            return null;
        }

        Usage usage = new Usage();
        usage.setPromptTokens(anthropicUsage.getInputTokens());
        usage.setCompletionTokens(anthropicUsage.getOutputTokens());
        if (anthropicUsage.getInputTokens() != null && anthropicUsage.getOutputTokens() != null) {
            usage.setTotalTokens(anthropicUsage.getInputTokens() + anthropicUsage.getOutputTokens());
        }

        return usage;
    }

    /**
     * 将OpenAI工具转换为Anthropic工具
     */
    public static List<AnthropicTool> openAiToAnthropicTools(List<Tool> openAiTools) {
        if (openAiTools == null || openAiTools.isEmpty()) {
            return null;
        }

        return openAiTools.stream()
                .map(tool -> {
                    AnthropicCustomTool anthropicTool = new AnthropicCustomTool();
                    anthropicTool.setType("custom");
                    anthropicTool.setName(tool.getFunction().getName());
                    anthropicTool.setDescription(tool.getFunction().getDescription());
                    anthropicTool.setInputSchema(tool.getFunction().getParameters());
                    return anthropicTool;
                })
                .collect(Collectors.toList());
    }

    /**
     * 将OpenAI工具选择策略转换为Anthropic工具选择策略
     */
    public static AnthropicToolChoice openAiToAnthropicToolChoice(ToolChoice toolChoice) {
        if (toolChoice == null) {
            return null;
        }

        AnthropicToolChoice anthropicToolChoice = new AnthropicToolChoice();
        if ("none".equals(toolChoice.getType())) {
            anthropicToolChoice.setType("none");
        } else if ("auto".equals(toolChoice.getType())) {
            anthropicToolChoice.setType("auto");
        } else if ("required".equals(toolChoice.getType())) {
            anthropicToolChoice.setType("any");
        } else if (toolChoice.getFunction() != null) {
            anthropicToolChoice.setType("tool");
            anthropicToolChoice.setName(toolChoice.getFunction().getName());
        }

        return anthropicToolChoice;
    }

    /**
     * 将Anthropic工具转换为OpenAI工具
     */
    public static List<Tool> anthropicToOpenAiTools(List<AnthropicTool> anthropicTools) {
        if (anthropicTools == null || anthropicTools.isEmpty()) {
            return null;
        }

        return anthropicTools.stream()
                .map(anthropicTool -> {
                    Tool openAiTool = new Tool();
                    openAiTool.setType("function");

                    if (anthropicTool instanceof AnthropicCustomTool customTool) {
                        Function function = new Function();
                        function.setName(customTool.getName());
                        function.setDescription(customTool.getDescription());
                        function.setParameters(customTool.getInputSchema());
                        openAiTool.setFunction(function);
                    }

                    return openAiTool;
                })
                .collect(Collectors.toList());
    }

    /**
     * 将Anthropic工具选择策略转换为OpenAI工具选择策略
     */
    public static ToolChoice anthropicToOpenAiToolChoice(AnthropicToolChoice anthropicToolChoice) {
        if (anthropicToolChoice == null) {
            return null;
        }

        ToolChoice toolChoice = new ToolChoice();
        if ("none".equals(anthropicToolChoice.getType())) {
            toolChoice.setType("none");
        } else if ("auto".equals(anthropicToolChoice.getType())) {
            toolChoice.setType("auto");
        } else if ("any".equals(anthropicToolChoice.getType())) {
            toolChoice.setType("required");
        } else if ("tool".equals(anthropicToolChoice.getType())) {
            toolChoice.setType("function");
            Function function = new Function();
            function.setName(anthropicToolChoice.getName());
            toolChoice.setFunction(function);
        }

        return toolChoice;
    }
}

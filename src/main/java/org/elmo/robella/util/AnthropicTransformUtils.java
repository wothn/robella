package org.elmo.robella.util;

import org.elmo.robella.model.anthropic.content.*;
import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
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

        AnthropicMessage anthropicMessage = new AnthropicMessage();
        
        // 处理 tool 角色的特殊转换
        if ("tool".equals(openAiMessage.getRole())) {
            anthropicMessage.setRole("user"); // Anthropic 中工具结果通常用 user 角色表示
            
            // 创建 tool_result 内容
            AnthropicToolResultContent toolResultContent = new AnthropicToolResultContent();
            toolResultContent.setType("tool_result");
            toolResultContent.setToolUseId(openAiMessage.getToolCallId());
            
            // 将 OpenAI content 转换为 Anthropic content
            if (openAiMessage.getContent() != null && !openAiMessage.getContent().isEmpty()) {
                List<AnthropicContent> resultContents = new ArrayList<>();
                for (OpenAIContent openAiContent : openAiMessage.getContent()) {
                    if (openAiContent instanceof OpenAITextContent) {
                        AnthropicTextContent textContent = new AnthropicTextContent();
                        textContent.setType("text");
                        textContent.setText(((OpenAITextContent) openAiContent).getText());
                        resultContents.add(textContent);
                    }
                    // 可以添加对其他内容类型的支持
                }
                toolResultContent.setContent(resultContents);
            }
            
            anthropicMessage.setContent(List.of(toolResultContent));
            return anthropicMessage;
        }
        
        anthropicMessage.setRole(openAiMessage.getRole());
        
        // 处理内容转换
        if (openAiMessage.getContent() != null && !openAiMessage.getContent().isEmpty()) {
            List<AnthropicContent> anthropicContents = new ArrayList<>();
            for (OpenAIContent openAiContent : openAiMessage.getContent()) {
                if (openAiContent instanceof OpenAITextContent) {
                    AnthropicTextContent textContent = new AnthropicTextContent();
                    textContent.setType("text");
                    textContent.setText(((OpenAITextContent) openAiContent).getText());
                    anthropicContents.add(textContent);
                } else if (openAiContent instanceof OpenAIImageContent) {
                    OpenAIImageContent openAiImageContent = (OpenAIImageContent) openAiContent;
                    AnthropicImageContent imageContent = new AnthropicImageContent();
                    imageContent.setType("image");
                    
                    AnthropicImageSource imageSource = new AnthropicImageSource();
                    ImageUrl imageUrl = openAiImageContent.getImageUrl();
                    if (imageUrl != null) {
                        if (imageUrl.getUrl() != null && imageUrl.getUrl().startsWith("data:")) {
                            // Base64编码的图片
                            String[] parts = imageUrl.getUrl().split(",");
                            if (parts.length == 2) {
                                imageSource.setType("base64");
                                // 从data URL中提取媒体类型
                                String mediaType = parts[0].substring(5); // 移除 "data:" 前缀
                                if (mediaType.contains(";")) {
                                    mediaType = mediaType.substring(0, mediaType.indexOf(";"));
                                }
                                imageSource.setMediaType(mediaType);
                                imageSource.setData(parts[1]);
                            }
                        } else if (imageUrl.getUrl() != null) {
                            // URL图片
                            imageSource.setType("url");
                            imageSource.setUrl(imageUrl.getUrl());
                        }
                    }
                    imageContent.setSource(imageSource);
                    anthropicContents.add(imageContent);
                }
            }
            anthropicMessage.setContent(anthropicContents);
        }
        
        // 处理 OpenAI 的 tool_calls，转换为 Anthropic 的 tool_use 内容
        if (openAiMessage.getToolCalls() != null && !openAiMessage.getToolCalls().isEmpty()) {
            if (anthropicMessage.getContent() == null) {
                anthropicMessage.setContent(new ArrayList<>());
            }
            
            for (ToolCall toolCall : openAiMessage.getToolCalls()) {
                if ("function".equals(toolCall.getType()) && toolCall.getFunction() != null) {
                    AnthropicToolUseContent toolUseContent = new AnthropicToolUseContent();
                    toolUseContent.setType("tool_use");
                    toolUseContent.setId(toolCall.getId());
                    toolUseContent.setName(toolCall.getFunction().getName());
                    
                    // 将 JSON 字符串参数转换为 Map
                    try {
                        Map<String, Object> input = JsonUtils.fromJson(toolCall.getFunction().getArguments(), Map.class);
                        toolUseContent.setInput(input);
                    } catch (Exception e) {
                        // 如果解析失败，创建一个包含原始字符串的简单 Map
                        Map<String, Object> input = new HashMap<>();
                        input.put("raw_arguments", toolCall.getFunction().getArguments());
                        toolUseContent.setInput(input);
                    }
                    
                    anthropicMessage.getContent().add(toolUseContent);
                }
            }
        }

        return anthropicMessage;
    }

    /**
     * 将Anthropic格式的消息转换为OpenAI格式的消息
     */
    public static OpenAIMessage anthropicToOpenAiMessage(AnthropicMessage anthropicMessage) {
        if (anthropicMessage == null) {
            return null;
        }

        OpenAIMessage openAiMessage = new OpenAIMessage();
        openAiMessage.setRole(anthropicMessage.getRole());
        
        // 处理内容转换
        if (anthropicMessage.getContent() != null && !anthropicMessage.getContent().isEmpty()) {
            List<OpenAIContent> openAiContents = new ArrayList<>();
            for (AnthropicContent anthropicContent : anthropicMessage.getContent()) {
                if (anthropicContent instanceof AnthropicTextContent) {
                    // 文本内容转换
                    OpenAITextContent textContent = new OpenAITextContent();
                    textContent.setType("text");
                    textContent.setText(((AnthropicTextContent) anthropicContent).getText());
                    openAiContents.add(textContent);
                } else if (anthropicContent instanceof AnthropicImageContent anthropicImageContent) {
                    // 图片内容转换
                    OpenAIImageContent imageContent = new OpenAIImageContent();
                    imageContent.setType("image_url");
                    
                    ImageUrl imageUrl = new ImageUrl();
                    AnthropicImageSource imageSource = anthropicImageContent.getSource();
                    if (imageSource != null) {
                        if ("base64".equals(imageSource.getType())) {
                            // Base64编码的图片
                            imageUrl.setUrl("data:" + imageSource.getMediaType() + ";base64," + imageSource.getData());
                        } else if ("url".equals(imageSource.getType())) {
                            // URL图片
                            imageUrl.setUrl(imageSource.getUrl());
                        }
                    }
                    imageContent.setImageUrl(imageUrl);
                    openAiContents.add(imageContent);
                } else if (anthropicContent instanceof AnthropicToolUseContent toolUseContent) {
                    // 工具调用内容转换为 OpenAI 的 tool_calls
                    // 创建 ToolCall 对象
                    ToolCall toolCall = new ToolCall();
                    toolCall.setId(toolUseContent.getId());
                    toolCall.setType("function");
                    
                    // 创建 Function 对象
                    ToolCall.Function function = new ToolCall.Function();
                    function.setName(toolUseContent.getName());
                    // 将 Map 参数转换为 JSON 字符串
                    String arguments = JsonUtils.toJson(toolUseContent.getInput());
                    function.setArguments(arguments);
                    
                    toolCall.setFunction(function);
                    
                    // 如果消息还没有 tool_calls 列表，则创建一个
                    if (openAiMessage.getToolCalls() == null) {
                        openAiMessage.setToolCalls(new ArrayList<>());
                    }
                    openAiMessage.getToolCalls().add(toolCall);
                } else if (anthropicContent instanceof AnthropicToolResultContent toolResultContent) {
                    // 工具结果内容转换为 OpenAI 的 tool 消息
                    // 设置 role 为 "tool"
                    openAiMessage.setRole("tool");
                    // 设置 tool_call_id
                    openAiMessage.setToolCallId(toolResultContent.getToolUseId());
                    
                    // 将工具结果内容转换为 OpenAI content
                    if (toolResultContent.getContent() != null && !toolResultContent.getContent().isEmpty()) {
                        List<OpenAIContent> resultContents = new ArrayList<>();
                        for (AnthropicContent resultContent : toolResultContent.getContent()) {
                            if (resultContent instanceof AnthropicTextContent) {
                                OpenAITextContent textContent = new OpenAITextContent();
                                textContent.setType("text");
                                textContent.setText(((AnthropicTextContent) resultContent).getText());
                                resultContents.add(textContent);
                            } else if (resultContent instanceof AnthropicImageContent) {
                                // 如果需要，也可以处理图像结果
                                // 这里暂时只处理文本结果
                            }
                        }
                        openAiMessage.setContent(resultContents);
                    }
                }
            }
            openAiMessage.setContent(openAiContents);
        }

        return openAiMessage;
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
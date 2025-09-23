package org.elmo.robella.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.model.openai.content.ImageUrl;
import org.elmo.robella.model.openai.content.OpenAIAudioContent;
import org.elmo.robella.model.openai.content.OpenAIContent;
import org.elmo.robella.model.openai.content.OpenAIImageContent;
import org.elmo.robella.model.openai.content.OpenAITextContent;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.elmo.robella.model.openai.core.Choice;
import org.elmo.robella.model.openai.core.OpenAIMessage;
import org.elmo.robella.model.openai.stream.ChatCompletionChunk;
import org.elmo.robella.model.openai.stream.Delta;
import org.elmo.robella.model.openai.tool.Tool;
import org.elmo.robella.model.openai.tool.ToolCall;
import org.elmo.robella.model.anthropic.content.AnthropicTextContent;
import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.anthropic.stream.AnthropicStreamEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicContentBlockDeltaEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicMessageStartEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicDelta;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TokenCountingService {

    private static final EncodingRegistry encodingRegistry = Encodings.newLazyEncodingRegistry();

    /**
     * 计算给定内容的令牌数
     *
     * @param content   要计算的内容
     * @param modelName 模型名称（用于选择编码）
     * @return 令牌数
     */
    public int calculateTokens(String content, String modelName) {
        if (content == null || content.isEmpty()) {
            return 0;
        }

        Encoding encoding = getEncodingForModel(modelName);
        return encoding.countTokens(content);
    }

    /**
     * 计算聊天消息的提示令牌数
     *
     * @param messages  消息列表
     * @param modelName 模型名称（用于选择编码）
     * @return 提示令牌数
     */
    public int countOpenAIMessageToken(List<OpenAIMessage> messages, String modelName) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }

        int tokenPerMessage; // 每条消息的固定令牌数
        if (modelName == "gpt-3.5-turbo-0301") {
            tokenPerMessage = 4; // <|start|>{role}\n{content}<|end|>\n
        } else {
            tokenPerMessage = 3; // 新版本优化
        }

        int totalTokens = 0;

        for (OpenAIMessage message : messages) {
            totalTokens += tokenPerMessage;
            totalTokens += calculateTokens(message.getRole(), modelName);
            List<OpenAIContent> contents = message.getContent();
            for (OpenAIContent content : contents) {
                if (content instanceof OpenAITextContent) {
                    String text = ((OpenAITextContent) content).getText();
                    totalTokens += calculateTokens(text, modelName);
                } else if (content instanceof OpenAIImageContent) {
                    ImageUrl imageUrl = ((OpenAIImageContent) content).getImageUrl();
                    totalTokens += calculateImageTokens(imageUrl, modelName);
                } else if (content instanceof OpenAIAudioContent) {
                    totalTokens += 100; // todo: 根据实际情况完善音频令牌计算逻辑
                }
            }
        }
        totalTokens += 3; // 助手回复的起始token
        return totalTokens;
    }

    public int countOpenAIRequestToken(ChatCompletionRequest request, String modelName) {
        int totalTokens = 0;
        totalTokens += countOpenAIMessageToken(request.getMessages(), modelName);
        // 工具调用
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            List<Tool> tools = request.getTools();
            StringBuilder toolsStr = new StringBuilder();
            for (Tool tool : tools) {
                toolsStr.append(tool.getFunction().getName());
                if (tool.getFunction().getDescription() != null && !tool.getFunction().getDescription().isEmpty()) {
                    toolsStr.append(tool.getFunction().getDescription());
                }
                if (tool.getFunction().getParameters() != null) {
                    toolsStr.append(tool.getFunction().getParameters().toString());
                }
            }
            totalTokens += calculateTokens(toolsStr.toString(), modelName);
            totalTokens += 8; // 固定的工具格式开销
        }

        return totalTokens;
    }

    /**
     * 计算OpenAI流式响应块的令牌数
     * @param chunk  流式响应块
     * @param modelName 模型名称（用于选择编码）
     * @return
     */
    public int countOpenAIStreamChunkToken(ChatCompletionChunk chunk, String modelName) {
        int totalTokens = 0;

        if (chunk == null || chunk.getChoices() == null || chunk.getChoices().isEmpty()) {
            return totalTokens;
        }

        for (Choice choice : chunk.getChoices()) {
            if (choice.getDelta() == null) {
                continue;
            }

            Delta delta = choice.getDelta();

            // Count role tokens
            if (delta.getRole() != null && !delta.getRole().isEmpty()) {
                totalTokens += calculateTokens(delta.getRole(), modelName);
            }

            // Count content tokens
            if (delta.getContent() != null && !delta.getContent().isEmpty()) {
                for (OpenAIContent content : delta.getContent()) {
                    if (content instanceof OpenAITextContent) {
                        String text = ((OpenAITextContent) content).getText();
                        if (text != null && !text.isEmpty()) {
                            totalTokens += calculateTokens(text, modelName);
                        }
                    }
                }
            }

            // Count reasoning content tokens
            if (delta.getReasoningContent() != null && !delta.getReasoningContent().isEmpty()) {
                totalTokens += calculateTokens(delta.getReasoningContent(), modelName);
            }

            // Count tool call tokens
            if (delta.getToolCalls() != null && !delta.getToolCalls().isEmpty()) {
                for (ToolCall toolCall : delta.getToolCalls()) {
                    if (toolCall.getFunction() != null) {
                        if (toolCall.getFunction().getName() != null) {
                            totalTokens += calculateTokens(toolCall.getFunction().getName(), modelName);
                        }
                        if (toolCall.getFunction().getArguments() != null) {
                            totalTokens += calculateTokens(toolCall.getFunction().getArguments(), modelName);
                        }
                    }
                }
            }
        }

        return totalTokens;
    }

    /**
     * 计算OpenAI非流式响应的令牌数
     *
     * @param response  ChatCompletion响应
     * @param modelName 模型名称（用于选择编码）
     * @return 响应令牌数
     */
    public int countOpenAIResponseToken(ChatCompletionResponse response, String modelName) {
        int totalTokens = 0;

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return totalTokens;
        }

        for (Choice choice : response.getChoices()) {
            if (choice.getMessage() == null) {
                continue;
            }

            OpenAIMessage message = choice.getMessage();

            // Count role tokens
            if (message.getRole() != null && !message.getRole().isEmpty()) {
                totalTokens += calculateTokens(message.getRole(), modelName);
            }

            // Count content tokens
            if (message.getContent() != null && !message.getContent().isEmpty()) {
                for (OpenAIContent content : message.getContent()) {
                    if (content instanceof OpenAITextContent) {
                        String text = ((OpenAITextContent) content).getText();
                        if (text != null && !text.isEmpty()) {
                            totalTokens += calculateTokens(text, modelName);
                        }
                    } else if (content instanceof OpenAIImageContent) {
                        ImageUrl imageUrl = ((OpenAIImageContent) content).getImageUrl();
                        totalTokens += calculateImageTokens(imageUrl, modelName);
                    } else if (content instanceof OpenAIAudioContent) {
                        totalTokens += 100; // todo: 根据实际情况完善音频令牌计算逻辑
                    }
                }
            }

            // Count reasoning content tokens
            if (message.getReasoningContent() != null && !message.getReasoningContent().isEmpty()) {
                totalTokens += calculateTokens(message.getReasoningContent(), modelName);
            }

            // Count tool call tokens
            if (message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
                for (ToolCall toolCall : message.getToolCalls()) {
                    if (toolCall.getFunction() != null) {
                        if (toolCall.getFunction().getName() != null) {
                            totalTokens += calculateTokens(toolCall.getFunction().getName(), modelName);
                        }
                        if (toolCall.getFunction().getArguments() != null) {
                            totalTokens += calculateTokens(toolCall.getFunction().getArguments(), modelName);
                        }
                    }
                }
            }
        }

        return totalTokens;
    }

    private Encoding getEncodingForModel(String modelName) {
        ModelType modelType = mapModelNameToModelType(modelName);
        return encodingRegistry.getEncodingForModel(modelType);
    }

    private ModelType mapModelNameToModelType(String modelName) {
        if (modelName == null) {
            return ModelType.GPT_4;
        }

        String lowerModelName = modelName.toLowerCase();

        if (lowerModelName.contains("gpt-3.5") || lowerModelName.contains("gpt35")) {
            return ModelType.GPT_3_5_TURBO;
        }

        return ModelType.GPT_4;
    }

    private int calculateImageTokens(ImageUrl imageUrl, String modelName) {
        // todo: 根据实际情况完善图片令牌计算逻辑
        return 1000; // 临时固定值，待完善
    }

    /**
     * 计算Anthropic请求的令牌数
     *
     * @param request   Anthropic聊天请求
     * @param modelName 模型名称（用于选择编码）
     * @return 请求令牌数
     */
    public int countAnthropicRequestToken(AnthropicChatRequest request, String modelName) {
        if (request == null || request.getMessages() == null || request.getMessages().isEmpty()) {
            return 0;
        }

        int totalTokens = 0;

        // 计算系统提示词的令牌数
        if (request.getSystem() != null && !request.getSystem().isEmpty()) {
            for (AnthropicTextContent systemContent : request.getSystem()) {
                totalTokens += calculateTokens(systemContent.getText(), modelName);
            }
        }

        // 计算消息的令牌数
        for (AnthropicMessage message : request.getMessages()) {
            totalTokens += calculateTokens(message.getRole(), modelName);
            if (message.getContent() != null) {
                totalTokens += calculateTokens(message.getContent().toString(), modelName);
            }
        }

        return totalTokens;
    }

    /**
     * 计算Anthropic响应的令牌数
     *
     * @param response  Anthropic聊天响应
     * @param modelName 模型名称（用于选择编码）
     * @return 响应令牌数
     */
    public int countAnthropicResponseToken(AnthropicMessage response, String modelName) {
        if (response == null) {
            return 0;
        }

        int totalTokens = 0;

        // 计算角色的令牌数
        if (response.getRole() != null && !response.getRole().isEmpty()) {
            totalTokens += calculateTokens(response.getRole(), modelName);
        }

        // 计算内容的令牌数
        if (response.getContent() != null) {
            totalTokens += calculateTokens(response.getContent().toString(), modelName);
        }

        return totalTokens;
    }

    /**
     * 计算Anthropic流式事件的令牌数
     *
     * @param event     Anthropic流式事件
     * @param modelName 模型名称（用于选择编码）
     * @return 事件令牌数
     */
    public int countAnthropicStreamEventToken(AnthropicStreamEvent event, String modelName) {
        if (event == null) {
            return 0;
        }

        int totalTokens = 0;

        switch (event.getType()) {
            case "message_start":
                totalTokens += countMessageStartEvent((AnthropicMessageStartEvent) event, modelName);
                break;
            case "content_block_delta":
                totalTokens += countContentBlockDeltaEvent((AnthropicContentBlockDeltaEvent) event, modelName);
                break;
            case "content_block_start":
            case "content_block_stop":
            case "message_delta":
            case "message_stop":
            case "ping":
            case "error":
                // 这些事件类型不包含需要计数的文本内容
                break;
            default:
                log.warn("Unknown Anthropic stream event type: {}", event.getType());
                break;
        }

        return totalTokens;
    }

    /**
     * 计算消息开始事件的令牌数
     */
    private int countMessageStartEvent(AnthropicMessageStartEvent event, String modelName) {
        if (event == null || event.getMessage() == null) {
            return 0;
        }

        int totalTokens = 0;
        AnthropicMessage message = event.getMessage();

        // 计算角色的令牌数
        if (message.getRole() != null && !message.getRole().isEmpty()) {
            totalTokens += calculateTokens(message.getRole(), modelName);
        }

        // 计算内容的令牌数
        if (message.getContent() != null) {
            totalTokens += calculateTokens(message.getContent().toString(), modelName);
        }

        return totalTokens;
    }

    /**
     * 计算内容块增量事件的令牌数
     */
    private int countContentBlockDeltaEvent(AnthropicContentBlockDeltaEvent event, String modelName) {
        if (event == null || event.getDelta() == null) {
            return 0;
        }

        int totalTokens = 0;
        AnthropicDelta delta = event.getDelta();

        // 根据增量类型计算令牌数
        if (delta.isTextDelta() && delta.getText() != null && !delta.getText().isEmpty()) {
            totalTokens += calculateTokens(delta.getText(), modelName);
        } else if (delta.isThinkingDelta() && delta.getThinking() != null && !delta.getThinking().isEmpty()) {
            totalTokens += calculateTokens(delta.getThinking(), modelName);
        } else if (delta.isInputJsonDelta() && delta.getPartialJson() != null && !delta.getPartialJson().isEmpty()) {
            totalTokens += calculateTokens(delta.getPartialJson(), modelName);
        }

        return totalTokens;
    }

}
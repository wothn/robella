package org.elmo.robella.util;

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
import org.elmo.robella.model.anthropic.content.AnthropicContent;
import org.elmo.robella.model.anthropic.content.AnthropicImageContent;
import org.elmo.robella.model.anthropic.content.AnthropicToolUseContent;
import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.anthropic.stream.AnthropicStreamEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicContentBlockDeltaEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicMessageStartEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicDelta;
import org.elmo.robella.model.anthropic.stream.AnthropicMessageDeltaEvent;
import org.elmo.robella.model.anthropic.tool.AnthropicTool;
import org.elmo.robella.model.anthropic.tool.AnthropicCustomTool;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.springframework.stereotype.Service;

import java.util.List;
@Slf4j
@Service
public class TokenCountingUtils {

    private static final EncodingRegistry encodingRegistry = Encodings.newLazyEncodingRegistry();

    // Constants for model names
    private static final String GPT_35_TURBO_0301 = "gpt-3.5-turbo-0301";

    // Constants for token counts
    private static final int TOKENS_PER_MESSAGE_OLD = 4;
    private static final int TOKENS_PER_MESSAGE_NEW = 3;
    private static final int AUDIO_CONTENT_TOKENS = 100;
    private static final int IMAGE_CONTENT_TOKENS = 1000;
    private static final int TOOL_FORMAT_OVERHEAD = 8;
    private static final int ANTHROPIC_TOKENS_PER_MESSAGE = 3; // Anthropic每条消息的固定令牌数

    /**
     * 计算图片内容的令牌数
     * @param imageUrl 图片URL对象
     * @param modelName 模型名称
     * @return 令牌数
     */
    private int calculateImageTokens(ImageUrl imageUrl, String modelName) {
        // todo: 根据实际情况完善图片令牌计算逻辑
        return IMAGE_CONTENT_TOKENS; // 临时固定值，待完善
    }

    /**
     * 根据模型名称获取对应的编码
     * @param modelName 模型名称
     * @return 编码对象
     */
    private Encoding getEncodingForModel(String modelName) {
        ModelType modelType = mapModelNameToModelType(modelName);
        return encodingRegistry.getEncodingForModel(modelType);
    }

    /**
     * 将模型名称映射到模型类型
     * @param modelName 模型名称
     * @return 模型类型
     */
    private ModelType mapModelNameToModelType(String modelName) {
        if (modelName == null) {
            return ModelType.GPT_4;
        }

        String lowerModelName = modelName.toLowerCase();

        // Anthropic模型处理
        if (lowerModelName.contains("claude") || lowerModelName.contains("anthropic")) {
            // Anthropic Claude 3.5 Sonnet等较新的模型使用GPT-4编码
            if (lowerModelName.contains("claude-3-5") || lowerModelName.contains("claude-3.5")) {
                return ModelType.GPT_4;
            }
            // Claude 3 Opus/Haiku等也使用GPT-4编码
            else if (lowerModelName.contains("claude-3")) {
                return ModelType.GPT_4;
            }
            // Claude 2.x系列使用GPT-3.5编码
            else if (lowerModelName.contains("claude-2")) {
                return ModelType.GPT_3_5_TURBO;
            }
            // 默认使用GPT-4编码
            return ModelType.GPT_4;
        }

        if (lowerModelName.contains("gpt-3.5") || lowerModelName.contains("gpt35")) {
            return ModelType.GPT_3_5_TURBO;
        }

        return ModelType.GPT_4;
    }

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
    public int countOpenAIMessageTokens(List<OpenAIMessage> messages, String modelName) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }

        int tokenPerMessage; // 每条消息的固定令牌数
        if (modelName.equals(GPT_35_TURBO_0301)) {
            tokenPerMessage = TOKENS_PER_MESSAGE_OLD; // <|start|>{role}\n{content}<|end|>\n
        } else {
            tokenPerMessage = TOKENS_PER_MESSAGE_NEW; // 新版本优化
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
                    totalTokens += AUDIO_CONTENT_TOKENS; // todo: 根据实际情况完善音频令牌计算逻辑
                }
            }
        }
        totalTokens += TOKENS_PER_MESSAGE_NEW; // 助手回复的起始token
        return totalTokens;
    }

    /**
     * 计算Anthropic聊天消息的提示令牌数
     *
     * @param messages  消息列表
     * @param modelName 模型名称（用于选择编码）
     * @return 提示令牌数
     */
    public int countAnthropicMessageTokens(List<AnthropicMessage> messages, String modelName) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }

        int totalTokens = 0;

        for (AnthropicMessage message : messages) {
            totalTokens += ANTHROPIC_TOKENS_PER_MESSAGE;
            totalTokens += calculateTokens(message.getRole(), modelName);
            List<AnthropicContent> contents = message.getContent();
            if (contents != null) {
                for (AnthropicContent content : contents) {
                    if (content instanceof AnthropicTextContent) {
                        String text = ((AnthropicTextContent) content).getText();
                        totalTokens += calculateTokens(text, modelName);
                    } else if (content instanceof AnthropicImageContent) {
                        totalTokens += IMAGE_CONTENT_TOKENS; // todo: 根据实际情况完善图片令牌计算逻辑
                    } else if (content instanceof AnthropicToolUseContent) {
                        // 处理工具调用内容
                        AnthropicToolUseContent toolUse = (AnthropicToolUseContent) content;
                        if (toolUse.getName() != null) {
                            totalTokens += calculateTokens(toolUse.getName(), modelName);
                        }
                        if (toolUse.getInput() != null) {
                            totalTokens += calculateTokens(toolUse.getInput().toString(), modelName);
                        }
                    }
                }
            }
        }
        totalTokens += ANTHROPIC_TOKENS_PER_MESSAGE; // 助手回复的起始token
        return totalTokens;
    }

    /**
     * 计算OpenAI请求的令牌数
     *
     * @param request   ChatCompletion请求
     * @param modelName 模型名称（用于选择编码）
     * @return 请求令牌数
     */
    public int countRequestTokens(ChatCompletionRequest request) {
        int totalTokens = 0;
        totalTokens += countOpenAIMessageTokens(request.getMessages(), request.getModel());
        // 工具调用
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            List<Tool> tools = request.getTools();
            // 预估StringBuilder容量以减少扩容
            StringBuilder toolsStr = new StringBuilder(1024);
            for (Tool tool : tools) {
                // 添加空指针检查
                if (tool.getFunction() != null) {
                    toolsStr.append(tool.getFunction().getName());
                    if (tool.getFunction().getDescription() != null && !tool.getFunction().getDescription().isEmpty()) {
                        toolsStr.append(tool.getFunction().getDescription());
                    }
                    if (tool.getFunction().getParameters() != null) {
                        toolsStr.append(tool.getFunction().getParameters().toString());
                    }
                }
            }
            totalTokens += calculateTokens(toolsStr.toString(), request.getModel());
            totalTokens += TOOL_FORMAT_OVERHEAD; // 固定的工具格式开销
        }

        return totalTokens;
    }

    /**
     * 计算Anthropic请求的令牌数
     *
     * @param request   AnthropicChatRequest请求
     * @param modelName 模型名称（用于选择编码）
     * @return 请求令牌数
     */
    public int countRequestTokens(AnthropicChatRequest request) {
        int totalTokens = 0;
        totalTokens += countAnthropicMessageTokens(request.getMessages(), request.getModel());

        // 系统消息
        if (request.getSystem() != null && !request.getSystem().isEmpty()) {
            for (AnthropicTextContent systemContent : request.getSystem()) {
                if (systemContent.getText() != null) {
                    totalTokens += calculateTokens(systemContent.getText(), request.getModel());
                }
            }
            totalTokens += ANTHROPIC_TOKENS_PER_MESSAGE; // 系统消息的固定开销
        }
        
        // 工具调用
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            List<AnthropicTool> tools = request.getTools();
            StringBuilder toolsStr = new StringBuilder(1024);
            for (AnthropicTool tool : tools) {
                if (tool.getName() != null) {
                    toolsStr.append(tool.getName());
                }
                // 处理自定义工具的描述和输入schema
                if (tool instanceof AnthropicCustomTool) {
                    AnthropicCustomTool customTool = (AnthropicCustomTool) tool;
                    if (customTool.getDescription() != null && !customTool.getDescription().isEmpty()) {
                        toolsStr.append(customTool.getDescription());
                    }
                    if (customTool.getInputSchema() != null) {
                        toolsStr.append(customTool.getInputSchema().toString());
                    }
                }
            }
            totalTokens += calculateTokens(toolsStr.toString(), request.getModel());
            totalTokens += TOOL_FORMAT_OVERHEAD; // 固定的工具格式开销
        }

        return totalTokens;
    }

    /**
     * 计算流式响应块的令牌数
     * @param chunk  流式响应块
     * @param vendorModelKey 模型名称（用于选择编码）
     * @return 令牌数
     */
    public int countChunkTokens(ChatCompletionChunk chunk, String vendorModelKey) {
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
                totalTokens += calculateTokens(delta.getRole(), vendorModelKey);
            }

            // Count content tokens
            if (delta.getContent() != null && !delta.getContent().isEmpty()) {
                for (OpenAIContent content : delta.getContent()) {
                    if (content == null) {
                        continue;
                    }
                    if (content instanceof OpenAITextContent textContent) {
                        String text = textContent.getText();
                        if (text != null && !text.isEmpty()) {
                            totalTokens += calculateTokens(text, vendorModelKey);
                        }
                    } else if (content instanceof OpenAIImageContent) {
                        // 处理图像内容
                        totalTokens += IMAGE_CONTENT_TOKENS;
                    } else if (content instanceof OpenAIAudioContent) {
                        // 处理音频内容
                        totalTokens += AUDIO_CONTENT_TOKENS;
                    } else {
                        log.warn("Unknown OpenAIContent type: {}", content.getClass().getName());
                    }
                }
            }

            // Count reasoning content tokens
            if (delta.getReasoningContent() != null && !delta.getReasoningContent().isEmpty()) {
                totalTokens += calculateTokens(delta.getReasoningContent(), vendorModelKey);
            }

            // Count tool call tokens
            if (delta.getToolCalls() != null && !delta.getToolCalls().isEmpty()) {
                for (ToolCall toolCall : delta.getToolCalls()) {
                    if (toolCall == null) {
                        continue;
                    }
                    
                    ToolCall.Function function = toolCall.getFunction();
                    if (function != null) {
                        if (function.getName() != null) {
                            totalTokens += calculateTokens(function.getName(), vendorModelKey);
                        }
                        if (function.getArguments() != null) {
                            totalTokens += calculateTokens(function.getArguments(), vendorModelKey);
                        }
                    }
                }
            }
        }

        return totalTokens;
    }

    /**
     * 计算Anthropic流式响应事件的令牌数
     * @param event  Anthropic流式事件
     * @param vendorModelKey 模型名称（用于选择编码）
     * @return 令牌数
     */
    public int countChunkTokens(AnthropicStreamEvent event, String vendorModelKey) {
        int totalTokens = 0;

        if (event == null) {
            return totalTokens;
        }

        // 处理内容块增量事件
        if (event instanceof AnthropicContentBlockDeltaEvent) {
            AnthropicContentBlockDeltaEvent deltaEvent = (AnthropicContentBlockDeltaEvent) event;
            AnthropicDelta delta = deltaEvent.getDelta();
            if (delta != null) {
                // 文本增量
                if ("text_delta".equals(delta.getType()) && delta.getText() != null) {
                    totalTokens += calculateTokens(delta.getText(), vendorModelKey);
                }
                // 工具输入JSON增量
                else if ("input_json_delta".equals(delta.getType()) && delta.getPartialJson() != null) {
                    totalTokens += calculateTokens(delta.getPartialJson(), vendorModelKey);
                }
                // 思考增量
                else if ("thinking_delta".equals(delta.getType()) && delta.getThinking() != null) {
                    totalTokens += calculateTokens(delta.getThinking(), vendorModelKey);
                }
            }
        }
        // 处理消息增量事件（包含usage信息）
        else if (event instanceof AnthropicMessageDeltaEvent) {
            // MessageDeltaEvent通常包含usage信息，但不包含实际内容tokens
            // 这里暂时不计算，因为usage已经在其他地方处理
        }
        // 处理消息开始事件（包含完整的message对象）
        else if (event instanceof AnthropicMessageStartEvent) {
            AnthropicMessageStartEvent startEvent = (AnthropicMessageStartEvent) event;
            AnthropicMessage message = startEvent.getMessage();
            if (message != null) {
                totalTokens += countAnthropicMessageTokens(List.of(message), vendorModelKey);
            }
        }

        return totalTokens;
    }

    /**
     * 计算非流式响应的令牌数
     *
     * @param response  ChatCompletion响应
     * @return 响应令牌数
     */
    public int countResponseTokens(ChatCompletionResponse response) {
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
                totalTokens += calculateTokens(message.getRole(), response.getModel());
            }
            
            // Count content tokens
            if (message.getContent() != null && !message.getContent().isEmpty()) {
                for (OpenAIContent content : message.getContent()) {
                    if (content == null) {
                        continue;
                    }
                    if (content instanceof OpenAITextContent) {
                        String text = ((OpenAITextContent) content).getText();
                        if (text != null && !text.isEmpty()) {
                            totalTokens += calculateTokens(text, response.getModel());
                        }
                    } else if (content instanceof OpenAIImageContent) {
                        ImageUrl imageUrl = ((OpenAIImageContent) content).getImageUrl();
                        totalTokens += calculateImageTokens(imageUrl, response.getModel());
                    } else if (content instanceof OpenAIAudioContent) {
                        totalTokens += AUDIO_CONTENT_TOKENS; // todo: 根据实际情况完善音频令牌计算逻辑
                    } else {
                        log.warn("Unknown OpenAIContent type: {}", content.getClass().getName());
                    }
                }
            }

            // Count reasoning content tokens
            if (message.getReasoningContent() != null && !message.getReasoningContent().isEmpty()) {
                totalTokens += calculateTokens(message.getReasoningContent(), response.getModel());
            }

            // Count tool call tokens
            if (message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
                for (ToolCall toolCall : message.getToolCalls()) {
                    if (toolCall != null && toolCall.getFunction() != null) {
                        if (toolCall.getFunction().getName() != null) {
                            totalTokens += calculateTokens(toolCall.getFunction().getName(), response.getModel());
                        }
                        if (toolCall.getFunction().getArguments() != null) {
                            totalTokens += calculateTokens(toolCall.getFunction().getArguments(), response.getModel());
                        }
                    }
                }
            }
        }

        return totalTokens;
    }

    /**
     * 计算Anthropic非流式响应的令牌数
     *
     * @param response  AnthropicMessage响应
     * @param modelName 模型名称（用于选择编码）
     * @return 响应令牌数
     */
    public int countResponseTokens(AnthropicMessage response, String modelName) {
        int totalTokens = 0;

        if (response == null) {
            return totalTokens;
        }

        // Count role tokens
        if (response.getRole() != null && !response.getRole().isEmpty()) {
            totalTokens += calculateTokens(response.getRole(), modelName);
        }
        
        // Count content tokens
        if (response.getContent() != null && !response.getContent().isEmpty()) {
            for (AnthropicContent content : response.getContent()) {
                if (content == null) {
                    continue;
                }
                if (content instanceof AnthropicTextContent) {
                    String text = ((AnthropicTextContent) content).getText();
                    if (text != null && !text.isEmpty()) {
                        totalTokens += calculateTokens(text, modelName);
                    }
                } else if (content instanceof AnthropicImageContent) {
                    totalTokens += IMAGE_CONTENT_TOKENS; // todo: 根据实际情况完善图片令牌计算逻辑
                } else if (content instanceof AnthropicToolUseContent) {
                    AnthropicToolUseContent toolUse = (AnthropicToolUseContent) content;
                    if (toolUse.getName() != null) {
                        totalTokens += calculateTokens(toolUse.getName(), modelName);
                    }
                    if (toolUse.getInput() != null) {
                        totalTokens += calculateTokens(toolUse.getInput().toString(), modelName);
                    }
                }
            }
        }

        // Count usage tokens (if available in the response)
        if (response.getUsage() != null) {
            // Usage信息通常包含在响应中，但这里我们只计算实际内容的tokens
            // Usage的input_tokens和output_tokens应该与我们计算的结果一致
        }

        return totalTokens;
    }

    /**
     * 估算UnifiedChatRequest的token数量
     * 专业方法用于请求成本估算，支持消息、工具调用等内容的token计算
     *
     * @param request 统一聊天请求
     * @param modelName 模型名称（用于选择编码）
     * @return 估算的token数量
     */
    public int estimateRequestTokens(UnifiedChatRequest request, String modelName) {
        if (request == null) {
            return 0;
        }

        int totalTokens = 0;

        // 计算消息token数量
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            totalTokens += countOpenAIMessageTokens(request.getMessages(), modelName);
        }

        // 计算工具调用的token数量
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            // 工具调用通常需要额外的token，基于实际工具内容计算
            StringBuilder toolsStr = new StringBuilder(1024);
            for (Tool tool : request.getTools()) {
                if (tool.getFunction() != null) {
                    // 添加工具名称token
                    if (tool.getFunction().getName() != null) {
                        toolsStr.append(tool.getFunction().getName());
                    }
                    // 添加工具描述token
                    if (tool.getFunction().getDescription() != null && !tool.getFunction().getDescription().isEmpty()) {
                        toolsStr.append(tool.getFunction().getDescription());
                    }
                    // 添加工具参数schema token
                    if (tool.getFunction().getParameters() != null) {
                        toolsStr.append(tool.getFunction().getParameters().toString());
                    }
                }
            }
            totalTokens += calculateTokens(toolsStr.toString(), modelName);
            totalTokens += TOOL_FORMAT_OVERHEAD; // 固定的工具格式开销
        }

        return totalTokens;
    }
}
package org.elmo.robella.service.transform;

import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.config.ProviderType;
import org.elmo.robella.model.anthropic.*;
import org.elmo.robella.model.internal.*;
import org.elmo.robella.model.openai.*;
import org.elmo.robella.service.VendorTransform;
import org.elmo.robella.util.AnthropicTransformUtils;

import java.util.*;

/**
 * Anthropic Messages API 转换实现
 */
@Slf4j
public class AnthropicTransform implements VendorTransform {
    
    @Override
    public String type() {
        return ProviderType.Anthropic.getName();
    }

    @Override
    public UnifiedChatRequest vendorRequestToUnified(Object vendorRequest) {
        if (!(vendorRequest instanceof AnthropicChatRequest req)) {
            return null;
        }

        // 转换基础字段
        UnifiedChatRequest.UnifiedChatRequestBuilder builder = AnthropicTransformUtils.convertBaseToUnified(req);

        // 转换消息列表
        if (req.getMessages() != null) {
            List<ChatMessage> unifiedMessages = AnthropicTransformUtils.convertAnthropicMessagesToOpenAI(req.getMessages());
            builder.messages(unifiedMessages);
        }

        // 转换工具列表
        if (req.getTools() != null && !req.getTools().isEmpty()) {
            List<Tool> unifiedTools = AnthropicTransformUtils.convertAnthropicToolsToOpenAI(req.getTools());
            builder.tools(unifiedTools);
        }

        // 转换工具选择
        if (req.getToolChoice() != null) {
            // 暂时简化处理，直接设置为字符串
            Object unifiedToolChoice = AnthropicTransformUtils.convertAnthropicToolChoiceToOpenAI(req.getToolChoice());
            if (unifiedToolChoice instanceof String) {
                ToolChoice toolChoice = new ToolChoice();
                // TODO: 需要根据 ToolChoice 的实际构造方法来设置
                builder.toolChoice(toolChoice);
            }
        }

        // 处理思考参数
        if (req.getThinking() != null) {
            UnifiedChatRequest.ThinkingOptions thinkingOptions = convertAnthropicThinkingToUnified(req.getThinking());
            if (thinkingOptions != null) {
                builder.thinkingOptions(thinkingOptions);
            }
        }

        // 处理元数据
        if (req.getMetadata() != null) {
            Map<String, Object> metadata = new HashMap<>();
            // 将 AnthropicMetadata 转换为通用的 Map
            metadata.put("anthropic.metadata", req.getMetadata());
            builder.metadata(metadata);
        }

        return builder.build();
    }

    @Override
    public Object unifiedToVendorRequest(UnifiedChatRequest unifiedRequest) {
        AnthropicChatRequest request = AnthropicTransformUtils.convertUnifiedToBase(unifiedRequest);

        // 处理系统消息
        if (unifiedRequest.getSystemMessage() != null) {
            request.setSystem(unifiedRequest.getSystemMessage());
        } else if (unifiedRequest.getMessages() != null) {
            // 从消息列表中提取系统消息
            String systemMessage = AnthropicTransformUtils.extractSystemMessage(unifiedRequest.getMessages());
            if (systemMessage != null) {
                request.setSystem(systemMessage);
            }
        }

        // 转换消息列表（过滤掉系统消息）
        if (unifiedRequest.getMessages() != null) {
            List<ChatMessage> filteredMessages = AnthropicTransformUtils.filterOutSystemMessages(unifiedRequest.getMessages());
            List<AnthropicMessage> anthropicMessages = AnthropicTransformUtils.convertOpenAIMessagesToAnthropic(filteredMessages);
            request.setMessages(anthropicMessages);
        }

        // 转换工具列表
        if (unifiedRequest.getTools() != null && !unifiedRequest.getTools().isEmpty()) {
            List<AnthropicTool> anthropicTools = AnthropicTransformUtils.convertOpenAIToolsToAnthropic(unifiedRequest.getTools());
            request.setTools(anthropicTools);
        }

        // 转换工具选择
        if (unifiedRequest.getToolChoice() != null) {
            AnthropicToolChoice anthropicToolChoice = AnthropicTransformUtils.convertOpenAIToolChoiceToAnthropic(unifiedRequest.getToolChoice());
            request.setToolChoice(anthropicToolChoice);
        }

        // 处理思考参数
        if (unifiedRequest.getThinkingOptions() != null) {
            AnthropicThinking thinking = convertUnifiedThinkingToAnthropic(unifiedRequest.getThinkingOptions());
            if (thinking != null) {
                request.setThinking(thinking);
            }
        }

        // 处理元数据
        if (unifiedRequest.getMetadata() != null) {
            Object anthropicMetadata = unifiedRequest.getMetadata().get("anthropic.metadata");
            if (anthropicMetadata instanceof AnthropicMetadata) {
                request.setMetadata((AnthropicMetadata) anthropicMetadata);
            }
        }

        return request;
    }

    @Override
    public UnifiedChatResponse vendorResponseToUnified(Object vendorResponse) {
        if (!(vendorResponse instanceof AnthropicChatResponse resp)) {
            return null;
        }

        UnifiedChatResponse.UnifiedChatResponseBuilder builder = UnifiedChatResponse.builder()
                .id(resp.getId())
                .model(resp.getModel())
                .object("chat.completion"); // Anthropic 返回 "message"，但统一为 OpenAI 格式

        // 转换 Usage
        if (resp.getUsage() != null) {
            Usage usage = AnthropicTransformUtils.convertAnthropicUsageToOpenAI(resp.getUsage());
            builder.usage(usage);
        }

        // 转换 Choice
        if (resp.getContent() != null) {
            List<Choice> choices = convertAnthropicContentToChoices(resp);
            builder.choices(choices);
        }

        return builder.build();
    }

    @Override
    public Object unifiedToVendorResponse(UnifiedChatResponse unifiedResponse) {
        if (unifiedResponse == null) {
            return null;
        }

        AnthropicChatResponse response = new AnthropicChatResponse();
        response.setId(unifiedResponse.getId());
        response.setModel(unifiedResponse.getModel());
        response.setType("message");
        response.setRole("assistant");

        // 转换 Usage
        if (unifiedResponse.getUsage() != null) {
            AnthropicUsage usage = AnthropicTransformUtils.convertOpenAIUsageToAnthropic(unifiedResponse.getUsage());
            response.setUsage(usage);
        }

        // 转换 Choice 到 Content
        if (unifiedResponse.getChoices() != null && !unifiedResponse.getChoices().isEmpty()) {
            List<AnthropicContent> content = convertChoicesToAnthropicContent(unifiedResponse.getChoices());
            response.setContent(content);
            
            // 设置停止原因
            Choice firstChoice = unifiedResponse.getChoices().get(0);
            if (firstChoice.getFinishReason() != null) {
                response.setStopReason(firstChoice.getFinishReason());
            }
        }

        return response;
    }

    @Override
    public UnifiedStreamChunk vendorStreamEventToUnified(Object vendorEvent) {
        if (vendorEvent == null || !(vendorEvent instanceof AnthropicStreamEvent event)) {
            return null;
        }

        // 根据不同的事件类型进行转换
        if (event instanceof AnthropicMessageStartEvent messageStart) {
            return convertMessageStartToUnified(messageStart);
        } else if (event instanceof AnthropicContentBlockDeltaEvent deltaEvent) {
            return convertContentBlockDeltaToUnified(deltaEvent);
        } else if (event instanceof AnthropicMessageDeltaEvent messageDelta) {
            return convertMessageDeltaToUnified(messageDelta);
        } else if (event instanceof AnthropicMessageStopEvent) {
            return convertMessageStopToUnified();
        }

        // 对于其他类型的事件，暂时返回 null
        return null;
    }

    @Override
    public Object unifiedStreamChunkToVendor(UnifiedStreamChunk chunk) {
        if (chunk == null) {
            return null;
        }

        // TODO: 根据需要实现 UnifiedStreamChunk 到 AnthropicStreamEvent 的转换
        // 这个方向的转换相对复杂，因为需要根据 chunk 的内容决定生成哪种类型的 Anthropic 事件
        log.warn("unifiedStreamChunkToVendor not implemented for Anthropic");
        return null;
    }

    // === 私有辅助方法 ===

    /**
     * 转换 Anthropic Thinking 到 Unified ThinkingOptions
     */
    private UnifiedChatRequest.ThinkingOptions convertAnthropicThinkingToUnified(AnthropicThinking anthropicThinking) {
        if (anthropicThinking == null) {
            return null;
        }

        // TODO: 根据 AnthropicThinking 的具体结构进行转换
        // 这里需要根据实际的 AnthropicThinking 类结构来实现
        return null;
    }

    /**
     * 转换 Unified ThinkingOptions 到 Anthropic Thinking
     */
    private AnthropicThinking convertUnifiedThinkingToAnthropic(UnifiedChatRequest.ThinkingOptions thinkingOptions) {
        if (thinkingOptions == null) {
            return null;
        }

        // TODO: 根据 ThinkingOptions 的结构转换为 AnthropicThinking
        // 这里需要根据实际的 AnthropicThinking 类结构来实现
        return null;
    }

    /**
     * 将 Anthropic 响应内容转换为 OpenAI Choice 列表
     */
    private List<Choice> convertAnthropicContentToChoices(AnthropicChatResponse response) {
        List<Choice> choices = new ArrayList<>();
        
        Choice choice = new Choice();
        choice.setIndex(0);
        choice.setFinishReason(response.getStopReason());
        
        // 创建 assistant 消息
        ChatMessage message = ChatMessage.builder()
                .role("assistant")
                .build();
        
        // 转换内容
        if (response.getContent() != null && !response.getContent().isEmpty()) {
            List<ContentPart> contentParts = new ArrayList<>();
            for (AnthropicContent content : response.getContent()) {
                if (content instanceof AnthropicTextContent textContent) {
                    ContentPart part = ContentPart.builder()
                            .type("text")
                            .text(textContent.getText())
                            .build();
                    contentParts.add(part);
                }
                // TODO: 处理其他类型的内容
            }
            message.setContent(contentParts);
        }
        
        choice.setMessage(message);
        choices.add(choice);
        
        return choices;
    }

    /**
     * 将 OpenAI Choice 列表转换为 Anthropic 内容
     */
    private List<AnthropicContent> convertChoicesToAnthropicContent(List<Choice> choices) {
        List<AnthropicContent> contentList = new ArrayList<>();
        
        if (choices != null && !choices.isEmpty()) {
            Choice firstChoice = choices.get(0);
            if (firstChoice.getMessage() != null && firstChoice.getMessage().getContent() != null) {
                for (ContentPart part : firstChoice.getMessage().getContent()) {
                    if ("text".equals(part.getType()) && part.getText() != null) {
                        AnthropicTextContent textContent = new AnthropicTextContent();
                        textContent.setType("text");
                        textContent.setText(part.getText());
                        contentList.add(textContent);
                    }
                    // TODO: 处理其他类型的内容
                }
            }
        }
        
        return contentList;
    }

    /**
     * 转换 MessageStart 事件
     */
    private UnifiedStreamChunk convertMessageStartToUnified(AnthropicMessageStartEvent messageStart) {
        return UnifiedStreamChunk.builder()
                .object("chat.completion.chunk")
                .finished(false)
                .build();
    }

    /**
     * 转换 ContentBlockDelta 事件
     */
    private UnifiedStreamChunk convertContentBlockDeltaToUnified(AnthropicContentBlockDeltaEvent deltaEvent) {
        if (deltaEvent.getDelta() == null) {
            return null;
        }

        String content = null;
        if (deltaEvent.getDelta().getText() != null) {
            content = deltaEvent.getDelta().getText();
        }

        if (content != null) {
            // 创建一个 Choice 对象，其中 delta 包含文本增量
            Choice choice = new Choice();
            choice.setIndex(deltaEvent.getIndex() != null ? deltaEvent.getIndex() : 0);
            
            // 创建 delta 对象
            Delta delta = new Delta();
            delta.setRole(null); // 在增量中通常不重复角色
            
            // 创建文本内容部分
            ContentPart textPart = ContentPart.builder()
                    .type("text")
                    .text(content)
                    .build();
            delta.setContent(List.of(textPart));
            choice.setDelta(delta);
            
            return UnifiedStreamChunk.builder()
                    .object("chat.completion.chunk")
                    .choices(List.of(choice))
                    .contentDelta(content)
                    .finished(false)
                    .build();
        }

        return null;
    }

    /**
     * 转换 MessageDelta 事件
     */
    private UnifiedStreamChunk convertMessageDeltaToUnified(AnthropicMessageDeltaEvent messageDelta) {
        // MessageDelta 通常包含 usage 信息
        UnifiedStreamChunk.UnifiedStreamChunkBuilder builder = UnifiedStreamChunk.builder()
                .object("chat.completion.chunk")
                .finished(false);

        if (messageDelta.getDelta() != null && messageDelta.getDelta().getUsage() != null) {
            Usage usage = AnthropicTransformUtils.convertAnthropicUsageToOpenAI(messageDelta.getDelta().getUsage());
            builder.usage(usage);
        }

        return builder.build();
    }

    /**
     * 转换 MessageStop 事件
     */
    private UnifiedStreamChunk convertMessageStopToUnified() {
        Choice choice = new Choice();
        choice.setIndex(0);
        choice.setFinishReason("stop");
        
        return UnifiedStreamChunk.builder()
                .object("chat.completion.chunk")
                .choices(List.of(choice))
                .finished(true)
                .build();
    }
}

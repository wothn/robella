package org.elmo.robella.service.transform;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.config.ProviderType;
import org.elmo.robella.model.internal.*;
import org.elmo.robella.service.VendorTransform;
import org.elmo.robella.util.ConfigUtils;

import org.elmo.robella.model.anthropic.core.*;
import org.elmo.robella.model.anthropic.stream.*;
import org.elmo.robella.model.anthropic.content.*;
import org.elmo.robella.util.AnthropicTransformUtils;

import org.elmo.robella.model.openai.core.Choice;
import org.elmo.robella.model.openai.stream.Delta;
import org.elmo.robella.model.openai.tool.ToolCall;
import org.elmo.robella.model.openai.content.OpenAITextContent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;


/**
 * Anthropic Messages API 转换实现
 */
@Slf4j
@RequiredArgsConstructor
public class AnthropicTransform implements VendorTransform {
    
    private final ConfigUtils configUtils;

    // 移除有状态的Map，改为无状态设计


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
        return null; // TODO: 实现 vendorResponseToUnified
    }

    @Override
    public Object unifiedToVendorResponse(UnifiedChatResponse unifiedResponse) {
        return null; // TODO: 实现 unifiedToVendorResponse
    }

    @Override
    public UnifiedStreamChunk vendorStreamEventToUnified(Object vendorEvent) {
        if (!(vendorEvent instanceof AnthropicStreamEvent event)) {
            return null;
        }

        UnifiedStreamChunk chunk = new UnifiedStreamChunk();
        chunk.setObject("chat.completion.chunk");
        chunk.setChoices(Collections.singletonList(new Choice()));
        chunk.getChoices().get(0).setIndex(0);
        chunk.getChoices().get(0).setFinishReason(null);
        
        // 为所有chunk设置基础ID和时间戳（如果可用）
        setBasicChunkInfo(chunk);

        switch (event.getType()) {
            case "message_start":
                handleMessageStart((AnthropicMessageStartEvent) event, chunk);
                break;
            case "content_block_start":
                return handleContentBlockStart((AnthropicContentBlockStartEvent) event, chunk);
            case "content_block_delta":
                handleContentBlockDelta((AnthropicContentBlockDeltaEvent) event, chunk);
                break;
            case "message_delta":
                handleMessageDelta((AnthropicMessageDeltaEvent) event, chunk);
                break;
            case "message_stop":
                handleMessageStop(chunk);
                break;
            case "content_block_stop":
            case "ping":
                // 忽略这些事件，不生成chunk
                return null;
            default:
                log.warn("Unhandled Anthropic stream event type: {}", event.getType());
                return null;
        }

        return chunk;
    }

    private void setBasicChunkInfo(UnifiedStreamChunk chunk) {
        // 设置基础信息，如果已有则不覆盖
        if (chunk.getId() == null) {
            chunk.setId("chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        }
        if (chunk.getCreated() == null) {
            chunk.setCreated(System.currentTimeMillis() / 1000);
        }
    }

    private void handleMessageStart(AnthropicMessageStartEvent event, UnifiedStreamChunk chunk) {
        AnthropicMessage message = event.getMessage();
        
        // 设置消息ID（添加chatcmpl-前缀）
        chunk.setId("chatcmpl-" + message.getId());
        
        // 设置模型名称
        chunk.setModel(message.getModel());
        
        // 设置创建时间戳
        chunk.setCreated(System.currentTimeMillis() / 1000);
        
        // 设置角色为assistant
        Delta delta = new Delta();
        delta.setRole("assistant");
        chunk.getChoices().get(0).setDelta(delta);
    }

    private void handleContentBlockDelta(AnthropicContentBlockDeltaEvent event, UnifiedStreamChunk chunk) {
        AnthropicDelta delta = event.getDelta();
        
        if (delta.isTextDelta()) {
            handleTextDelta(delta, chunk);
        } else if (delta.isInputJsonDelta()) {
            handleInputJsonDelta(event.getIndex(), delta, chunk);
        } else if (delta.isThinkingDelta()) {
            handleThinkingDelta(delta, chunk);
        }
    }

    private void handleTextDelta(AnthropicDelta delta, UnifiedStreamChunk chunk) {
        Delta openaiDelta = chunk.getChoices().get(0).getDelta();
        if (openaiDelta == null) {
            openaiDelta = new Delta();
            chunk.getChoices().get(0).setDelta(openaiDelta);
        }
        
        // 将文本增量添加到内容数组中
        OpenAITextContent textContent = new OpenAITextContent();
        textContent.setType("text");
        textContent.setText(delta.getText());
        
        if (openaiDelta.getContent() == null) {
            openaiDelta.setContent(Collections.singletonList(textContent));
        } else {
            // 追加到现有内容
            openaiDelta.getContent().add(textContent);
        }
    }

    private void handleInputJsonDelta(Integer contentBlockIndex, AnthropicDelta delta, UnifiedStreamChunk chunk) {
        Delta openaiDelta = chunk.getChoices().get(0).getDelta();
        if (openaiDelta == null) {
            openaiDelta = new Delta();
            chunk.getChoices().get(0).setDelta(openaiDelta);
        }
        
        // 简化处理：直接创建工具调用增量chunk
        // contentBlockIndex对应Anthropic的content block索引，在OpenAI格式中作为tool_calls的index
        ToolCall toolCall = new ToolCall();
        toolCall.setIndex(contentBlockIndex); // 使用content block index作为tool call index
        toolCall.setType("function");
        // 注意：delta chunk中不需要设置ID，只有start事件才设置完整的ID
        
        ToolCall.Function function = new ToolCall.Function();
        // 只设置参数增量，工具名称和ID在content_block_start中已设置
        function.setArguments(delta.getPartialJson());
        toolCall.setFunction(function);
        
        // 每个delta chunk只包含一个工具调用的参数增量
        openaiDelta.setToolCalls(Collections.singletonList(toolCall));
    }

    private void handleThinkingDelta(AnthropicDelta delta, UnifiedStreamChunk chunk) {
        // 将Anthropic思考内容设置到OpenAI Delta的reasoningContent字段
        Delta openaiDelta = chunk.getChoices().get(0).getDelta();
        if (openaiDelta == null) {
            openaiDelta = new Delta();
            chunk.getChoices().get(0).setDelta(openaiDelta);
        }
        
        // 直接设置到reasoningContent字段
        openaiDelta.setReasoningContent(delta.getThinking());
    }

    private void handleMessageDelta(AnthropicMessageDeltaEvent event, UnifiedStreamChunk chunk) {
        AnthropicDelta delta = event.getDelta();
        
        // 设置停止原因映射
        if (delta.getStopReason() != null) {
            String finishReason = mapStopReason(delta.getStopReason());
            chunk.getChoices().get(0).setFinishReason(finishReason);
        }
        
        // 清空delta内容，只保留finish_reason
        chunk.getChoices().get(0).setDelta(new Delta());
    }

    private void handleMessageStop(UnifiedStreamChunk chunk) {
        // message_stop事件不生成chunk，由调用方处理[DONE]标记
        // 这里可以设置一些最终状态
        chunk.getChoices().get(0).setFinishReason("stop");
    }

    private UnifiedStreamChunk handleContentBlockStart(AnthropicContentBlockStartEvent event, UnifiedStreamChunk chunk) {
        // 在content_block_start事件中直接生成工具调用的开始chunk
        if (event.getContentBlock() instanceof AnthropicToolUseContent toolUse) {
            // 生成工具调用开始的chunk
            chunk.setObject("chat.completion.chunk");
            chunk.setChoices(Collections.singletonList(new Choice()));
            chunk.getChoices().get(0).setIndex(0);
            chunk.getChoices().get(0).setFinishReason(null);
            
            Delta delta = new Delta();
            ToolCall toolCall = new ToolCall();
            // 使用content block index作为tool call index
            toolCall.setIndex(event.getIndex());
            toolCall.setType("function");
            // 生成基于Anthropic tool use id的一致性ID
            toolCall.setId(toolUse.getId() != null ? toolUse.getId() : 
                          "toolu_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
            
            ToolCall.Function function = new ToolCall.Function();
            function.setName(toolUse.getName());
            function.setArguments(""); // 初始为空，后续通过delta累加
            toolCall.setFunction(function);
            
            delta.setToolCalls(Collections.singletonList(toolCall));
            chunk.getChoices().get(0).setDelta(delta);
            
            return chunk; // 返回chunk而不是null
        }
        
        // 非工具事件，不生成chunk
        return null;
    }

    private String mapStopReason(String anthropicStopReason) {
        switch (anthropicStopReason) {
            case "end_turn":
                return "stop";
            case "tool_use":
                return "tool_calls";
            case "max_tokens":
                return "length";
            case "stop_sequence":
                return "stop";
            default:
                return "stop";
        }
    }

    @Override
    public String unifiedStreamChunkToVendor(UnifiedStreamChunk chunk) {
        // TODO: 实现 Anthropic 流事件转换
        return null;
    }
    


}

package org.elmo.robella.service.transform;

import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.config.ProviderType;
import org.elmo.robella.model.anthropic.*;
import org.elmo.robella.model.internal.*;
import org.elmo.robella.model.openai.*;
import org.elmo.robella.service.VendorTransform;
import org.elmo.robella.util.AnthropicTransformUtils;

import java.util.*;
import java.util.stream.Collectors;


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
        UnifiedChatRequest request = AnthropicTransformUtils.convertBaseToUnified(req);

        List<OpenAIMessage> allMessages = new ArrayList<>();

        // 处理系统消息
        if (req.getSystem() != null) {
            OpenAIMessage systemMessage = new OpenAIMessage();
            systemMessage.setRole("system");
            OpenAITextContent openAIContent = new OpenAITextContent();
            openAIContent.setType("text");
            openAIContent.setText(req.getSystem());
            systemMessage.setContent(new ArrayList<>());
            systemMessage.getContent().add(openAIContent);
            allMessages.add(systemMessage);
        }

        // 转换消息列表
        if (req.getMessages() != null) {
            List<OpenAIMessage> unifiedMessages = AnthropicTransformUtils.convertAnthropicMessagesToOpenAI(req.getMessages());
            allMessages.addAll(unifiedMessages);
        }

        request.setMessages(allMessages);

        // 转换工具列表
        if (req.getTools() != null && !req.getTools().isEmpty()) {
            List<Tool> unifiedTools = AnthropicTransformUtils.convertAnthropicToolsToOpenAI(req.getTools());
            request.setTools(unifiedTools);
        }

        // 转换工具选择
        if (req.getToolChoice() != null) {
            ToolChoice unifiedToolChoice = AnthropicTransformUtils.convertAnthropicToolChoiceToOpenAI(req.getToolChoice());
            request.setToolChoice(unifiedToolChoice);
        }

        // 处理思考参数
        if (req.getThinking() != null) {
            UnifiedChatRequest.ThinkingOptions thinkingOptions = AnthropicTransformUtils.convertAnthropicThinkingToUnified(req.getThinking());
            if (thinkingOptions != null) {
                request.setThinkingOptions(thinkingOptions);
            }
        }

        // 处理元数据
        if (req.getMetadata() != null) {
            Map<String, Object> metadata = new HashMap<>();
            // 将 AnthropicMetadata 转换为通用的 Map
            metadata.put("anthropic.metadata", req.getMetadata());
            request.setVendorExtras(metadata);
        }

        return request;
    }

    @Override
    public Object unifiedToVendorRequest(UnifiedChatRequest unifiedRequest) {
        AnthropicChatRequest request = AnthropicTransformUtils.convertUnifiedToBase(unifiedRequest);

        // 处理系统消息
        if (unifiedRequest.getMessages() != null && !unifiedRequest.getMessages().isEmpty()) {
            // 从消息列表中提取系统消息
            String systemMessage = AnthropicTransformUtils.extractSystemMessage(unifiedRequest.getMessages());
            if (systemMessage != null) {
                request.setSystem(systemMessage);
            }
        }

        // 转换消息列表
        if (unifiedRequest.getMessages() != null && !unifiedRequest.getMessages().isEmpty()) {
            // 过滤系统消息
            List<OpenAIMessage> filteredMessages = unifiedRequest.getMessages().stream()
                    .filter(m -> !"system".equals(m.getRole()))
                    .collect(Collectors.toList());
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
            AnthropicThinking thinking = AnthropicTransformUtils.convertUnifiedThinkingToAnthropic(unifiedRequest.getThinkingOptions());
            if (thinking != null) {
                request.setThinking(thinking);
            }
        }

        // 处理元数据
        if (unifiedRequest.getVendorExtras() != null) {
            Object anthropicMetadata = unifiedRequest.getVendorExtras().get("anthropic.metadata");
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

        UnifiedChatResponse response = new UnifiedChatResponse();
        response.setId(resp.getId());
        response.setModel(resp.getModel());
        response.setObject("chat.completion"); // Anthropic 返回 "message"，但统一为 OpenAI 格式

        // 转换 Usage
        if (resp.getUsage() != null) {
            Usage usage = AnthropicTransformUtils.convertAnthropicUsageToOpenAI(resp.getUsage());
            response.setUsage(usage);
        }

        // 转换 Choice
        if (resp.getContent() != null) {
            List<Choice> choices = AnthropicTransformUtils.convertAnthropicContentToChoices(resp);
            response.setChoices(choices);
        }

        return response;
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
            List<AnthropicContent> content = AnthropicTransformUtils.convertChoicesToAnthropicContent(unifiedResponse.getChoices());
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
        if (!(vendorEvent instanceof AnthropicStreamEvent event)) {
            return null;
        }
        
        // 处理各种事件类型
        if (event instanceof AnthropicMessageStartEvent) {
            // 开始事件：目前 unified 不需要额外信息，返回一个空的非 finished chunk
            return AnthropicTransformUtils.convertMessageStartToUnified();
        }
        
        if (event instanceof AnthropicContentBlockDeltaEvent deltaEvent) {
            return AnthropicTransformUtils.convertContentBlockDeltaToUnified(deltaEvent);
        }
        
        if (event instanceof AnthropicContentBlockStartEvent startEvent) {
            return AnthropicTransformUtils.convertContentBlockStartToUnified(startEvent);
        }
        
        if (event instanceof AnthropicMessageDeltaEvent messageDelta) {
            return AnthropicTransformUtils.convertMessageDeltaToUnified(messageDelta);
        }
        
        if (event instanceof AnthropicMessageStopEvent) {
            return AnthropicTransformUtils.convertMessageStopToUnified();
        }
        
        if (event instanceof AnthropicContentBlockStopEvent) {
            // content_block_stop 对统一层无增量内容，返回一个空的非 finished chunk 让过滤器处理
            return new UnifiedStreamChunk();
        }
        
        if (event instanceof AnthropicPingEvent) {
            // ping 事件仅用于保活，对上层业务无意义，返回一个空的非 finished chunk
            log.trace("Received ping event");
            return new UnifiedStreamChunk();
        }
        
        if (event instanceof AnthropicErrorEvent errorEvent) {
            // 错误事件记录日志并返回一个空的非 finished chunk，让上层处理异常
            if (errorEvent.getError() != null) {
                log.error("Received error event: type={}, message={}", 
                    errorEvent.getError().getType(), 
                    errorEvent.getError().getMessage());
            }
            return new UnifiedStreamChunk();
        }
        
        log.warn("Unknown Anthropic stream event type: {}", event.getClass().getSimpleName());
        // 对于未识别事件，返回一个空的非 finished chunk
        return new UnifiedStreamChunk();
    }

    @Override
    public Object unifiedStreamChunkToVendor(UnifiedStreamChunk chunk) {
        if (chunk == null) {
            return null;
        }
        
        // 处理结束事件
        if (chunk.isFinished()) {
            AnthropicMessageStopEvent stop = new AnthropicMessageStopEvent();
            stop.setType("message_stop");
            return stop;
        }

        // 处理内容增量 -> content_block_delta (text_delta)
        if (chunk.getContentDelta() != null && !chunk.getContentDelta().isEmpty()) {
            AnthropicContentBlockDeltaEvent deltaEvent = new AnthropicContentBlockDeltaEvent();
            deltaEvent.setType("content_block_delta");
            deltaEvent.setIndex(0); // 暂不支持多 block，统一使用 0
            
            AnthropicDelta delta = new AnthropicDelta();
            delta.setType("text_delta");
            delta.setText(chunk.getContentDelta());
            deltaEvent.setDelta(delta);
            return deltaEvent;
        }

        // 处理推理增量 -> content_block_delta (thinking_delta)
        if (chunk.getReasoningDelta() != null && !chunk.getReasoningDelta().isEmpty()) {
            AnthropicContentBlockDeltaEvent deltaEvent = new AnthropicContentBlockDeltaEvent();
            deltaEvent.setType("content_block_delta");
            deltaEvent.setIndex(0); // 暂不支持多 block，统一使用 0
            
            AnthropicDelta delta = new AnthropicDelta();
            delta.setType("thinking_delta");
            delta.setThinking(chunk.getReasoningDelta());
            deltaEvent.setDelta(delta);
            return deltaEvent;
        }

        // 处理工具调用增量 -> content_block_delta (input_json_delta)
        if (chunk.getToolCallDeltas() != null && !chunk.getToolCallDeltas().isEmpty()) {
            Object toolCallObj = chunk.getToolCallDeltas().get(0);
            if (toolCallObj instanceof ToolCall toolCall && 
                toolCall.getFunction() != null && toolCall.getFunction().getArguments() != null) {
                AnthropicContentBlockDeltaEvent deltaEvent = new AnthropicContentBlockDeltaEvent();
                deltaEvent.setType("content_block_delta");
                deltaEvent.setIndex(0); // 暂不支持多 block，统一使用 0
                
                AnthropicDelta delta = new AnthropicDelta();
                delta.setType("input_json_delta");
                delta.setPartialJson(toolCall.getFunction().getArguments());
                deltaEvent.setDelta(delta);
                return deltaEvent;
            }
        }

        // 处理消息级别的更新 -> message_delta
        if (chunk.getUsage() != null || 
            (chunk.getChoices() != null && !chunk.getChoices().isEmpty() && 
             chunk.getChoices().get(0).getFinishReason() != null)) {
            
            AnthropicMessageDeltaEvent messageDelta = new AnthropicMessageDeltaEvent();
            messageDelta.setType("message_delta");
            
            AnthropicDelta delta = new AnthropicDelta();
            
            // 设置使用量
            if (chunk.getUsage() != null) {
                delta.setUsage(AnthropicTransformUtils.convertOpenAIUsageToAnthropic(chunk.getUsage()));
            }
            
            // 设置停止原因
            if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                Choice choice = chunk.getChoices().get(0);
                if (choice.getFinishReason() != null) {
                    // 将 OpenAI finish_reason 转换为 Anthropic stop_reason
                    String stopReason = convertFinishReasonToStopReason(choice.getFinishReason());
                    delta.setStopReason(stopReason);
                }
            }
            
            messageDelta.setDelta(delta);
            return messageDelta;
        }

        return null;
    }
    
    /**
     * 将 OpenAI finish_reason 转换为 Anthropic stop_reason
     */
    private String convertFinishReasonToStopReason(String finishReason) {
        return switch (finishReason) {
            case "stop" -> "end_turn";
            case "length" -> "max_tokens";
            case "tool_calls" -> "tool_use";
            default -> "end_turn";
        };
    }
}

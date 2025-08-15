package org.elmo.robella.util;

import org.elmo.robella.model.internal.*;
import org.elmo.robella.model.openai.*;

import java.util.*;

/**
 * OpenAI 转换工具类，提供常用的转换方法
 */
public class OpenAITransformUtils {

    /**
     * 将基础请求转换为统一格式，并将简单字段先赋值
     * @param req OpenAI 基础请求
     * @return 统一格式的聊天请求构建器
     */
    public static UnifiedChatRequest.UnifiedChatRequestBuilder convertBaseToUnified(ChatCompletionRequest req) {
        return UnifiedChatRequest.builder()
                .audio(req.getAudio())
                .frequencyPenalty(req.getFrequencyPenalty())
                .logprobs(req.getLogprobs())
                .model(req.getModel())
                .maxTokens(req.getMaxTokens())
                .modalities(req.getModalities())
                .n(req.getN())
                .parallelToolCalls(req.getParallelToolCalls())
                .prediction(req.getPrediction())
                .promptCacheKey(req.getPromptCacheKey())
                .presencePenalty(req.getPresencePenalty())
                .responseFormat(req.getResponseFormat())
                .stream(req.getStream())
                .streamOptions(req.getStreamOptions())
                .stop(req.getStop())
                .temperature(req.getTemperature())
                .topP(req.getTopP())
                .topLogprobs(req.getTopLogprobs())
                .textOptions(req.getText());
    }

    public static ChatCompletionRequest.ChatCompletionRequestBuilder convertUnifiedToBase(UnifiedChatRequest req) {
        return ChatCompletionRequest.builder()
                .model(req.getModel())
                .frequencyPenalty(req.getFrequencyPenalty())
                .maxTokens(req.getMaxTokens())
                .presencePenalty(req.getPresencePenalty())
                .prediction(req.getPrediction())
                .responseFormat(req.getResponseFormat())
                .stop(req.getStop())
                .stream(req.getStream())
                .streamOptions(req.getStreamOptions())
                .temperature(req.getTemperature())
                .topP(req.getTopP())
                .logprobs(req.getLogprobs())
                .topLogprobs(req.getTopLogprobs())
                .modalities(req.getModalities())
                .n(req.getN())
                .parallelToolCalls(req.getParallelToolCalls())
                .promptCacheKey(req.getPromptCacheKey())
                .audio(req.getAudio());
    }


    public static UnifiedChatRequest.ThinkingOptions convertThinkingToUnified(ChatCompletionRequest req, Map<String, Object> tempFields) {
        UnifiedChatRequest.ThinkingOptions thinkingOptions = new UnifiedChatRequest.ThinkingOptions();
        
        // 智谱系列映射
        if(req.getThinking() != null) {
            thinkingOptions.setType(req.getThinking().getType());
            tempFields.put("originalThinkingField", "thinking");
            return thinkingOptions;
        }
        // Qwen系列映射
        else if (req.getEnableThinking() != null && req.getEnableThinking()) {
            thinkingOptions.setType("enable");
            if(req.getThinkingBudget() != null) {
                thinkingOptions.setThinkingBudget(req.getThinkingBudget());
            }
            tempFields.put("originalThinkingField", "enableThinking");
            return thinkingOptions;
        }
        // OpenAI系列映射
        else if (req.getReasoningEffort() != null) {
            thinkingOptions.setReasoningEffort(req.getReasoningEffort());
            tempFields.put("originalThinkingField", "reasoningEffort");
            return thinkingOptions;
        }
        
        // 没有思考选项时返回 null
        return null;
    }

    public static void convertThinkingToChat(UnifiedChatRequest req, ChatCompletionRequest.ChatCompletionRequestBuilder builder) {
        if (req.getTempFields() != null && req.getTempFields().containsKey("originalThinkingField")) {
            String originalField = (String) req.getTempFields().get("originalThinkingField");
            
            if ("thinking".equals(originalField)) {
                Thinking thinking = new Thinking();
                thinking.setType(req.getThinkingOptions().getType());
                builder.thinking(thinking);
            } else if ("enableThinking".equals(originalField)) {
                builder.enableThinking(true);
                if (req.getThinkingOptions().getThinkingBudget() != null) {
                    builder.thinkingBudget(req.getThinkingOptions().getThinkingBudget());
                }
            } else if ("reasoningEffort".equals(originalField)) {
                builder.reasoningEffort(req.getThinkingOptions().getReasoningEffort());
            }
        }
    }



}

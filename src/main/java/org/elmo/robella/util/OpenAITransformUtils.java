package org.elmo.robella.util;

import org.elmo.robella.model.internal.*;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;


/**
 * OpenAI 转换工具类，提供常用的转换方法
 */
public class OpenAITransformUtils {

    /**
     * 将基础请求转换为统一格式，并将简单字段先赋值
     *
     * @param req            OpenAI 基础请求
     * @param unifiedRequest 目标统一格式的聊天请求对象
     */
    public static void convertBaseToUnified(ChatCompletionRequest req, UnifiedChatRequest unifiedRequest) {
        unifiedRequest.setAudio(req.getAudio());
        unifiedRequest.setFrequencyPenalty(req.getFrequencyPenalty());
        unifiedRequest.setLogprobs(req.getLogprobs());
        unifiedRequest.setModel(req.getModel());
        unifiedRequest.setMaxTokens(req.getMaxTokens());
        unifiedRequest.setMaxTokens(req.getMaxCompletionsTokens());
        unifiedRequest.setModalities(req.getModalities());
        unifiedRequest.setN(req.getN());
        unifiedRequest.setParallelToolCalls(req.getParallelToolCalls());
        unifiedRequest.setPrediction(req.getPrediction());
        unifiedRequest.setPromptCacheKey(req.getPromptCacheKey());
        unifiedRequest.setPresencePenalty(req.getPresencePenalty());
        unifiedRequest.setResponseFormat(req.getResponseFormat());
        unifiedRequest.setStream(req.getStream());
        unifiedRequest.setStreamOptions(req.getStreamOptions());
        unifiedRequest.setStop(req.getStop());
        unifiedRequest.setTemperature(req.getTemperature());
        unifiedRequest.setTopP(req.getTopP());
        unifiedRequest.setTopLogprobs(req.getTopLogprobs());
        unifiedRequest.setTextOptions(req.getText());
    }

    public static void convertUnifiedToBase(UnifiedChatRequest req, ChatCompletionRequest chatRequest) {
        chatRequest.setModel(req.getModel());
        chatRequest.setFrequencyPenalty(req.getFrequencyPenalty());
        chatRequest.setMaxTokens(req.getMaxTokens());
        chatRequest.setPresencePenalty(req.getPresencePenalty());
        chatRequest.setPrediction(req.getPrediction());
        chatRequest.setResponseFormat(req.getResponseFormat());
        chatRequest.setStop(req.getStop());
        chatRequest.setStream(req.getStream());
        chatRequest.setStreamOptions(req.getStreamOptions());
        chatRequest.setTemperature(req.getTemperature());
        chatRequest.setTopP(req.getTopP());
        chatRequest.setLogprobs(req.getLogprobs());
        chatRequest.setTopLogprobs(req.getTopLogprobs());
        chatRequest.setModalities(req.getModalities());
        chatRequest.setN(req.getN());
        chatRequest.setParallelToolCalls(req.getParallelToolCalls());
        chatRequest.setPromptCacheKey(req.getPromptCacheKey());
        chatRequest.setAudio(req.getAudio());
    }


    public static void convertThinkingToUnified(ChatCompletionRequest req, UnifiedChatRequest unifiedRequest) {

        ThinkingOptions thinkingOptions = unifiedRequest.getThinkingOptions();
        if (thinkingOptions == null) {
            thinkingOptions = new ThinkingOptions();
            unifiedRequest.setThinkingOptions(thinkingOptions);
        }

        // 智谱系列映射
        if (req.getThinking() != null) {
            thinkingOptions.setType(req.getThinking().getType());
        }
        // Qwen系列映射
        else if (req.getEnableThinking() != null && req.getEnableThinking()) {
            thinkingOptions.setType("enable");
            if (req.getThinkingBudget() != null) {
                thinkingOptions.setThinkingBudget(req.getThinkingBudget());
            }
        }
        // OpenAI系列映射
        else if (req.getReasoningEffort() != null) {
            thinkingOptions.setReasoningEffort(req.getReasoningEffort());
        }

    }

    public static void convertThinkingToChat(UnifiedChatRequest req, ChatCompletionRequest chatRequest) {
        // 端点级别的映射都尽量遵循openai的思考参数
        // Provider需要特殊处理的，可以创建VendorTransform处理
        if (req.getThinkingOptions() != null) {
            ThinkingOptions thinkingOptions = req.getThinkingOptions();
            if (thinkingOptions.getReasoningEffort() != null) {
                chatRequest.setReasoningEffort(thinkingOptions.getReasoningEffort());
            } else if (thinkingOptions.getType() != null) {
                chatRequest.setReasoningEffort(mapThinkingTypeToReasoning(thinkingOptions.getType()));
            }
            
        }
    }

    public static String mapThinkingTypeToReasoning(String type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "enabled", "enable" -> "midium";
            case "disabled", "disable" -> "minimal";
            case "auto" -> "auto";
            default -> "midium";
        };
    }

    public static String mapReasoningToThinkingType(String reasoningEffort) {
        if (reasoningEffort == null) {
            return null;
        }
        return switch (reasoningEffort) {
            case "minimal" -> "disabled";
            case "midium" -> "enabled";
            case "auto" -> "auto";
            default -> "enabled";
        };
    }

}

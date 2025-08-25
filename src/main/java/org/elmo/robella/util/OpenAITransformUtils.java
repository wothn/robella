package org.elmo.robella.util;

import org.elmo.robella.model.internal.*;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.Thinking;


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

        // 智谱系列映射
        if (req.getThinking() != null) {
            thinkingOptions.setType(req.getThinking().getType());
            unifiedRequest.getTempFields().put("req_thinking", "thinking");
        }
        // Qwen系列映射
        else if (req.getEnableThinking() != null && req.getEnableThinking()) {
            thinkingOptions.setType("enable");
            if (req.getThinkingBudget() != null) {
                thinkingOptions.setThinkingBudget(req.getThinkingBudget());
            }
            unifiedRequest.getTempFields().put("req_thinking", "enableThinking");
        }
        // OpenAI系列映射
        else if (req.getReasoningEffort() != null) {
            thinkingOptions.setReasoningEffort(req.getReasoningEffort());
            unifiedRequest.getTempFields().put("req_thinking", "reasoningEffort");
        }

    }

    public static void convertThinkingToChat(UnifiedChatRequest req, ChatCompletionRequest chatRequest) {
        if (req.getTempFields() != null && req.getTempFields().containsKey("thinking")) {
            String originalField = (String) req.getTempFields().get("thinking");

            if ("thinking".equals(originalField)) {
                Thinking thinking = new Thinking();
                thinking.setType(req.getThinkingOptions().getType());
                chatRequest.setThinking(thinking);
            } else if ("enableThinking".equals(originalField)) {
                chatRequest.setEnableThinking(true);
                if (req.getThinkingOptions().getThinkingBudget() != null) {
                    chatRequest.setThinkingBudget(req.getThinkingOptions().getThinkingBudget());
                }
            } else if ("reasoningEffort".equals(originalField)) {
                chatRequest.setReasoningEffort(req.getThinkingOptions().getReasoningEffort());
            }
        }
    }
}

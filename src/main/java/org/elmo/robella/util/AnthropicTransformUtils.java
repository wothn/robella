package org.elmo.robella.util;

import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.anthropic.core.*;
import org.elmo.robella.model.anthropic.tool.*;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.openai.core.StreamOptions;
import org.elmo.robella.model.openai.tool.Function;
import org.elmo.robella.model.openai.tool.Tool;
import org.elmo.robella.model.openai.tool.ToolChoice;

import java.util.*;

/**
 * Anthropic 与 Unified 模型转换工具类
 */
@Slf4j
public class AnthropicTransformUtils {


    public static void convertBaseToUnified(AnthropicChatRequest req, UnifiedChatRequest unifiedRequest) {
        unifiedRequest.setModel(req.getModel());
        unifiedRequest.setStream(req.getStream());
        StreamOptions streamOptions = new StreamOptions();
        streamOptions.setIncludeUsage(true);
        unifiedRequest.setStreamOptions(streamOptions);
        unifiedRequest.setMaxTokens(req.getMaxTokens());
        unifiedRequest.setTemperature(req.getTemperature());
        unifiedRequest.setTopP(req.getTopP());
        unifiedRequest.setTopK(req.getTopK());
        unifiedRequest.setStop(req.getStopSequences());
    }

    public static void convertToolsToUnified(AnthropicChatRequest req, UnifiedChatRequest unifiedRequest) {
        List<Tool> tools = new ArrayList<>();
        for (AnthropicTool tool : req.getTools()) {
            if (tool instanceof AnthropicCustomTool customTool){
                Tool openAITool = new Tool();
                openAITool.setType("function");
                Function function = new Function();
                function.setName(customTool.getName());
                function.setDescription(customTool.getDescription());
                function.setParameters(customTool.getInputSchema());
                openAITool.setFunction(function);
                tools.add(openAITool);
            }
            // TODO: Anthropic的服务端工具保留，如computer、bash、text_editor
        }
        unifiedRequest.setTools(tools);
    }

    public static void convertToolChoiceToUnified(AnthropicChatRequest req, UnifiedChatRequest unifiedRequest) {
        ToolChoice toolChoice = ToolChoice.AUTO; // 默认值

        AnthropicToolChoice anthropicToolChoice = req.getToolChoice();

        if (anthropicToolChoice != null) {
            String type = anthropicToolChoice.getType();
            if ("auto".equals(type)) {
                toolChoice = ToolChoice.AUTO;
            } else if ("any".equals(type)) {
                toolChoice = ToolChoice.REQUIRED;
            } else if ("tool".equals(type)) {
                // 如果是指定工具，设置工具名称
                if (anthropicToolChoice.getName() != null) {
                    toolChoice = ToolChoice.ofFunction(anthropicToolChoice.getName());
                }
                // 如果 name 为 null，保持默认的 AUTO
            } else if ("none".equals(type)) {
                toolChoice = ToolChoice.NONE;
            }
            // 其他未知类型保持默认的 AUTO
        }
        unifiedRequest.setToolChoice(toolChoice);
    }

    public static void convertThinkingToUnified(AnthropicChatRequest req, UnifiedChatRequest unifiedRequest) {
        AnthropicThinking thinking = req.getThinking();

    }
}

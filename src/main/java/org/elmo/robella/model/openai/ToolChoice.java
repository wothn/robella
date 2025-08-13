package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.io.IOException;
import java.util.List;

/**
 * OpenAI Tool选择控制
 * 支持字符串形式："none", "auto", "required"
 * 或对象形式：{"type": "function", "function": {"name": "function_name"}}
 * 或allowed_tools配置：{"type": "allowed_tools", "allowed_tools": [...]}
 * 或custom tool choice：{"type": "custom", "custom": {"name": "custom_tool_name"}}
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize(using = ToolChoice.ToolChoiceSerializer.class)
@JsonDeserialize(using = ToolChoice.ToolChoiceDeserializer.class)
public class ToolChoice {

    /**
     * 选择类型：none, auto, required, function, allowed_tools, custom
     */
    private String type;

    /**
     * 特定function选择，仅当type为function时有效
     */
    private Function function;

    /**
     * 允许的工具列表，仅当type为allowed_tools时有效
     */
    private AllowedTools allowedTools;

    /**
     * 自定义工具选择，仅当type为custom时有效
     */
    private CustomTool custom;

    /**
     * 创建简单字符串类型的ToolChoice
     */
    public static ToolChoice of(String type) {
        ToolChoice choice = new ToolChoice();
        choice.type = type;
        return choice;
    }

    /**
     * 创建function类型的ToolChoice
     */
    public static ToolChoice function(String functionName) {
        ToolChoice choice = new ToolChoice();
        choice.type = "function";
        Function function = new Function();
        function.setName(functionName);
        choice.function = function;
        return choice;
    }

    /**
     * 创建allowed_tools类型的ToolChoice
     */
    public static ToolChoice allowedTools(String mode, List<Tool> tools) {
        ToolChoice choice = new ToolChoice();
        choice.type = "allowed_tools";
        AllowedTools allowedTools = new AllowedTools();
        allowedTools.setMode(mode);
        allowedTools.setTools(tools);
        choice.allowedTools = allowedTools;
        return choice;
    }

    /**
     * 创建custom类型的ToolChoice
     */
    public static ToolChoice custom(String customToolName) {
        ToolChoice choice = new ToolChoice();
        choice.type = "custom";
        CustomTool custom = new CustomTool();
        custom.setName(customToolName);
        choice.custom = custom;
        return choice;
    }

    /**
     * 常用预定义值
     */
    public static final ToolChoice NONE = ToolChoice.of("none");
    public static final ToolChoice AUTO = ToolChoice.of("auto");
    public static final ToolChoice REQUIRED = ToolChoice.of("required");

    /**
     * 检查是否为简单字符串类型
     */
    public boolean isStringType() {
        return function == null && allowedTools == null && custom == null &&
                ("none".equals(type) || "auto".equals(type) || "required".equals(type));
    }

    /**
     * 自定义序列化器
     */
    public static class ToolChoiceSerializer extends JsonSerializer<ToolChoice> {
        @Override
        public void serialize(ToolChoice value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }

            // 如果是简单字符串类型，直接序列化为字符串
            if (value.isStringType()) {
                gen.writeString(value.type);
            } else {
                // 否则序列化为对象
                gen.writeStartObject();
                gen.writeStringField("type", value.type);
                if (value.function != null) {
                    gen.writeObjectField("function", value.function);
                }
                if (value.allowedTools != null) {
                    gen.writeObjectField("allowed_tools", value.allowedTools);
                }
                if (value.custom != null) {
                    gen.writeObjectField("custom", value.custom);
                }
                gen.writeEndObject();
            }
        }
    }

    /**
     * 自定义反序列化器
     */
    public static class ToolChoiceDeserializer extends JsonDeserializer<ToolChoice> {
        @Override
        public ToolChoice deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            if (node.isTextual()) {
                // 字符串类型
                return ToolChoice.of(node.asText());
            } else if (node.isObject()) {
                // 对象类型
                ToolChoice choice = new ToolChoice();
                choice.type = node.get("type").asText();

                JsonNode functionNode = node.get("function");
                if (functionNode != null) {
                    Function function = new Function();
                    function.setName(functionNode.get("name").asText());
                    choice.function = function;
                }

                JsonNode allowedToolsNode = node.get("allowed_tools");
                if (allowedToolsNode != null) {
                    choice.allowedTools = p.getCodec().treeToValue(allowedToolsNode, AllowedTools.class);
                }

                JsonNode customNode = node.get("custom");
                if (customNode != null) {
                    CustomTool custom = new CustomTool();
                    custom.setName(customNode.get("name").asText());
                    choice.custom = custom;
                }

                return choice;
            }

            return null;
        }
    }
}

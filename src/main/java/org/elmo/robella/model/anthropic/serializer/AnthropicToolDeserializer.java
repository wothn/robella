package org.elmo.robella.model.anthropic.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elmo.robella.model.anthropic.tool.*;

import java.io.IOException;

/**
 * AnthropicTool 自定义反序列化器
 */
public class AnthropicToolDeserializer extends JsonDeserializer<AnthropicTool> {
    
    @Override
    public AnthropicTool deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode node = mapper.readTree(parser);
        
        String type = node.has("type") ? node.get("type").asText() : null;
        
        // 根据 type 字段或默认为 custom 类型来决定反序列化的目标类
        Class<? extends AnthropicTool> targetClass;
        switch (type != null ? type : "custom") {
            case "computer_20241022":
                targetClass = AnthropicComputerTool.class;
                break;
            case "bash_20241022":
                targetClass = AnthropicBashTool.class;
                break;
            case "text_editor_20241022":
                targetClass = AnthropicTextEditorTool.class;
                break;
            case "custom":
            default:
                targetClass = AnthropicCustomTool.class;
                break;
        }
        
        return mapper.treeToValue(node, targetClass);
    }
}

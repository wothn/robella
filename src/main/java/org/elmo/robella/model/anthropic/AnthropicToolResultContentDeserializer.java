package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Anthropic 工具结果内容反序列化器
 * 支持：
 * - 纯字符串 -> AnthropicTextContent
 * - 对象数组 -> List<AnthropicContent>（只支持 text 和 image 类型）
 */
public class AnthropicToolResultContentDeserializer extends JsonDeserializer<List<AnthropicContent>> {
    
    @Override
    public List<AnthropicContent> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node == null || node.isNull()) {
            return null;
        }
        
        List<AnthropicContent> contents = new ArrayList<>();
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        
        // 处理纯字符串情况
        if (node.isTextual()) {
            AnthropicTextContent textContent = new AnthropicTextContent();
            textContent.setType("text");
            textContent.setText(node.asText());
            contents.add(textContent);
            return contents;
        }
        
        // 处理数组情况
        if (node.isArray()) {
            for (JsonNode contentNode : node) {
                AnthropicContent content = parseContentNode(contentNode, mapper);
                if (content != null) {
                    contents.add(content);
                }
            }
            return contents;
        }
        
        // 处理单个对象情况
        if (node.isObject()) {
            AnthropicContent content = parseContentNode(node, mapper);
            if (content != null) {
                contents.add(content);
            }
            return contents;
        }
        
        return contents;
    }
    
    private AnthropicContent parseContentNode(JsonNode node, ObjectMapper mapper) throws IOException {
        if (!node.has("type")) {
            return null;
        }
        
        String type = node.get("type").asText();
        // 工具结果只支持 text 和 image 类型
        return switch (type) {
            case "text" -> mapper.treeToValue(node, AnthropicTextContent.class);
            case "image" -> mapper.treeToValue(node, AnthropicImageContent.class);
            default -> null;
        };
    }
}

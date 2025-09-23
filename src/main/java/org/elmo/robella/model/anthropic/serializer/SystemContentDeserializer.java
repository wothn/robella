package org.elmo.robella.model.anthropic.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.anthropic.content.AnthropicTextContent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * System字段反序列化器，支持字符串和列表形式的转换
 */
@Slf4j
public class SystemContentDeserializer extends JsonDeserializer<List<AnthropicTextContent>> {

    @Override
    public List<AnthropicTextContent> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        JsonNode node = p.getCodec().readTree(p);
        List<AnthropicTextContent> result = new ArrayList<>();

        if (node.isTextual()) {
            // 字符串形式：转换为 AnthropicTextContent 列表
            AnthropicTextContent content = new AnthropicTextContent();
            content.setText(node.asText());
            result.add(content);
        } else if (node.isArray()) {
            // 列表形式：直接处理
            for (JsonNode item : node) {
                if (item.isTextual()) {
                    AnthropicTextContent content = new AnthropicTextContent();
                    content.setText(item.asText());
                    result.add(content);
                } else if (item.isObject()) {
                    // 对象形式：直接映射到 AnthropicTextContent
                    AnthropicTextContent content = p.getCodec().treeToValue(item, AnthropicTextContent.class);
                    result.add(content);
                }
            }
        } else if (node.isNull()) {
            // null 值：返回空列表
            return result;
        } else {
            log.warn("Unsupported system content type: {}", node.getNodeType());
        }

        return result;
    }
}
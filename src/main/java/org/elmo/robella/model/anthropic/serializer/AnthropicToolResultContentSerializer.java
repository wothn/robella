package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.elmo.robella.model.anthropic.content.AnthropicContent;
import org.elmo.robella.model.anthropic.content.AnthropicTextContent;

import java.io.IOException;
import java.util.List;

/**
 * Anthropic 工具结果内容序列化器
 */
public class AnthropicToolResultContentSerializer extends JsonSerializer<List<AnthropicContent>> {
    
    @Override
    public void serialize(List<AnthropicContent> value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        
        // 优化：如果只有一个文本内容，直接序列化为字符串
        if (value.size() == 1 && 
            value.get(0) instanceof AnthropicTextContent textContent &&
            "text".equals(textContent.getType())) {
            gen.writeString(textContent.getText() != null ? textContent.getText() : "");
            return;
        }
        
        // 序列化为数组
        gen.writeStartArray();
        for (AnthropicContent content : value) {
            if (content != null) {
                serializers.defaultSerializeValue(content, gen);
            }
        }
        gen.writeEndArray();
    }
}

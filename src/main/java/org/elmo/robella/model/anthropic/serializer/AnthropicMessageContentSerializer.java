package org.elmo.robella.model.anthropic.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.elmo.robella.model.anthropic.content.AnthropicContent;

import java.io.IOException;
import java.util.List;

/**
 * Anthropic 消息内容序列化器
 * 始终将内容序列化为数组格式
 */
public class AnthropicMessageContentSerializer extends JsonSerializer<List<AnthropicContent>> {
    
    @Override
    public void serialize(List<AnthropicContent> value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        
        // 始终序列化为数组
        gen.writeStartArray();
        for (AnthropicContent content : value) {
            if (content != null) {
                serializers.defaultSerializeValue(content, gen);
            }
        }
        gen.writeEndArray();
    }
}

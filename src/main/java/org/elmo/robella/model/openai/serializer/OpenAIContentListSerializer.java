package org.elmo.robella.model.openai.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.elmo.robella.model.openai.content.OpenAIAudioContent;
import org.elmo.robella.model.openai.content.OpenAIContent;
import org.elmo.robella.model.openai.content.OpenAIImageContent;
import org.elmo.robella.model.openai.content.OpenAITextContent;

import java.io.IOException;
import java.util.List;

/**
 * 序列化 content 字段：
 * - 单一 text 片段 -> 直接写字符串
 * - 其他情况 -> 数组
 */
public class OpenAIContentListSerializer extends JsonSerializer<List<OpenAIContent>> {
    /**
     * 序列化ContentPart列表为JSON格式
     *
     * @param value 要序列化的ContentPart对象列表
     * @param gen JSON生成器，用于写入JSON数据
     * @param serializers 序列化提供者，可用于查找其他序列化器
     * @throws IOException 当IO操作出现错误时抛出
     */
    @Override
    public void serialize(List<OpenAIContent> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 处理null值情况
        if (value == null) { gen.writeNull(); return; }
        // 优化单一text类型内容的序列化
        if (value.size() == 1 && value.get(0) != null && "text".equals(value.get(0).getType()) && value.get(0) instanceof OpenAITextContent textContent) {
            gen.writeString(textContent.getText() == null ? "" : textContent.getText());
            return;
        }
        // 处理数组形式的序列化
        gen.writeStartArray();
        for (OpenAIContent part : value) {
            if (part == null) continue;
            gen.writeStartObject();
            gen.writeStringField("type", part.getType());
            switch (part.getType()) {
                case "text" -> {
                    if (part instanceof OpenAITextContent textPart) {
                        gen.writeStringField("text", textPart.getText());
                    }
                }
                case "image_url" -> {
                    if (part instanceof OpenAIImageContent imagePart) {
                        gen.writeObjectField("image_url", imagePart.getImageUrl());
                    }
                }
                case "input_audio" -> {
                    if (part instanceof OpenAIAudioContent audioPart) {
                        gen.writeObjectField("input_audio", audioPart.getInputAudio());
                    }
                }
                default -> { /* 忽略未知 */ }
            }
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }
}

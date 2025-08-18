package org.elmo.robella.model.openai;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.List;

/**
 * 序列化 content 字段：
 * - 单一 text 片段 -> 直接写字符串
 * - 其他情况 -> 数组
 */
public class ContentPartListSerializer extends JsonSerializer<List<ContentPart>> {
    /**
     * 序列化ContentPart列表为JSON格式
     *
     * @param value 要序列化的ContentPart对象列表
     * @param gen JSON生成器，用于写入JSON数据
     * @param serializers 序列化提供者，可用于查找其他序列化器
     * @throws IOException 当IO操作出现错误时抛出
     */
    @Override
    public void serialize(List<ContentPart> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 处理null值情况
        if (value == null) { gen.writeNull(); return; }
        // 优化单一text类型内容的序列化
        if (value.size() == 1 && value.get(0) != null && "text".equals(value.get(0).getType())) {
            gen.writeString(value.get(0).getText() == null ? "" : value.get(0).getText());
            return;
        }
        // 处理数组形式的序列化
        gen.writeStartArray();
        for (ContentPart part : value) {
            if (part == null) continue;
            gen.writeStartObject();
            gen.writeStringField("type", part.getType());
            switch (part.getType()) {
                case "text" -> gen.writeStringField("text", part.getText());
                case "image_url" -> gen.writeObjectField("image_url", part.getImageUrl());
                case "input_audio" -> gen.writeObjectField("input_audio", part.getInputAudio());
                default -> { /* 忽略未知 */ }
            }
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }
}

package org.elmo.robella.model.openai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 反序列化 content 字段：
 * - 纯字符串 -> [ text part ]
 * - 对象(含type) -> [object]
 * - 数组 -> 列表
 */
public class ContentPartListDeserializer extends JsonDeserializer<List<ContentPart>> {
    /**
     * 反序列化JsonNode为ContentPart列表
     * 
     * @param p Json解析器
     * @param ctxt 反序列化上下文
     * @return ContentPart对象列表
     * @throws IOException 当IO操作出现错误时抛出
     */
    @Override
    public List<ContentPart> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node == null || node.isNull()) return null;
        List<ContentPart> parts = new ArrayList<>();
        // 处理文本节点情况
        if (node.isTextual()) {
            parts.add(ContentPart.ofText(node.asText()));
            return parts;
        }
        // 处理对象节点情况
        if (node.isObject()) {
            parts.add(parseObject(node, p.getCodec()));
            return parts;
        }
        // 处理数组节点情况
        if (node.isArray()) {
            for (JsonNode n : node) {
                if (n.isTextual()) {
                    parts.add(ContentPart.ofText(n.asText()));
                } else if (n.isObject()) {
                    parts.add(parseObject(n, p.getCodec()));
                }
            }
            return parts;
        }
        return parts;
    }

    /**
     * 解析JSON对象节点为ContentPart对象
     * 
     * @param n JSON对象节点
     * @param codec ObjectMapper实例，用于类型转换
     * @return 解析后的ContentPart对象
     */
    private ContentPart parseObject(JsonNode n, Object codec) {
        String type = n.path("type").asText();
        ObjectMapper mapper = (ObjectMapper) codec;
        switch (type) {
            case "text":
                return ContentPart.ofText(n.path("text").asText(""));
            case "image_url":
                ImageUrl img = mapper.convertValue(n.path("image_url"), ImageUrl.class);
                return ContentPart.builder().type("image_url").imageUrl(img).build();
            case "input_audio":
                InputAudio audio = mapper.convertValue(n.path("input_audio"), InputAudio.class);
                return ContentPart.builder().type("input_audio").inputAudio(audio).build();
            default:
                return ContentPart.ofText(n.toString());
        }
    }
}

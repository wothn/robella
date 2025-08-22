package org.elmo.robella.model.openai.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elmo.robella.model.openai.audio.InputAudio;
import org.elmo.robella.model.openai.content.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 反序列化 content 字段：
 * - 纯字符串 -> [ text part ]
 * - 对象(含type) -> [object]
 * - 数组 -> 列表
 */
public class OpenAIContentListDeserializer extends JsonDeserializer<List<OpenAIContent>> {
    /**
     * 反序列化JsonNode为OpenAIContent列表
     * 
     * @param p Json解析器
     * @param ctxt 反序列化上下文
     * @return OpenAIContent对象列表
     * @throws IOException 当IO操作出现错误时抛出
     */
    @Override
    public List<OpenAIContent> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node == null || node.isNull()) return null;
        
        List<OpenAIContent> parts = new ArrayList<>();
        
        // 处理文本节点情况
        if (node.isTextual()) {
            parts.add(createTextContent(node.asText()));
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
                    parts.add(createTextContent(n.asText()));
                } else if (n.isObject()) {
                    parts.add(parseObject(n, p.getCodec()));
                }
            }
            return parts;
        }
        
        return parts;
    }

    /**
     * 创建文本类型的内容对象
     * 
     * @param text 文本内容
     * @return OpenAIContent对象
     */
    private OpenAIContent createTextContent(String text) {
        OpenAITextContent content = new OpenAITextContent();
        content.setType("text");
        content.setText(text);
        return content;
    }

    /**
     * 解析JSON对象节点为OpenAIContent对象
     * 
     * @param n JSON对象节点
     * @param codec ObjectMapper实例，用于类型转换
     * @return 解析后的OpenAIContent对象
     */
    private OpenAIContent parseObject(JsonNode n, Object codec) {
        String type = n.path("type").asText();
        ObjectMapper mapper = (ObjectMapper) codec;
        
        switch (type) {
            case "text":
                OpenAITextContent textContent = new OpenAITextContent();
                textContent.setType("text");
                textContent.setText(n.path("text").asText(""));
                return textContent;
            case "image_url":
                OpenAIImageContent imageContent = new OpenAIImageContent();
                imageContent.setType("image_url");
                ImageUrl img = mapper.convertValue(n.path("image_url"), ImageUrl.class);
                imageContent.setImageUrl(img);
                return imageContent;
            case "input_audio":
                OpenAIAudioContent audioContent = new OpenAIAudioContent();
                audioContent.setType("input_audio");
                InputAudio audio = mapper.convertValue(n.path("input_audio"), InputAudio.class);
                audioContent.setInputAudio(audio);
                return audioContent;
            default:
                // 对于未知类型，当作文本处理
                OpenAITextContent defaultContent = new OpenAITextContent();
                defaultContent.setType("text");
                defaultContent.setText(n.toString());
                return defaultContent;
        }
    }
}

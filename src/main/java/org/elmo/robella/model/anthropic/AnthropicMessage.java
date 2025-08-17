package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Anthropic 消息模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnthropicMessage {
    
    /**
     * 消息角色：user 或 assistant
     */
    private String role;
    
    /**
     * 消息内容，可以是字符串或内容块数组
     * 使用自定义序列化器处理多态性
     */
    @JsonProperty("content")
    @JsonDeserialize(using = AnthropicMessageContentDeserializer.class)
    @JsonSerialize(using = AnthropicMessageContentSerializer.class)
    @Builder.Default
    private List<AnthropicContent> content = new ArrayList<>();
    
    // --- 便利方法 ---
    
    /**
     * 获取第一个文本内容（如果存在）
     */
    public String getFirstTextContent() {
        return content.stream()
                .filter(c -> c instanceof AnthropicTextContent)
                .map(c -> ((AnthropicTextContent) c).getText())
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 是否只包含单个文本内容
     */
    public boolean isSingleTextContent() {
        return content.size() == 1 && 
               content.get(0) instanceof AnthropicTextContent;
    }
    
    /**
     * 添加文本内容
     */
    public AnthropicMessage addTextContent(String text) {
        AnthropicTextContent textContent = new AnthropicTextContent();
        textContent.setType("text");
        textContent.setText(text);
        this.content.add(textContent);
        return this;
    }
    
    /**
     * 添加图片内容
     */
    public AnthropicMessage addImageContent(String mediaType, String base64Data) {
        AnthropicImageContent imageContent = new AnthropicImageContent();
        imageContent.setType("image");
        
        AnthropicImageSource source = new AnthropicImageSource();
        source.setType("base64");
        source.setMediaType(mediaType);
        source.setData(base64Data);
        
        imageContent.setSource(source);
        this.content.add(imageContent);
        return this;
    }
    
    // --- 静态工厂方法 ---
    
    /**
     * 创建用户文本消息
     */
    public static AnthropicMessage userText(String text) {
        return AnthropicMessage.builder()
                .role("user")
                .build()
                .addTextContent(text);
    }
    
    /**
     * 创建助手文本消息
     */
    public static AnthropicMessage assistantText(String text) {
        return AnthropicMessage.builder()
                .role("assistant")
                .build()
                .addTextContent(text);
    }
}

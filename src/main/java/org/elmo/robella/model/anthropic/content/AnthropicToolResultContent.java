package org.elmo.robella.model.anthropic.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.elmo.robella.model.anthropic.AnthropicToolResultContentDeserializer;
import org.elmo.robella.model.anthropic.AnthropicToolResultContentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Anthropic 工具结果内容块
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicToolResultContent extends AnthropicContent {
    
    /**
     * 对应的工具使用 ID
     */
    @JsonProperty("tool_use_id")
    private String toolUseId;
    
    /**
     * 工具执行结果，可以是字符串或内容块数组（只支持 text 和 image 类型）
     */
    @JsonProperty("content")
    @JsonDeserialize(using = AnthropicToolResultContentDeserializer.class)
    @JsonSerialize(using = AnthropicToolResultContentSerializer.class)
    private List<AnthropicContent> content = new ArrayList<>();
    
    /**
     * 是否为错误结果
     */
    @JsonProperty("is_error")
    private Boolean isError;
    
    // --- 便利方法 ---
    
    /**
     * 设置文本结果
     */
    public AnthropicToolResultContent setTextResult(String text) {
        AnthropicTextContent textContent = new AnthropicTextContent();
        textContent.setType("text");
        textContent.setText(text);
        this.content = List.of(textContent);
        return this;
    }
    
    /**
     * 获取第一个文本结果
     */
    public String getFirstTextResult() {
        return content.stream()
                .filter(c -> c instanceof AnthropicTextContent)
                .map(c -> ((AnthropicTextContent) c).getText())
                .findFirst()
                .orElse(null);
    }
}

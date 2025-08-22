package org.elmo.robella.model.openai.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息注释
 * 包含消息中的引用、链接等额外信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageAnnotation {
    
    /**
     * 注释类型，目前只支持 "url_citation"
     */
    private String type;
    
    /**
     * URL引用信息
     * 当type="url_citation"时使用
     */
    @JsonProperty("url_citation")
    private UrlCitation urlCitation;
    
    /**
     * 注释在消息中的结束位置索引
     */
    @JsonProperty("end_index")
    private Integer endIndex;
    
    /**
     * 注释在消息中的开始位置索引
     */
    @JsonProperty("start_index")
    private Integer startIndex;
    
    /**
     * URL引用的详细信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UrlCitation {
        
        /**
         * 引用资源的标题
         */
        private String title;
        
        /**
         * 引用的URL地址
         */
        private String url;
    }
}

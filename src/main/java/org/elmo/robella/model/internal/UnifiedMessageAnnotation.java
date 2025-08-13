package org.elmo.robella.model.internal;

import lombok.*;
import java.util.Map;

/**
 * 统一消息注解结构
 * 支持 OpenAI 的消息注解功能，如引用、工具调用等
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedMessageAnnotation {
    
    /**
     * 注解类型：citation, tool_call, reasoning, warning 等
     */
    private String type;
    
    /**
     * 引用信息（当 type=citation）
     */
    private Citation citation;
    
    /**
     * 工具调用引用（当 type=tool_call）
     */
    private String toolCallId;
    
    /**
     * 推理步骤引用（当 type=reasoning）
     */
    private String reasoningStep;
    
    /**
     * 文本范围（在消息中的位置）
     */
    private TextRange range;
    
    /**
     * 注解元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 引用信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation {
        /**
         * 引用的文档或来源 ID
         */
        private String sourceId;
        
        /**
         * 引用的具体位置或页码
         */
        private String location;
        
        /**
         * 引用文本
         */
        private String text;
        
        /**
         * 引用 URL（如果有）
         */
        private String url;
        
        /**
         * 置信度分数
         */
        private Double confidence;
    }
    
    /**
     * 文本范围定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextRange {
        /**
         * 开始位置
         */
        private Integer start;
        
        /**
         * 结束位置
         */
        private Integer end;
        
        /**
         * 范围类型（character, token, word 等）
         */
        private String type;
    }
}

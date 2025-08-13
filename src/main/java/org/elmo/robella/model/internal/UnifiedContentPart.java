package org.elmo.robella.model.internal;

import lombok.*;

/**
 * 统一多模态内容片段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedContentPart {
    private String type;              // text | image | audio | video | file | json | tool_result | reasoning | refusal | other
    private String text;              // 文本
    private String url;               // 资源 URL（图片/文件）
    private String detail;            // 图像 detail / 说明 / caption
    private String format;            // 媒体格式（mp3/mp4/png 等）
    private String data;              // base64 数据
    private String mimeType;          // MIME
    private String json;              // 结构化 JSON（字符串形式）
    private Long sizeBytes;           // 文件大小（若有）
    private Integer width;            // 图像宽
    private Integer height;           // 图像高
    private Double durationSeconds;   // 音/视频时长
    private String toolCallId;        // 若来源于 tool result 对应调用ID
    private String segmentId;         // 序列化分片 ID（例如长文本切片）
    private Integer segmentIndex;     // 分片顺序
    private Boolean finalSegment;     // 是否最后一片
    
    // 新增字段以支持更多功能
    private String refusal;           // 拒绝回答的原因（OpenAI safety）
    private String voice;             // 音频声音类型（OpenAI audio）
    private Object transcript;        // 音频转录文本（若有）
    private Boolean isError;          // 标记内容是否为错误信息
    private String errorCode;         // 错误代码
    private Object sourceAttribution; // 内容来源归属（版权等）
    
    private java.util.Map<String,Object> metadata; // 片段级元数据（OCR lang 等）

    // 静态工厂方法
    public static UnifiedContentPart text(String t){
        return UnifiedContentPart.builder().type("text").text(t).build();
    }
    
    public static UnifiedContentPart image(String url, String detail){
        return UnifiedContentPart.builder().type("image").url(url).detail(detail).build();
    }
    
    public static UnifiedContentPart audio(String base64, String format){
        return UnifiedContentPart.builder().type("audio").data(base64).format(format).build();
    }
    
    public static UnifiedContentPart refusal(String reason){
        return UnifiedContentPart.builder().type("refusal").refusal(reason).build();
    }
    
    public static UnifiedContentPart toolResult(String toolCallId, String content){
        return UnifiedContentPart.builder().type("tool_result").toolCallId(toolCallId).text(content).build();
    }
    
    public static UnifiedContentPart reasoning(String reasoningText){
        return UnifiedContentPart.builder().type("reasoning").text(reasoningText).build();
    }
}

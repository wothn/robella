package org.elmo.robella.model.internal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UnifiedChatResponse {
    private String id;
    private String model;
    private String content;          // 汇总文本内容
    private String finishReason;     // stop / length / tool_calls ...
    private Usage usage;
    private Object rawVendor;        // 原始厂商响应（调试用）

    @Data
    @Builder
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
}

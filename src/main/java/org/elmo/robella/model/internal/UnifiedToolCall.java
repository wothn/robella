package org.elmo.robella.model.internal;

import lombok.*;

/**
 * 统一工具调用记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedToolCall {
    private String id;            // 调用ID
    private String type;          // function
    private FunctionCall function;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionCall {
        private String name;
        private String arguments; // JSON 字符串
        private String output;    // tool 消息返回结果（仅 role=tool 时）
    }
}

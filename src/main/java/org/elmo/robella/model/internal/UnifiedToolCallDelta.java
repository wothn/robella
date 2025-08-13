package org.elmo.robella.model.internal;

import lombok.*;

/**
 * 流式工具调用增量（用于拼接 arguments）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedToolCallDelta {
    private String id;           // 调用ID（可能首包出现）
    private String type;         // 工具类型（通常是 "function"）
    private String name;         // function 名称（可能首包出现）
    private String argumentsDelta; // arguments 增量片段
    private String argumentsJson;  // 若厂商直接给结构化 JSON
    private String outputDelta;    // 工具执行结果的增量（若支持）
    private Boolean error;         // 标记该调用出错
    private String errorMessage;   // 错误信息
    private String errorCode;      // 错误代码
    private boolean finished;      // 是否结束（当 finishReason=tool_calls 或完成本调用）
    private Object metadata;       // 工具调用元数据
}

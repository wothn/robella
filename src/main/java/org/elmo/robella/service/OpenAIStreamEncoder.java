package org.elmo.robella.service;

import org.elmo.robella.model.internal.UnifiedStreamChunk;

/**
 * 统一增量 -> OpenAI ChatCompletion 流式 JSON 编码器（不含 SSE "data:" 包装）。
 */
public interface OpenAIStreamEncoder {
    /**
     * 将一个统一增量块编码为 OpenAI 兼容 JSON。返回 null/空串表示无需下发。
     * @param chunk 统一增量
     * @param model 模型名（保持与请求一致）
     * @param idHint 可选 ID（用于保持同一会话稳定 ID）
     */
    String encodeChunk(UnifiedStreamChunk chunk, String model, String idHint);

    /**
     * 结束标记。[DONE]
     */
    String encodeDone();
}

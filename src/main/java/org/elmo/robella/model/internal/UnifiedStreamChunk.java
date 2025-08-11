package org.elmo.robella.model.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedStreamChunk {
    private String contentDelta; // 新增的文本片段
    private boolean finished;    // 是否结束
}

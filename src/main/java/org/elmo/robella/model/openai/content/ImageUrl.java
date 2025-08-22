package org.elmo.robella.model.openai.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图像URL结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageUrl {
    /**
     * 图像地址（必需）
     */
    private String url;

    /**
     * 细节层级：auto | low | high
     */
    private String detail;
}

package org.elmo.robella.model.anthropic.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 计算机工具
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicComputerTool extends AnthropicTool {
    
    /**
     * 显示宽度(像素)
     */
    @JsonProperty("display_width_px")
    private Integer displayWidthPx;
    
    /**
     * 显示高度(像素)
     */
    @JsonProperty("display_height_px")
    private Integer displayHeightPx;
    
    /**
     * X11 显示编号
     */
    @JsonProperty("display_number")
    private Integer displayNumber;
}

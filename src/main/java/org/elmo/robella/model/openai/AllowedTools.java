package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * OpenAI Allowed Tools配置
 * 用于限制模型可以调用的工具范围
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AllowedTools {
    
    /**
     * 模式：auto - 允许模型从所有工具中选择，required - 必须调用其中一个工具
     */
    private String mode;
    
    /**
     * 允许的工具列表
     */
    private List<Tool> tools;
}

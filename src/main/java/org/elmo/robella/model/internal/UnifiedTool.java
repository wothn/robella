package org.elmo.robella.model.internal;

import lombok.*;

/**
 * 统一工具定义（ function 工具为主 ）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedTool {
    private String type;              // function, code_interpreter, file_search, browser 等
    private Function function;        // function 工具定义
    private Object codeInterpreter;   // 代码解释器配置（OpenAI）
    private Object fileSearch;       // 文件搜索配置（OpenAI）
    private Object browser;           // 浏览器工具配置
    private Object custom;            // 自定义工具配置
    private Object cacheControl;      // 缓存控制（Anthropic）

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Function {
        private String name;
        private String description;
        private Object parameters;    // 可为 Map / JSON Schema 对象
        private Boolean strict;       // 是否严格模式（OpenAI structured outputs）
        private Object examples;      // 使用示例
        private Object metadata;      // 函数元数据
    }
}

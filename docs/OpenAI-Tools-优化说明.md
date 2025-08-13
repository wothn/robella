# OpenAI Tools 优化文档

## 概述

根据 OpenAI API 规范，我们对工具列表（tools）相关的类进行了全面优化，增强了对各种工具类型和配置选项的支持。

## 主要优化内容

### 1. Tool 类扩展
- **新增支持**: 除了原有的 `function` 类型，现在还支持 `custom` 类型工具
- **字段增加**: 添加了 `custom` 字段，用于配置自定义工具

### 2. Function 类增强
- **新增 strict 字段**: 支持严格模式，确保模型严格遵循参数模式
- **完善注释**: 详细说明了各字段的用途和限制

### 3. CustomTool 类优化
- **新增 description 字段**: 提供工具描述信息
- **新增 format 字段**: 支持自定义工具的输入格式配置

### 4. 新增 CustomToolFormat 类
支持两种格式类型：
- **文本格式 (text)**: 无约束的自由文本输入
- **语法格式 (grammar)**: 用户定义的语法结构

### 5. ChatCompletionRequest 优化
- **top_logprobs 字段**: 完善了注释，说明了取值范围和使用条件

### 6. 新增 ToolsBuilder 工具类
提供便捷的构建方法：
- `function()`: 创建函数工具
- `custom()`: 创建自定义工具
- `textFormat()`: 创建文本格式
- `grammarFormat()`: 创建语法格式

### 7. 新增 ToolsExample 示例类
提供完整的使用示例：
- 基础函数工具创建
- 严格模式函数工具
- 自定义工具配置
- ToolChoice 使用方法
- 完整聊天请求示例

## 使用示例

### 创建基础函数工具
```java
Tool weatherTool = ToolsBuilder.function(
    "get_weather",
    "获取指定城市的天气信息"
);
```

### 创建带参数的函数工具
```java
Map<String, Object> parameters = new HashMap<>();
// ... 参数定义
Tool calculatorTool = ToolsBuilder.function(
    "calculate",
    "执行数学计算",
    parameters
);
```

### 创建严格模式函数工具
```java
Tool strictTool = ToolsBuilder.function(
    "search",
    "搜索信息",
    parameters,
    true // 启用严格模式
);
```

### 创建自定义工具
```java
Tool customTool = ToolsBuilder.custom(
    "data_processor",
    "处理数据",
    ToolsBuilder.textFormat()
);
```

### 配置 ToolChoice
```java
// 自动选择
ToolChoice auto = ToolChoice.AUTO;

// 指定特定函数
ToolChoice specific = ToolChoice.function("get_weather");

// 指定自定义工具
ToolChoice custom = ToolChoice.custom("data_processor");
```

## 兼容性

所有优化都保持了向后兼容性，现有代码无需修改即可继续使用。新增的字段都使用了 `@JsonInclude(JsonInclude.Include.NON_NULL)` 注解，确保不会影响现有的 JSON 序列化。

## 文件清单

- `Tool.java` - 主工具类，支持 function 和 custom 类型
- `Function.java` - 函数工具定义，增加 strict 支持
- `CustomTool.java` - 自定义工具定义
- `CustomToolFormat.java` - 自定义工具格式定义
- `ToolsBuilder.java` - 工具构建器（新增）
- `ToolsExample.java` - 使用示例（新增）
- `ChatCompletionRequest.java` - 优化了 top_logprobs 注释

这次优化使 Robella 项目能够完全支持 OpenAI Tools API 的所有功能，为用户提供了更灵活和强大的工具配置能力。

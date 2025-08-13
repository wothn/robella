# ToolChoice 优化说明

## 优化前
```java
/**
 * 工具选择控制
 */
@JsonProperty("tool_choice")
private Object toolChoice; // String或ToolChoice对象
```

在优化前，`toolChoice` 字段使用 `Object` 类型，需要在运行时进行类型判断和转换，缺乏类型安全性。

## 优化后
```java
/**
 * 工具选择控制
 * 支持字符串形式："none", "auto", "required"
 * 或对象形式：{"type": "function", "function": {"name": "function_name"}}
 * 或allowed_tools配置：{"type": "allowed_tools", "allowed_tools": [...]}
 * 或custom tool choice：{"type": "custom", "custom": {"name": "custom_tool_name"}}
 */
@JsonProperty("tool_choice")
private ToolChoice toolChoice;
```

现在使用强类型的 `ToolChoice` 类，提供：

### 1. 类型安全
- 编译时类型检查
- IDE 自动补全和代码提示
- 减少运行时类型转换错误

### 2. 便利的静态方法
```java
// 预定义常量
ToolChoice.NONE      // "none"
ToolChoice.AUTO      // "auto"  
ToolChoice.REQUIRED  // "required"

// 工厂方法
ToolChoice.of("auto")                            // 创建字符串类型
ToolChoice.function("my_function")               // 创建函数类型
ToolChoice.allowedTools("auto", toolsList)      // 创建allowed_tools类型
ToolChoice.custom("my_custom_tool")              // 创建custom类型
```

### 3. 智能序列化
- 字符串类型：序列化为简单字符串 `"auto"`
- 函数类型：序列化为对象 `{"type": "function", "function": {"name": "my_function"}}`
- 允许工具类型：序列化为对象 `{"type": "allowed_tools", "allowed_tools": {"mode": "auto", "tools": [...]}}`
- 自定义工具类型：序列化为对象 `{"type": "custom", "custom": {"name": "my_custom_tool"}}`

### 4. 自动反序列化
- 支持从字符串和对象两种 JSON 格式自动反序列化
- 无需手动类型判断

### 5. 便利方法
```java
toolChoice.isStringType()  // 判断是否为简单字符串类型
```

## 使用示例

```java
// 设置自动选择
request.setToolChoice(ToolChoice.AUTO);

// 禁用工具
request.setToolChoice(ToolChoice.NONE);

// 指定特定函数
request.setToolChoice(ToolChoice.function("calculate"));

// 从字符串创建
request.setToolChoice(ToolChoice.of("required"));

// 设置允许的工具列表（限制工具调用范围）
List<Tool> allowedTools = Arrays.asList(tool1, tool2);
request.setToolChoice(ToolChoice.allowedTools("auto", allowedTools));

// 指定自定义工具
request.setToolChoice(ToolChoice.custom("my_custom_tool"));
```

## 兼容性
此优化完全向后兼容，`OpenAIVendorTransform` 中的 `convertToolChoice` 方法处理了从 `UnifiedChatRequest` 的 `Object toolChoice` 到 `ToolChoice` 的转换。

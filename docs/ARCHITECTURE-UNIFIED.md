# 统一对话改造记录 (OpenAI + Anthropic)

## 1. 目标概述
在保留 OpenAI 兼容接口 `/v1/chat/completions` 的同时，对外新增 Anthropic 原生接口 `/anthropic/v1/messages`，内部统一使用 `Unified*` 数据模型，形成“多入口 → 统一模型 → 路由/适配 → 厂商调用 → 统一响应 → 多出口格式”的闭环。后续可平滑扩展 Gemini、Qwen 等。

## 2. 当前完成度 (2025-08-13)
已完成:
1. 内部统一模型: `UnifiedChatRequest`, `UnifiedChatResponse`, `UnifiedStreamChunk`, `UnifiedError` (已二次扩展多模态/工具/增量字段)。
2. `TransformService` 扩展：支持 OpenAI 与 Anthropic 的 Unified 双向转换（请求/响应/流事件）。
3. `ForwardingService` 新增 `forwardUnified` / `streamUnified`，并在实现中按 provider 直接调用对应适配器（不再经 OpenAI 中转）。
4. `OpenAIController` 已迁移到 unified 流程，仍保持对外兼容 OpenAI 格式；原 `forwardChatCompletion` / `streamChatCompletion` 暂保留用于兼容/回退。
5. 新增 `AnthropicController` 暴露原生 `/anthropic/v1/messages`（流式 SSE + 非流式 JSON）。
6. 路由增强：`RoutingService` 新增 `decideProviderByModel(String)`，统一入口不再构造临时 `ChatCompletionRequest`。
7. 错误处理：`GlobalExceptionHandler` 根据 URL 前缀动态输出 OpenAI 或 Anthropic 官方风格错误 JSON。

待完成 / 规划：Gemini 原生接口、多模态统一表示、工具调用抽象、细粒度统计与速率治理、统一权限与配额策略。

## 3. 统一数据流设计
```
入口请求 (OpenAI / Anthropic) → Transform 入 (to UnifiedChatRequest)
	→ Routing(decideProviderByModel / 强制 provider) → AdapterFactory → Vendor Adapter
	→ 厂商响应 / 流事件 → 统一转换 (UnifiedChatResponse / UnifiedStreamChunk)
	→ Transform 出：按出口协议再封装 (OpenAI JSON / Anthropic SSE)
```

## 4. 路由逻辑
- 旧: 通过 `RoutingService.decideProvider(ChatCompletionRequest)`。
- 新: 统一代码路径改为优先调用 `decideProviderByModel(model)`，减少对象构造。
- 控制器可通过 `forcedProvider`（如 Anthropic 原生入口）绕过模型推断。

## 5. 转换职责划分
| 层 | 输入 | 输出 | 说明 |
|----|------|------|------|
| Controller | 协议请求 | UnifiedChatRequest | 入口协议差异终结点 |
| Transform (to vendor) | UnifiedChatRequest | Vendor Request | 依据 providerType 决定分支 |
| Adapter | Vendor Request | Vendor Response / Stream | 只关心厂商调用与底层 HTTP |
| Transform (from vendor) | Vendor Response / Event | UnifiedChatResponse / UnifiedStreamChunk | 统一抽象 |
| Controller | Unified* | 协议响应 | 输出格式封装 |

## 6. 流式映射 (Anthropic)
| Anthropic event | Unified 映射 | 说明 |
|-----------------|--------------|------|
| message_start | ignore | 无文本 |
| content_block_start | ignore | 结构起始 |
| content_block_delta | contentDelta | 取 `delta.text` |
| content_block_stop | ignore | 结束标记无文本 |
| message_delta | ignore | stop_reason 在 stop 处理 |
| message_stop | finished=true | 结束事件 |

OpenAI 流式：Unified 增量封装为单 chunk JSON；结束发送 `[DONE]`。

## 7. 错误格式统一策略
`GlobalExceptionHandler` 根据路径前缀输出：
- Anthropic: `{"type":"error","error":{"type":"provider_error","message":"..."}}`
- OpenAI: `{"error":{"type":"provider_error","message":"..."}}`
支持的错误类别：`provider_error`, `transform_error`, `serialization_error`, `illegal_argument`, `internal_error`。

## 8. OpenAIController 迁移说明
旧逻辑: 直接调用 `forwardChatCompletion` / `streamChatCompletion`。
新逻辑: 使用 `TransformService.getVendorTransform("OpenAI")` 将 `ChatCompletionRequest` 转为 `UnifiedChatRequest`，通过 `forwardingService.(stream)Unified` 处理，再用同 transform 的 `unifiedToVendorResponse` 生成 OpenAI 兼容响应。
后续清理计划：
1. 已移除便捷方法 `openAIToUnified/unifiedToOpenAI`。
2. 后续：标记并删除遗留未使用的文档/代码引用，确保所有入口都走统一策略分发。

## 9. 代码清理与后续计划
短期 (S):
- 添加单元测试：路由 (decideProviderByModel)、OpenAI & Anthropic 流式事件映射、错误格式。
- 抽取 OpenAI 流式 JSON chunk 构造为工具方法减少重复。

中期 (M):
- Gemini 原生接口接入；
- 多模态内容统一结构 (支持 image/audio/tool)；
- 统一工具调用抽象 (tool_calls / tool_use)；

长期 (L):
- 细粒度配额 / 限流策略（基于 provider + model）;
- Observability：链路日志 + 统一埋点 + metrics (Prometheus)。

## 10. 统一模型快速参考 (v2 扩展)
`UnifiedChatRequest`
```
model,
messages[]: role,id,contents[],toolCalls[],reasoningContent,metadata,vendorExtras
stream,maxTokens,temperature,topP,topK,minP,frequencyPenalty,presencePenalty,seed,
stop[],tools[],toolChoice,logprobs,topLogprobs,responseFormat,thinking,
metadata (trace/user/session),vendorExtras (openai.*,anthropic.*)
```
`UnifiedChatResponse`
```
id,model,content,assistantMessage,messages[],toolCalls[],reasoningContent,
finishReason,usage{promptTokens,completionTokens,totalTokens,extra{}},
warnings[],metadata{},rawVendor
```
`UnifiedStreamChunk`
```
messageId,role,contentDelta,reasoningDelta,contentParts[],toolCallDeltas[],
usage (仅结束),finished,rawVendor
```

增量工具调用 (`UnifiedToolCallDelta`): id,name,argumentsDelta,argumentsJson,error,errorMessage,finished

多模态内容 (`UnifiedContentPart`):
```
type(text|image|audio|video|file|json|tool_result|reasoning|other),text,url,detail,
format,data,mimeType,json,sizeBytes,width,height,durationSeconds,
toolCallId,segmentId,segmentIndex,finalSegment,metadata{}
```

## 11. 已知限制 / 待补强
- OpenAI ToolCall 现模型差异（function/custom 嵌套）已做适配，但后续考虑重写成统一内部结构再降级输出。
- Anthropic streaming 仅提取文本 delta，未覆盖 tool_use / thinking（需模型升级后适配）。
- contentParts 流增量目前 OpenAI 解析仅落文本；图像/音频增量待厂商事件示例后实现。
- backpressure/大型多模态分片缓存策略未细化（需评估内存与延迟）。
- 缺少单元/集成测试覆盖新字段序列化与回放。

---
如需新增厂商：只需新增 DTO + Transform 分支 + Controller（或统一 ProviderController）+ providers.yml 配置 + AdapterFactory 分支。

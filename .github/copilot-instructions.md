# Robella AI 代理协作指令（面向自动化编码代理）

聚焦本仓库的真实实现（WebFlux 响应式 + 多厂商聚合）。按此文档操作可快速迭代而不破坏既有约定。

## 1. 架构速览
数据流: `OpenAI 兼容请求 → ForwardingService → RoutingService(模型→provider) → AdapterFactory( type→Adapter ) → AIProviderAdapter → 厂商 HTTP → TransformService 统一模型响应`

核心服务 (`service/`):
- ForwardingServiceImpl: 统一/流式转发入口 (`forwardUnified` / `streamUnified`)，内部决定 provider 并调用适配器。
- RoutingServiceImpl: 模型名匹配 provider；维护适配器缓存 + 模型列表原子缓存 (启动 @PostConstruct 构建)。
- TransformServiceImpl: 通过 `VendorTransformRegistry` 按 provider type(字符串) 取具体 `VendorTransform` 实现，完成 Unified <-> Vendor 互转与流事件映射。

## 2. Provider / Adapter 机制
定义：`adapter/AIProviderAdapter` 接口；工厂：`AdapterFactory#createAdapter(providerName, ProviderConfig.Provider)` 依据 `ProviderConfig.Provider.getProviderType()` (枚举 ProviderType) 分支。
内置实现：
- OpenAIAdapter: 处理 OpenAI 及 OpenAI 兼容 (DeepSeek、ModelScope、AIHubMix、AzureOpenAI)。Azure 通过 `deploymentName` 拼 URL `/deployments/{name}/chat/completions`。
- AnthropicAdapter: 对接 Messages API (`/messages`)，支持流式 SSE。
扩展：新增枚举值 -> 实现适配器 -> 更新 switch 分支。

约束：任何逻辑决策依赖 provider 的 `type` / `ProviderType`，不要硬编码 provider 名称 (除 AnthropicAdapter 内部使用固定 key "claude" 获取配置)。

## 3. 配置与模型路由
文件：`resources/providers.yml` -> 绑定 `ProviderConfig`。字段：`name,type,api-key,base-url,deploymentName,models[] (name,vendorModel)`。
路由策略：`RoutingServiceImpl#decideProviderByModel` 线性扫描 providers.models 匹配 `request.model`，未命中默认 `openai`。
模型列表缓存：`RoutingServiceImpl` 启动时构建；刷新调用 `refreshModelCache()`。

安全注意：当前示例配置含明文 api-key，提交前需改为环境变量占位（代理在 PR 可自动检查是否遗留明文）。

## 4. 统一模型 (Unified*) 转换
统一请求/响应：`model/internal/UnifiedChatRequest / UnifiedChatResponse / UnifiedStreamChunk`。
TransformServiceImpl 仅路由到 `VendorTransform`；具体转换在 `service/transform/*VendorTransform`. 若新增 provider type，需：
1. 实现 `VendorTransform` 并注册到 `VendorTransformRegistry`。
2. 确保 provider.yml 中 type 与注册 key 一致。
3. 考虑流事件裁剪：`streamUnified` 中最后会 `filter(ch -> contentDelta!=null || finished)`。

## 5. 流式处理要点
- OpenAIAdapter: 设置 `request.setStream(true)`，接收 `text/event-stream` 字符串片段。
- AnthropicAdapter: 同样 Accept: `text/event-stream`，逐条字符串事件。
- Transform: 负责把供应商 SSE 解析成 `UnifiedStreamChunk`；只返回非空增量或结束信号。

## 6. 错误处理约定
所有下游 HTTP 异常包装为 `ProviderException`（在各 Adapter `onErrorMap` 中），消息包含 HTTP 状态与响应体片段；上层无需再次包装，保持栈追踪。

## 7. 新增模型 vs 新增厂商步骤
仅新增模型(同一 provider)：编辑 `providers.yml` 对应 provider.models -> 刷新缓存 (`ForwardingService.refreshModelCache`)。
新增兼容厂商(type 已存在)：新增 provider 节点 (type 使用现有 `OpenAI` 或 `Anthropic`) -> 无需代码改动。
新增全新协议(type 不存在)：
1. enum ProviderType 添加条目 + 解析逻辑 (`fromString`).
2. 实现新的 Adapter。
3. AdapterFactory switch 增加分支。
4. 实现对应 `VendorTransform` 并注册。
5. providers.yml 增加新 provider (type=新枚举名)。

## 8. 性能/缓存/并发
- 适配器实例缓存：`RoutingServiceImpl.adapterCache` (ConcurrentHashMap)。
- 模型列表 AtomicReference，避免每次聚合。
- 流超时：OpenAI 流使用读超时 *5；Anthropic 同样策略 (`webClientProperties.getTimeout()`)。

## 9. 常用开发命令
构建: `mvn clean package`
运行: `java -jar target/robella-1.0.0.jar`
健康: `GET /actuator/health`
模型列表: `GET /v1/models`
聊天: `POST /v1/chat/completions` (body 含 model/messages/stream)

## 10. 代码修改指引（Agent 需遵守）
- 不在业务代码硬编码 API Key；若发现明文 key，改为 `${ENV_VAR}` 占位并更新 README 说明。
- 添加新 provider 时保持 `ProviderConfig.Provider.type` 与 `ProviderType` 枚举一致。
- 统一转换逻辑放在新 `VendorTransform`，不要把解析散落在 Adapter。
- 保留现有日志级别与格式，新增日志使用 `log.debug` / `log.trace` 以免噪音。

## 11. 关键文件索引
`service/ForwardingServiceImpl` 请求调度与流过滤
`service/RoutingServiceImpl` provider / 模型缓存与匹配
`service/TransformServiceImpl` 与 `service/transform/*` 统一模型转换
`adapter/*Adapter` 具体 HTTP 调用 & 异常包装
`config/ProviderConfig` + `resources/providers.yml` 配置入口

## 12. 快速检查清单 (自测前)
- ProviderType 分支完整？
- providers.yml 新增条目是否含 name/type/base-url/models？
- 无明文私密信息？
- 新增流处理是否产生空 chunk？

# Robella AI 代理协作指令（面向自动化编码代理）

Robella 是基于 Spring Boot WebFlux 的响应式 AI API 聚合网关，将多家厂商（OpenAI、Anthropic、Gemini 等）统一为 OpenAI 兼容接口。

## 1. 架构核心概念
**数据流**: `HTTP 请求 → Controller → ForwardingService → RoutingService(模型路由) → AIProviderAdapter → 厂商 API → TransformService(响应转换) → 统一格式`

**关键设计理念**:
- **端点分离**: 请求端点格式（如 OpenAI）独立于后端 provider 类型，实现灵活路由
- **统一转换层**: `UnifiedChatRequest/Response/StreamChunk` 作为内部中间格式，所有厂商适配器都转换到此格式
- **响应式流处理**: 使用 WebFlux `Mono/Flux` 处理同步/流式响应，避免阻塞

**核心服务** (`service/`):
- `ForwardingServiceImpl`: 请求调度入口，处理模型映射、流式响应过滤
- `RoutingServiceImpl`: 模型→provider 路由决策，适配器实例缓存（ConcurrentHashMap），模型列表原子缓存（AtomicReference，@PostConstruct 初始化）
- `TransformServiceImpl`: 通过 `VendorTransformRegistry` 分发到具体 `VendorTransform`，处理 Unified ↔ Vendor 双向转换

## 2. Provider / Adapter 机制
**接口**: `AIProviderAdapter` 定义通用契约（`chatCompletion()`, `streamChatCompletion()`）
**工厂**: `AdapterFactory.createAdapter()` 基于 `ProviderType` 枚举 switch 分支创建实例
**内置适配器**:
- `OpenAIAdapter`: 处理 OpenAI 及兼容厂商（DeepSeek、ModelScope、AIHubMix）。Azure 通过 `deploymentName` 特殊处理：URL 拼接 `/deployments/{name}/chat/completions`
- `AnthropicAdapter`: 对接 Messages API (`/messages`)，处理 SSE 流式响应，固定使用 "claude" 配置 key

**扩展新厂商**: 添加 ProviderType 枚举 → 实现 AIProviderAdapter → 更新 AdapterFactory switch → 实现对应 VendorTransform → 注册到 Registry

**重要约定**: 所有逻辑判断基于 `provider.getProviderType()` 而非 provider 名称硬编码

## 3. 配置与模型路由
**配置文件**: `application.yml` 中 `providers:` 节点 → 绑定到 `ProviderConfig`
**字段结构**: `name, type, api-key, base-url, deploymentName?, models[]{name, vendor-model}`
**路由策略**: `RoutingServiceImpl.decideProviderByModel()` 线性扫描所有 providers.models 匹配请求的 model，未命中则默认 "openai"
**模型缓存**: `@PostConstruct` 时构建 `AtomicReference<ModelListResponse>` 缓存，调用 `refreshModelCache()` 可重新加载

**环境变量**: 配置中 API Key 使用 `${ENV_VAR}` 占位符，如 `${OPENAI_API_KEY}`，运行前需设置对应环境变量

## 4. 统一模型 (Unified*) 转换
**统一格式**: `model/internal/UnifiedChatRequest / UnifiedChatResponse / UnifiedStreamChunk` 作为内部中间表示
**转换分发**: `TransformServiceImpl` 仅负责路由到具体 `VendorTransform`；实际转换逻辑在 `service/transform/*VendorTransform`
**注册机制**: `VendorTransformRegistry` 手动注册各厂商转换器，按 providerType 字符串查找

**新增厂商转换**:
1. 实现 `VendorTransform` 接口并在 `VendorTransformRegistry` 中注册
2. 确保 `application.yml` 中 provider.type 与注册的 key 一致
3. 实现双向转换：`vendorRequestToUnified` / `unifiedToVendorRequest` / `vendorResponseToUnified` / `unifiedToVendorResponse`
4. 流式转换：`vendorStreamEventToUnified` / `unifiedStreamChunkToVendor`

**关键设计**: 端点格式与实际调用 provider 分离，支持 OpenAI 端点调用 Anthropic 后端

## 5. 流式处理要点
**WebFlux响应式**: 使用 `Flux<UnifiedStreamChunk>` 处理流式响应，支持背压和非阻塞
**适配器实现**:
- `OpenAIAdapter`: 设置 `request.setStream(true)`，接收 `text/event-stream` 字符串片段
- `AnthropicAdapter`: Accept: `text/event-stream`，逐条解析 SSE 事件

**转换流程**: 厂商 SSE 事件 → `VendorTransform.vendorStreamEventToUnified()` → `UnifiedStreamChunk` → `unifiedStreamChunkToEndpoint()` → 端点格式
**过滤机制**: `ForwardingService.streamUnified()` 最后过滤掉空增量，只返回有内容变化或结束信号的 chunk
**流结束**: OpenAI 端点返回 `[DONE]` 标记，Controller 层通过 `concatWith(Flux.just("[DONE]"))` 添加

## 6. 错误处理约定
**统一异常**: 所有下游 HTTP 异常包装为 `ProviderException`（在各 Adapter `onErrorMap` 中），消息包含 HTTP 状态与响应体片段
**异常传播**: 上层无需再次包装，保持栈追踪完整性
**WebFlux错误处理**: 使用 `onErrorMap` 和 `onErrorResume` 进行响应式错误处理

## 7. 新增模型 vs 新增厂商步骤
**仅新增模型**(同一 provider)：编辑 `application.yml` 对应 provider.models → 刷新缓存 (`ForwardingService.refreshModelCache`)
**新增兼容厂商**(type 已存在)：新增 provider 节点 (type 使用现有 `OpenAI` 或 `Anthropic`) → 无需代码改动
**新增全新协议**(type 不存在)：
1. enum ProviderType 添加条目 + 解析逻辑 (`fromString`)
2. 实现新的 Adapter
3. AdapterFactory switch 增加分支
4. 实现对应 `VendorTransform` 并注册
5. application.yml 增加新 provider (type=新枚举名)

## 8. 性能/缓存/并发
**适配器缓存**: `RoutingServiceImpl.adapterCache` (ConcurrentHashMap) 缓存适配器实例
**模型列表缓存**: AtomicReference 保证线程安全，@PostConstruct 初始化，避免每次重新聚合
**连接池**: WebClient 使用 Reactor Netty 连接池，配置在 `robella.webclient.connection-pool`
**流超时**: 读超时配置 `robella.webclient.timeout.read`，默认 60s

## 9. 常用开发命令
**构建**: `mvn clean package`
**运行**: `java -jar target/robella-1.0.0.jar`
**测试**: `mvn test` (流式转换测试在 `service/transform/*Test.java`)
**健康检查**: `GET /actuator/health`
**模型列表**: `GET /v1/models`
**聊天**: `POST /v1/chat/completions` (body 含 model/messages/stream)

## 10. 代码修改指引（Agent 需遵守）
- **安全原则**: 不在业务代码硬编码 API Key；若发现明文 key，改为 `${ENV_VAR}` 占位并更新 README 说明
- **类型一致性**: 添加新 provider 时保持 `ProviderConfig.Provider.type` 与 `ProviderType` 枚举一致
- **转换分离**: 统一转换逻辑放在对应 `VendorTransform`，不要把解析逻辑散落在 Adapter 中
- **日志约定**: 保留现有日志级别与格式，新增日志使用 `log.debug` / `log.trace` 以免产生噪音
- **响应式编程**: 使用 WebFlux 的 `Mono/Flux`，避免阻塞操作，善用 `map/flatMap/filter`
- **错误处理**: 在适配器层使用 `onErrorMap` 包装为 `ProviderException`，上层不重复包装

## 11. 关键文件索引
**服务层**: `service/ForwardingServiceImpl` (请求调度)，`service/RoutingServiceImpl` (路由缓存)，`service/TransformServiceImpl` (转换分发)
**转换层**: `service/transform/*VendorTransform` (具体转换逻辑)，`VendorTransformRegistry` (注册中心)
**适配器**: `adapter/*Adapter` (HTTP 调用)，`AdapterFactory` (工厂创建)
**配置**: `config/ProviderConfig` + `application.yml` (厂商配置)，`config/ProviderType` (枚举定义)
**控制器**: `controller/OpenAIController` (OpenAI 兼容接口)，`controller/AnthropicController` (Anthropic 接口)

## 12. 快速检查清单 (自测前)
- [ ] ProviderType 分支是否完整 (AdapterFactory, VendorTransformRegistry)？
- [ ] application.yml 新增条目是否包含 name/type/base-url/models？
- [ ] 是否使用环境变量占位符而非明文 API Key？
- [ ] 新增流处理是否过滤空 chunk，避免无意义输出？
- [ ] 错误处理是否使用 WebFlux 响应式模式？

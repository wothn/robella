# Robella AI Gateway - 架构文档

## 构建与开发
**构建**: `mvn clean package`
**运行**: `java -jar target/robella-1.0.0.jar`

## 架构概览
Robella是基于Spring Boot WebFlux的响应式AI API聚合网关，统一多个AI提供商(OpenAI、Anthropic、Gemini等)的标准化API接口。

### 核心架构
**数据流**: `HTTP请求 → Controller → ForwardingService → RoutingService → AIApiClient → 厂商API → TransformService`

**核心服务**:
- `ForwardingServiceImpl` - 请求分发入口，处理模型映射和流响应过滤
- `RoutingServiceImpl` - 模型→提供商路由决策，适配器实例缓存
- `TransformServiceImpl` - 通过VendorTransformRegistry路由到特定厂商转换器

**设计原则**:
- 端点格式分离(OpenAI格式独立于后端提供商类型)
- 统一转换层使用`UnifiedChatRequest/Response/StreamChunk`作为内部中间格式
- 使用WebFlux `Mono/Flux`进行响应式流处理

### 提供商/适配器系统
- **接口**: `AIApiClient`定义契约(`chatCompletion()`, `streamChatCompletion()`)
- **工厂**: `ClientFactory.createClient()`基于`ProviderType`枚举创建实例
- **内置适配器**:
  - `OpenAIClient`: 处理OpenAI及兼容提供商(DeepSeek、ModelScope、AIHubMix)
  - `AnthropicClient`: Anthropic Messages API，处理SSE流式传输

### 配置与路由
- **配置文件**: `application.yml`中的`providers:`部分绑定到`ProviderConfig`
- **提供商结构**: `name, type, api-key, base-url, deploymentName?, models[]{name, vendor-model}`
- **路由策略**: `RoutingServiceImpl.decideProviderByModel()`线性扫描所有提供商模型进行匹配
- **环境变量**: API密钥使用`${ENV_VAR}`占位符

### 错误处理
- 所有下游HTTP异常包装为`ProviderException`
- 使用WebFlux `onErrorMap`和`onErrorResume`进行响应式错误处理

## 关键特性
1. **统一内部格式**: 所有提供商数据转换为统一格式进行内部处理
3. **状态转换**: 当端点家族不同时，状态转换器维护会话状态
4. **响应式处理**: 充分利用Reactor的`Flux`进行流处理
5. **灵活路由**: 基于模型名称的提供商选择，支持模型名称映射

## 关键文件
- **服务**: `service/ForwardingServiceImpl`, `service/RoutingServiceImpl`, `service/TransformServiceImpl`
- **转换器**: `service/transform/*VendorTransform`, `VendorTransformRegistry`
- **客户端**: `client/*Client`, `ClientFactory`
- **配置**: `config/ProviderConfig`, `config/ProviderType`, `application.yml`
- **控制器**: `controller/OpenAIController`, `controller/AnthropicController`

## 开发指南
- **安全**: 永不硬编码API密钥，使用环境变量占位符
- **类型一致性**: 确保配置类型与枚举值匹配
- **响应式编程**: 使用WebFlux `Mono/Flux`，避免阻塞操作
- **错误处理**: 在适配器中使用`onErrorMap`包装异常
- **日志**: 使用`log.debug`/`log.trace`避免日志噪音
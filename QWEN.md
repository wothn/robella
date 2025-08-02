# Robella AI 代理开发指南

## 架构总览

Robella 是一个**响应式 AI 代理**，将多家 AI 服务商（OpenAI、Claude、Gemini、Qwen）聚合在统一的 OpenAI 兼容 API 之下。基于 **Spring Boot 3 + WebFlux** 实现非阻塞 I/O。

### 核心服务链
```
Request → Transform → Route → Adapt → Response
```

核心服务位于 `src/main/java/org/elmo/robella/service/`：
- **ForwardingService**：请求流转的总入口
- **TransformService**：OpenAI 格式与各厂商格式的转换
- **RoutingService**：选择目标厂商并返回对应适配器

## 厂商适配器模式

**目录**：`src/main/java/org/elmo/robella/adapter/`

所有厂商适配器实现 `AIProviderAdapter` 接口。由 `AdapterFactory` 根据配置的 `type` 字段创建：
- `OpenAI` → `OpenAICompatibleAdapter`（也处理 DeepSeek、OpenRouter、Azure）
- `Anthropic` → `ClaudeAdapter`
- `Gemini` → `GeminiAdapter`

**注意**：始终用 type 字段分发，不要用 provider 名称判断。

## 配置系统

**厂商配置**：`src/main/resources/providers.yml`
```yaml
providers:
  openai:
    type: OpenAI           # 由 AdapterFactory 选择适配器
    api-key: ${ENV_VAR}    # 密钥务必用环境变量
    base-url: https://...
    models:
      - name: gpt-3.5-turbo          # 对外模型名
        vendor-model: gpt-3.5-turbo  # 厂商内部名
```

**关键**：适配器选择只看 type 字段，与 provider key 无关。

## 响应式模式

**流式与非流式**：`OpenAIController.chatCompletions()` 根据 `request.getStream()` 分支：
- 流式：返回 `Flux<ServerSentEvent<String>>`
- 非流式：返回 `Mono<ResponseEntity<?>>`

**错误处理**：所有厂商异常统一封装为 `ProviderException`，并带上下文。

## 开发工作流

### 新增厂商流程
1. 在 `providers.yml` 增加配置，type 必须正确
2. 新 type：实现新的 `AIProviderAdapter`
3. 更新 `AdapterFactory.createAdapter()` 分支
4. 在 `TransformService` 实现请求/响应转换

### 测试
- 构建：`mvn clean package`
- 本地运行：`java -jar target/robella-1.0.0.jar`
- 健康检查：`curl http://localhost:8080/actuator/health`

### 环境变量
所有密钥需用环境变量（见 `providers.yml`）：
```bash
OPENAI_API_KEY, CLAUDE_API_KEY, GEMINI_API_KEY, QWEN_API_KEY
```

## 请求流示例

1. **POST /v1/chat/completions** → `OpenAIController.chatCompletions()`
2. **Route**：模型名 → 厂商选择
3. **Adapt**：`OpenAIChatRequest` → 各厂商格式（Claude、Gemini 等）
4. **Call**：HTTP 请求实际 AI 厂商
5. **Transform**：厂商响应 → `OpenAIChatResponse`

## 关键文件
- `OpenAIController.java`：入口，流式/非流式分支
- `ForwardingServiceImpl.java`：主流程编排
- `AdapterFactory.java`：type → 适配器映射
- `ProviderConfig.java`：配置结构绑定
- `providers.yml`：运行时厂商配置

## 调试技巧
- 日志见 `logs/robella.log`，可追踪厂商调用
- 详细链路日志：`logging.level.org.elmo.robella: DEBUG`

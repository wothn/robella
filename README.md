# Robella - OpenAI API兼容转发平台

Robella是一个OpenAI API兼容的转发平台，可以统一接入多家AI服务商（OpenAI、Claude、Gemini、通义千问等），提供标准化的API接口和灵活的路由策略。

## 功能特性

- OpenAI API格式兼容
- 多AI服务商转发支持
- 流式传输处理
- 动态路由配置
- 监控统计功能

## 技术栈

- Java 17+
- Spring Boot 3.x
- Spring WebFlux (响应式编程)
- Maven 3.8+
- Redis 7.x
- Micrometer + Prometheus

## 快速开始

### 环境要求

- Java 17+
- Maven 3.8+
- Redis 7.x

### 构建项目

```bash
mvn clean package
```

### 运行应用

```bash
java -jar target/robella-1.0.0.jar
```

### 配置环境变量

在运行应用前，请设置以下环境变量：

```bash
export OPENAI_API_KEY="your-openai-api-key"
export CLAUDE_API_KEY="your-claude-api-key"
export GEMINI_API_KEY="your-gemini-api-key"
export QWEN_API_KEY="your-qwen-api-key"
```

## API接口

### 聊天完成

```http
POST /v1/chat/completions
Content-Type: application/json

{
  "model": "gpt-3.5-turbo",
  "messages": [
    {
      "role": "user",
      "content": "Hello!"
    }
  ],
  "stream": false
}
```

### 模型列表

```http
GET /v1/models
```

## 配置说明

配置文件位于 `src/main/resources` 目录下：

- `application.yml`: 主配置文件
- `providers.yml`: AI服务商配置文件

## 监控

应用集成了Micrometer和Prometheus，可以通过 `/actuator/prometheus` 端点获取监控指标。

## 许可证

[MIT License](LICENSE)
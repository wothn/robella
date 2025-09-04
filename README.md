# Robella - OpenAI API兼容转发平台

Robella是一AI API的转发平台，可以统一接入多家AI服务商（OpenAI、Claude、Gemini、通义千问等），提供标准化的API接口和灵活的路由策略。

## 功能特性

- OpenAI API端点
- Anthropic API端点
- 多AI服务商转发支持
- 流式传输处理
- 动态路由配置

## 技术栈

- Java 17+
- Spring Boot 3.x
- Spring WebFlux (响应式编程)
- Maven 3.8+

## 快速开始

### 环境要求

- Java 17+
- Maven 3.8+

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

## 前端开发

### 技术栈

- React 19.1.1
- TypeScript
- Vite
- Tailwind CSS
- Shadcn UI 组件库
- Radix UI 基础组件

### 开发环境

```bash
cd web
npm install
npm run dev
```

### 构建生产版本

```bash
cd web
npm run build
```

### 代码检查

```bash
cd web
npm run lint
```

### 项目结构

```
web/
├── src/
│   ├── components/     # React 组件
│   ├── lib/           # 工具函数和配置
│   ├── assets/        # 静态资源
│   ├── App.tsx        # 主应用组件
│   └── main.tsx       # 应用入口
├── dist/              # 构建输出目录
├── public/            # 公共资源
└── package.json       # 依赖配置
```

### 开发脚本

- `npm run dev` - 启动开发服务器
- `npm run build` - 构建生产版本
- `npm run lint` - 运行代码检查
- `npm run preview` - 预览生产构建

## 配置说明

配置文件位于 `src/main/resources` 目录下：

- `application.yml`: 主配置文件
- `providers.yml`: AI服务商配置文件

## 许可证

[MIT License](LICENSE)
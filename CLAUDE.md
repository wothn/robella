# CLAUDE.md

此文件为Claude Code (claude.ai/code) 在处理此存储库中的代码时提供指导。

## 项目概述

Robella是一个AI API网关，它提供对多个AI服务提供商（OpenAI、Anthropic/Claude、Qwen等）的统一访问。让用户能通过任意端点访问任意模型。

## 架构

### 应用
从应用层面来说，项目核心的ai被分为Provider、Model、VendorModel三层：
- **Provider**: 代表AI服务提供商（如OpenAI、Anthropic等），包含认证信息和API端点。
- **Model**: 代表AI模型（如gpt-4、claude-2等），包含能力（如推理能力，工具调用等）作为项目暴露给用户调用的模型。
- **VendorModel**: 连接Provider和Model，定义某个模型在某个提供商下的具体实现和定价。

模型请求进入系统后，经过路由服务（RoutingService）将用户请求的模型名称映射到具体的提供商模型名称，然后通过统一服务（UnifiedService）将请求转发给相应的提供商API。

其余部分包括：
用户管理
api密钥管理

### 后端 (Java Spring WebFlux)
首先：**不**使用Spring Security，仅导入spring-security-crypto进行密码编码
- **框架**: Spring Boot 3.2.5 配合 WebFlux 进行响应式编程
- **数据库**: PostgreSQL 配合 R2DBC 进行响应式数据库访问
- **认证**: 基于JWT的认证，支持GitHub OAuth
- **API兼容性**: OpenAI API兼容端点 (`/v1/chat/completions`, `/v1/models`) 和 Anthropic原生API (`/anthropic/v1/messages`)
- **关键组件**:
  - `OpenAIController.java:39` - 主要的OpenAI兼容API端点
  - `AnthropicController.java:52` - Anthropic API兼容层
  - `RoutingService.java:32` - 动态模型到提供商的路由
  - `UnifiedService.java:42` - 统一请求处理服务
  - `EndpointTransform.java` - 请求/响应转换的通用接口

### 前端 (React + TypeScript)
- **框架**: React 18.3.1 配合 TypeScript
- **构建工具**: Vite
- **UI库**: Shadcn UI组件配合Radix UI基础组件
- **样式**: Tailwind CSS
- **主要功能**:
  - 用户认证 (JWT + GitHub OAuth)
  - 提供商管理界面
  - 模型配置和路由
  - 包含使用情况分析的仪表板

### 数据库模式
- **Users**: 认证和角色管理
- **Providers**: AI服务提供商配置
- **Models**: 可用的AI模型及其能力和定价
- **VendorModels**: 模型与提供商之间的映射

## 开发命令

### 后端
```bash
# 构建项目
mvn clean package

# 运行应用程序
java -jar target/robella-1.0.0.jar

# 开发模式
mvn spring-boot:run

# 仅编译
mvn compile

# 测试 (当测试实现后)
mvn test
```

### 前端
```bash
# 进入web目录
cd web

# 安装依赖
npm install

# 开发服务器
npm run dev

# 构建生产环境
npm run build

# 代码检测
npm run lint

# 类型检查
tsc --noEmit

# 预览生产构建
npm run preview
```

## 配置

### 关键配置文件
- `src/main/resources/application.yml` - 主应用程序配置
- `src/main/resources/schema.sql` - 数据库模式定义
- `web/vite.config.ts` - 前端构建配置，包含到后端的代理


## 关键模式

### 请求流程
1. 请求到达OpenAI兼容或Anthropic端点
2. `RoutingService` 将客户端模型名称映射到供应商模型名称
3. `EndpointTransform` 将请求转换为统一格式
4. `UnifiedService` 根据供应商模型选择适当的提供商
5. 通过 `ApiClient` 实现将请求转发给提供商
6. 响应转换回请求的格式

### 认证
- 带刷新令牌轮换的JWT令牌
- GitHub OAuth集成
- 基于角色的访问控制 (管理员/用户)

### 数据库访问
- R2DBC 用于响应式数据库操作
- 带有响应式返回类型 (Mono/Flux) 的存储库模式
- Flyway 已配置但已禁用迁移

### 转换架构
- 请求/响应转换的通用 `EndpointTransform<T, R>` 接口
- `OpenAITransform` 和 `AnthropicTransform` 实现
- 支持流式转换的实时响应

## 重要提示

- 应用程序默认运行在端口10032
- 整个项目使用WebFlux进行响应式编程
- 所有数据库操作都返回响应式类型 (Mono/Flux)
- 前端是 `/web` 目录下的独立React应用程序
- 提供商配置存储在数据库中，可通过UI进行管理
- 模型路由是动态的，可以在运行时配置
- 支持流式和非流式响应
- 系统同时支持OpenAI兼容和Anthropic原生API格式

## AI 开发哲学

**KISS原则 (Keep It Simple, Stupid)**
1. 简单优于复杂: 优先选择简单直接的解决方案
2. 可读性第一: 代码是给人看的，其次才是给机器执行的
3. 避免过度设计: 不要为了未来可能的需求而过度设计
4. 单一职责: 每个函数、类、模块只做一件事

**Linus精神**
1. 代码质量至上: 糟糕的代码是技术债务，好的代码是资产
2. 直接而诚实: 代码评审时直接指出问题，不要含糊其辞
3. 性能意识: 始终考虑代码的性能影响
# AGENTS.md

This file provides guidance to AI Agent when working with code in this repository.

## Project Overview

Robella is an AI API gateway that provides unified access to multiple AI service providers (OpenAI, Anthropic/Claude, Qwen, etc.). It allows users to access any model through any endpoint with flexible routing and load balancing.

## Architecture

### Application Layer
The AI system is structured in three layers:
- **Provider**: AI service providers (OpenAI, Anthropic, etc.) with authentication and API endpoints
- **Model**: AI models (gpt-4, claude-2, etc.) with capabilities exposed to users, can bind multiple VendorModels
- **VendorModel**: Connects Provider and Model, defining specific implementation and pricing for a model under a provider

Request flow: User request → RoutingService (maps model name to provider model) → UnifiedService → Provider API

Additional components include user management and API key management.

### Backend (Java Spring Boot)
- **Framework**: Spring Boot 3.3.10 with Java 21 and virtual threads
- **Database**: PostgreSQL with MyBatis-Plus
- **Authentication**: JWT with GitHub OAuth (Note: Only spring-security-crypto is used, not full Spring Security)
- **API Compatibility**: OpenAI API compatible endpoints (`/v1/chat/completions`, `/v1/models`) and Anthropic native API (`/anthropic/v1/messages`)
- **Key Components**:
  - `OpenAIController.java:42` - Main OpenAI compatible API endpoint
  - `AnthropicController.java:51` - Anthropic API compatibility layer
  - `RoutingService.java:32` - Dynamic model to provider routing
  - `UnifiedService.java:42` - Unified request processing
  - `EndpointTransform.java` - Generic interface for request/response transformation

### Frontend (React + TypeScript)
- **Framework**: React 18.3.1 with TypeScript
- **Build Tool**: Vite
- **UI**: Shadcn UI with Radix UI primitives
- **Styling**: Tailwind CSS
- **Key Features**:
  - User authentication (JWT + GitHub OAuth)
  - Provider management interface
  - Model configuration and routing
  - Dashboard with usage analytics

### Database Schema
- **Users**: Authentication and role management
- **Providers**: AI service provider configurations
- **Models**: Available AI models with capabilities and pricing
- **VendorModels**: Model to provider mappings
- **ApiKeys**: API key management with rate limiting
- **RequestLog**: Request logging and analytics

## Development Commands

### Backend
```bash
# Build project
mvn clean package

# Run application
java -jar target/robella-0.1.0.jar

# Development mode
mvn spring-boot:run

# Compile only
mvn compile

# Test (when implemented)
mvn test
```

### Frontend
```bash
# Enter web directory
cd web

# Install dependencies
npm install

# Development server
npm run dev

# Build production
npm run build

# Lint code
npm run lint

# Type check
tsc --noEmit

# Preview production build
npm run preview
```

## Configuration

### Key Configuration Files
- `src/main/resources/application.yml` - Main application configuration
- `src/main/resources/schema.sql` - Database schema definition
- `web/vite.config.ts` - Frontend build configuration with proxy to backend

### Environment Variables
- `POSTGRES_USERNAME` - Database username
- `POSTGRES_PASSWORD` - Database password
- `JWT_SECRET` - JWT signing secret
- `GITHUB_CLIENT_ID` - GitHub OAuth client ID
- `GITHUB_CLIENT_SECRET` - GitHub OAuth client secret

## Key Patterns

### Request Flow
1. Request arrives at OpenAI compatible or Anthropic endpoint
2. `RoutingService` maps client model name to vendor model name and load balancing
3. `EndpointTransform` converts request to unified format
4. `UnifiedService` selects appropriate provider based on vendor model
5. Forwards request to provider via `ApiClient`
6. Response converted back to requested format

### Authentication
- JWT tokens with refresh token rotation
- GitHub OAuth integration
- Role-based access control (admin/user)

### Database Access
- MyBatis-Plus for database access
- Flyway configured but migrations disabled
- Uses traditional blocking database access (not reactive)

### Transformation Architecture
- Generic `EndpointTransform<T, R>` interface for request/response transformation
- `OpenAIEndpointTransform` and `AnthropicEndpointTransform` implementations
- Support for real-time streaming transformation using virtual threads

## Important Notes

- Application runs on port 10032 by default
- Uses Spring MVC with virtual threads (not WebFlux)
- Frontend is a separate React app in `/web` directory
- Provider configurations stored in database, manageable via UI
- Model routing is dynamic and configurable at runtime
- Supports both streaming and non-streaming responses
- Compatible with both OpenAI and Anthropic native API formats
- Database schema includes comprehensive request logging for analytics
## AI 开发哲学

**KISS原则 (Keep It Simple, Stupid)**
1. 简单优于复杂: 优先选择简单直接的解决方案
2. 可读性第一: 代码是给人看的，其次才是给机器执行的
3. 避免过度设计: 不要为了未来可能的需求而过度设计
4. 单一职责: 每个函数、类、模块只做一件事

**设计原则**
1. 代码质量至上: 糟糕的代码是技术债务，好的代码是资产
2. 直接而诚实: 代码评审时直接指出问题，不要含糊其辞
3. 性能意识: 始终考虑代码的性能影响
4. 设计时保持克制，不要在一开始就追求完美和极致

**工具使用**
1. 使用`readFile`或类似获取文件内容的工具时，始终指定范围一次阅读 2000 行代码，以确保您有足够的上下文。
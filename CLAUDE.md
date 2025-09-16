# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Robella is an AI API gateway that provides unified access to multiple AI service providers (OpenAI, Anthropic/Claude, Gemini, Qwen, etc.). It offers standardized OpenAI-compatible API endpoints with flexible routing strategies and provider management.

## Architecture

### Backend (Java Spring WebFlux)
the first：there is **not** use spring security, onlt import spring-security-crypto for password encoding
- **Framework**: Spring Boot 3.2.5 with WebFlux for reactive programming
- **Database**: PostgreSQL with R2DBC for reactive database access
- **Authentication**: JWT-based authentication with GitHub OAuth support
- **API Compatibility**: OpenAI API compatible endpoints (`/v1/chat/completions`, `/v1/models`) and Anthropic native API (`/anthropic/v1/messages`)
- **Key Components**:
  - `OpenAIController.java:39` - Main OpenAI-compatible API endpoint
  - `AnthropicController.java:52` - Anthropic API compatibility layer
  - `RoutingService.java:32` - Dynamic model-to-provider routing
  - `UnifiedService.java:42` - Unified request handling service
  - `EndpointTransform.java` - Generic interface for request/response transformation

### Frontend (React + TypeScript)
- **Framework**: React 18.3.1 with TypeScript
- **Build Tool**: Vite
- **UI Library**: Shadcn UI components with Radix UI primitives
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
- **VendorModels**: Mapping between models and providers

## Development Commands

### Backend
```bash
# Build the project
mvn clean package

# Run the application
java -jar target/robella-1.0.0.jar

# Development mode
mvn spring-boot:run

# Compile only
mvn compile

# Test (when tests are implemented)
mvn test
```

### Frontend
```bash
# Navigate to web directory
cd web

# Install dependencies
npm install

# Development server
npm run dev

# Build for production
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

## API Endpoints

### OpenAI Compatible
- `POST /v1/chat/completions` - Chat completions (streaming supported)
- `GET /v1/models` - List available models

### Anthropic Native
- `POST /anthropic/v1/messages` - Anthropic messages API (streaming supported)
- `GET /anthropic/v1/models` - List available models in Anthropic format

### Authentication
- `POST /api/users/login` - User login
- `POST /api/users/register` - User registration
- `GET /api/users/me` - Get current user
- `POST /api/oauth/github/login` - GitHub OAuth initiation
- `GET /api/oauth/github/callback` - GitHub OAuth callback

### Management
- `GET /api/providers` - List providers
- `POST /api/providers` - Create provider
- `PUT /api/providers/{id}` - Update provider
- `GET /api/providers/{id}/models` - List provider models

## Key Patterns

### Request Flow
1. Request arrives at OpenAI-compatible or Anthropic endpoint
2. `RoutingService` maps client model name to vendor model name
3. `EndpointTransform` converts request to unified format
4. `UnifiedService` selects appropriate provider based on vendor model
5. Request forwarded to provider via `ApiClient` implementation
6. Response transformed back to requested format

### Authentication
- JWT tokens with refresh token rotation
- GitHub OAuth integration
- Role-based access control (Admin/User)

### Database Access
- R2DBC for reactive database operations
- Repository pattern with reactive return types (Mono/Flux)
- Flyway configured but disabled for migrations

### Transformation Architecture
- Generic `EndpointTransform<T, R>` interface for request/response conversion
- `OpenAITransform` and `AnthropicTransform` implementations
- Stream transformation support for real-time responses

## Important Notes

- The application runs on port 10032 by default
- WebFlux is used throughout for reactive programming
- All database operations return reactive types (Mono/Flux)
- The frontend is a separate React application in the `/web` directory
- Provider configurations are stored in the database and can be managed through the UI
- Model routing is dynamic and can be configured at runtime
- Both streaming and non-streaming responses are supported
- The system supports both OpenAI-compatible and Anthropic-native API formats

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


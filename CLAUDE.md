# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
- **Authentication**: sa-Token with GitHub OAuth (Note: Only spring-security-crypto is used, not full Spring Security)
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
- **state**: React state management with Zustand
- **UI**: Shadcn UI with Radix UI primitives
- **Styling**: Tailwind CSS
- **Key Features**:
  - User authentication (sa-Token + GitHub OAuth)
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
- Sa-Token framework
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

## Development Guidelines

### Code Structure
- Backend follows standard Spring Boot package structure
- Frontend uses feature-based organization in `src/app/` directory
- Shared UI components in `src/components/ui/`
- Custom hooks in `src/hooks/` for reusable logic

### Testing Strategy
- Backend: JUnit 5 with Spring Boot Test
- Frontend: Component testing with React Testing Library
- Integration tests for API endpoints
- Use `mvn test` for backend tests
- Use `npm run lint` for frontend code quality

### State Management
- Backend: Spring beans and service layer
- Frontend: Zustand stores for global state (`src/stores/`)
- Component state with React hooks
- API client configuration in `src/lib/api.ts`

## Important Notes

- Application runs on port 10032 by default
- Uses Spring MVC with virtual threads
- Frontend is a separate React app in `/web` directory
- Provider configurations stored in database, manageable via UI
- Model routing is dynamic and configurable at runtime
- Supports both streaming and non-streaming responses
- Compatible with both OpenAI and Anthropic native API formats
- Database schema includes comprehensive request logging for analytics
- Your knowledge is outdated, you are not aware of the latest libraries and knowledge. If you need to import new dependencies, you need to use web search or Context7 for confirmation.


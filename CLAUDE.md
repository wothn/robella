# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Robella is an AI API gateway that provides unified access to multiple AI service providers (OpenAI, Anthropic/Claude, Gemini, Qwen, etc.). It offers standardized OpenAI-compatible API endpoints with flexible routing strategies and provider management.

## Architecture

### Backend (Java Spring webflux)
- **Framework**: Spring Boot 3.2.5 with WebFlux for reactive programming
- **Database**: PostgreSQL with R2DBC for reactive database access
- **Authentication**: JWT-based authentication with GitHub OAuth support
- **API Compatibility**: OpenAI API compatible endpoints (`/v1/chat/completions`, `/v1/models`)
- **Key Components**:
  - `OpenAIController.java:27` - Main OpenAI-compatible API endpoint
  - `AnthropicController.java` - Anthropic API compatibility layer
  - `RoutingService.java:24` - Dynamic model-to-provider routing
  - `ForwardingService.java` - Request forwarding to AI providers
  - `VendorTransformFactory` - Request/response transformation between vendor formats

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

### Environment Variables
```bash
# Database
POSTGRES_USERNAME=postgres
POSTGRES_PASSWORD=your_password

# JWT
JWT_SECRET=your_jwt_secret_key

# GitHub OAuth
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
GITHUB_REDIRECT_URI=http://localhost:10032/api/oauth/github/callback

# AI Provider API Keys
OPENAI_API_KEY=your_openai_key
CLAUDE_API_KEY=your_claude_key
GEMINI_API_KEY=your_gemini_key
QWEN_API_KEY=your_qwen_key
```

### Key Configuration Files
- `src/main/resources/application.yml` - Main application configuration
- `src/main/resources/schema.sql` - Database schema definition
- `web/vite.config.ts` - Frontend build configuration

## API Endpoints

### OpenAI Compatible
- `POST /v1/chat/completions` - Chat completions (streaming supported)
- `GET /v1/models` - List available models

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
1. Request arrives at OpenAI-compatible endpoint
2. `VendorTransformFactory` converts to unified format
3. `RoutingService` selects appropriate provider based on model
4. `ForwardingService` forwards request to provider
5. Response transformed back to OpenAI format

### Authentication
- JWT tokens with refresh token rotation
- GitHub OAuth integration
- Role-based access control (Admin/User)

### Database Access
- R2DBC for reactive database operations
- Flyway for database migrations (currently disabled)
- Repository pattern with reactive return types (Mono/Flux)

## Important Notes

- The application runs on port 10032 by default
- WebFlux is used throughout for reactive programming
- All database operations return reactive types (Mono/Flux)
- The frontend is a separate React application in the `/web` directory
- Provider configurations are stored in the database and can be managed through the UI
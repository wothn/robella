# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Backend (Java/Spring Boot)
**Build**: `mvn clean package`
**Test**: `mvn compile`
**Run**: `java -jar target/robella-1.0.0.jar`
**Dev Mode**: `mvn spring-boot:run`

### Frontend (React/TypeScript)
**Install Dependencies**: `cd web && npm install`
**Dev Server**: `cd web && npm run dev`
**Build**: `cd web && npm run build`
**Lint**: `cd web && npm run lint`

## Architecture Overview

Robella is a Spring Boot WebFlux reactive AI API aggregation gateway that unifies multiple AI providers (OpenAI, Anthropic, etc.) into standardized API interfaces with advanced streaming capabilities and user management features.


**Database Layer**:
- PostgreSQL with R2DBC reactive driver
- Connection pooling with `r2dbc-pool`
- Schema initialization via `schema.sql` and test data via `data.sql`
- User entity with reactive repository pattern


### Database & User Management
- **Reactive Database**: PostgreSQL with R2DBC driver for non-blocking database access
- **User Entity**: Complete user model with authentication, profile, and verification fields
- **Repository Pattern**: Reactive repository with `Flux`/`Mono` return types
- **Connection Pool**: Configured pool with 5-20 connections, 30m idle timeout, 60m lifetime
- **Initialization**: Automatic schema creation and test data insertion via Spring SQL initialization




### Frontend (React/TypeScript)
- **Components**: `web/src/components/` (React components)
- **Main App**: `web/src/App.tsx` (main application component)
- **Entry Point**: `web/src/main.tsx` (application entry)
- **Configuration**: `web/package.json` (dependencies), `web/vite.config.ts` (build config), `web/tailwind.config.js` (styling)


## Development Guidelines
### Backend (Java)
- **Reactive Programming**: Always use WebFlux `Mono/Flux`, avoid blocking operations
- **Stream Session Management**: Each stream transformation requires unique session ID
- **Error Handling**: Wrap exceptions as `ProviderException`, use reactive error operators
- **Type Safety**: Ensure consistent `ProviderType` enum values across config and factory switches
- **Security**: Never hardcode API keys, use `${ENV_VAR}` placeholders
- **Testing**: Use JUnit 5 with Reactor Test for reactive stream testing
- **Logging**: Use `log.debug`/`log.trace` levels, especially for stream state transitions
- **Database Access**: Use R2DBC reactive repositories, avoid blocking JDBC operations

### Frontend (React/TypeScript)
- **Component Structure**: Use functional components with hooks
- **TypeScript**: Always define types for props and state
- **State Management**: Use React hooks (useState, useEffect) for local state
- **Code Quality**: Run ESLint before commits
- **Build**: Use Vite for fast development and optimized builds

## Database Configuration
- **Connection**: `r2dbc:postgresql://localhost:5432/robella`
- **Credentials**: Environment variables `POSTGRES_USERNAME` and `POSTGRES_PASSWORD`
- **Initialization**: Automatic schema creation via `schema.sql` and test data via `data.sql`
- **Pool Settings**: 5 initial connections, 20 max, 30m idle timeout, 60m lifetime

## Debugging Stream Processing
- **Session Tracking**: Each stream has UUID session ID in logs
- **Transform Layers**: Monitor both vendor→unified and unified→endpoint conversions
- **Empty Chunk Filtering**: Check for null/empty chunks being filtered in `ForwardingService`
- **WebFlux Debugging**: Enable `reactor.netty.http.client: DEBUG` for network-level issues
- **Stream State**: Monitor session-based state management in transformers
- **Database Debugging**: Enable R2DBC logging for database query tracking
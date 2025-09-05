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

### Key Architecture Components

**Data Flow**: `HTTP Request → Controller → ForwardingService → RoutingService → ApiClient → Vendor API → Dual-Layer Stream Transform → Response`

**Core Services**:
- `ForwardingServiceImpl` - Request dispatch with model name mapping (`mapModelName()`) and dual-layer stream orchestration
- `RoutingServiceImpl` - Provider routing decisions, client instance caching, model list management
- `TransformServiceImpl` - Delegates to VendorTransform via `VendorTransformRegistry`
- `StreamTransformerFactory` - Factory for dual-layer stream transformers with session management
- `UserService` - User management with PostgreSQL R2DBC reactive database access

**Database Layer**:
- PostgreSQL with R2DBC reactive driver
- Connection pooling with `r2dbc-pool`
- Schema initialization via `schema.sql` and test data via `data.sql`
- User entity with reactive repository pattern

### Dual-Layer Stream Processing Architecture

**Stream Transformation Flow**:
1. **Vendor → Unified**: `StreamToUnifiedTransformer` converts vendor SSE events to `UnifiedStreamChunk`
2. **Unified → Endpoint**: `UnifiedToEndpointTransformer` converts to client-expected format (OpenAI/Anthropic)
3. **Session Management**: UUID-based session tracking for stateful stream transformations

**Stream Components**:
- `StreamTransformerFactory`: Factory providing transformer instances by `ProviderType`
- Session-based state management using `UUID.randomUUID().toString()`
- Filter empty chunks, return only content changes or end signals
- OpenAI endpoints append `[DONE]` marker via `concatWith(Flux.just("[DONE]"))`

### Provider/Adapter System
- **Interface**: `ApiClient` contract (`chatCompletion()`, `streamChatCompletion()`)
- **Factory**: `ClientFactory.createClient()` by `ProviderType` enum
- **Built-in Adapters**:
    - `OpenAIClient`: OpenAI and compatible (DeepSeek, ModelScope, AIHubMix). Azure via `deploymentName` with URL pattern `/deployments/{name}/chat/completions`
    - `AnthropicClient`: Anthropic Messages API (`/messages`), SSE streaming

### Configuration & Routing
- **Config File**: `application.yml` with `providers:` section → `ProviderConfig`
- **Provider Structure**: `name, type, api-key, base-url, deploymentName?, models[]{name, vendor-model}`
- **Model Mapping**: `ForwardingServiceImpl.mapModelName()` maps client model names to vendor models via `ConfigUtils.getModelMapping()`
- **Routing Strategy**: `RoutingServiceImpl.decideProviderByModel()` linear scan, defaults to "openai"
- **Environment Variables**: API keys use `${ENV_VAR}` (e.g., `${OPENAI_API_KEY}`, `${ANTHROPIC_API_KEY}`)

### Unified Model Conversion
- **Format**: `model/internal/UnifiedChatRequest/Response/StreamChunk` as internal representation
- **Registry**: `VendorTransformRegistry` manually registers transformers in constructor
- **Transformers**: `service/transform/*VendorTransform` handle bidirectional conversions
- **Stream Chunk**: `UnifiedStreamChunk` uses OpenAI `Choice` and `Usage` structures for compatibility

### Database & User Management
- **Reactive Database**: PostgreSQL with R2DBC driver for non-blocking database access
- **User Entity**: Complete user model with authentication, profile, and verification fields
- **Repository Pattern**: Reactive repository with `Flux`/`Mono` return types
- **Connection Pool**: Configured pool with 5-20 connections, 30m idle timeout, 60m lifetime
- **Initialization**: Automatic schema creation and test data insertion via Spring SQL initialization

### Stream Processing Details
- **Reactive Streams**: WebFlux `Flux<UnifiedStreamChunk>` with backpressure support
- **Conversion Flow**: 
  1. Vendor SSE → `StreamToUnifiedTransformer.transformToUnified()` → `UnifiedStreamChunk`
  2. `UnifiedStreamChunk` → `UnifiedToEndpointTransformer.transformToEndpoint()` → Endpoint format
- **Session Management**: Each stream gets unique session ID for state tracking across transformations
- **Filtering**: `ForwardingService.streamUnified()` filters null/empty chunks
- **Type Safety**: Final conversion to `Flux<String>` with JSON serialization fallback

### Error Handling
- Downstream HTTP exceptions wrapped as `ProviderException`
- WebFlux `onErrorMap` and `onErrorResume` for reactive error handling
- Exception propagation maintains stack trace integrity
- Stream error handling preserves session context
- Global exception handling via `GlobalExceptionHandler`

## Key Files
### Backend (Java)
- **Core Services**: `service/ForwardingServiceImpl` (dispatch & model mapping), `service/RoutingServiceImpl` (routing & caching), `service/TransformServiceImpl` (transform routing)
- **Stream Processing**: `service/stream/StreamTransformerFactory` (transformer factory), `service/stream/*/` (vendor-specific transformers)
- **Transformers**: `service/transform/*VendorTransform` (conversion logic), `VendorTransformRegistry` (registry)
- **Models**: `model/internal/UnifiedStreamChunk` (unified stream format), `model/*/stream/` (vendor stream events)
- **Clients**: `client/*Client` (HTTP wrappers), `ClientFactory` (client creation)
- **Configuration**: `config/ProviderConfig` (config binding), `config/ProviderType` (provider enum), `application.yml` (main config)
- **Controllers**: `controller/OpenAIController`, `controller/AnthropicController`, `controller/UserController`
- **Database**: `repository/UserRepository`, `src/main/resources/schema.sql`, `src/main/resources/data.sql`

### Frontend (React/TypeScript)
- **Components**: `web/src/components/` (React components)
- **Main App**: `web/src/App.tsx` (main application component)
- **Entry Point**: `web/src/main.tsx` (application entry)
- **Configuration**: `web/package.json` (dependencies), `web/vite.config.ts` (build config), `web/tailwind.config.js` (styling)

## Adding New Providers

### Adding Models (same provider)
Edit `application.yml` provider.models section → refresh cache via `ForwardingService.refreshModelCache()`

### Adding Compatible Providers (existing type)
Add new provider node in `application.yml` using existing type (`OpenAI` or `Anthropic`) → no code changes needed

### Adding New Protocol (new type)
1. Add `ProviderType` enum entry with `fromString()` parsing logic
2. Implement new `ApiClient` with `chatCompletion()` and `streamChatCompletion()` methods
3. Update `ClientFactory` switch statement
4. Implement `VendorTransform` with bidirectional conversions and register in `VendorTransformRegistry`
5. Create `StreamToUnifiedTransformer` and `UnifiedToEndpointTransformer` implementations
6. Update `StreamTransformerFactory` switch statements for both transformer types
7. Add provider configuration in `application.yml` with matching type

## Performance & Caching
- **Client Caching**: `RoutingServiceImpl.clientCache` (ConcurrentHashMap) caches client instances
- **Model List Caching**: AtomicReference for thread safety, `@PostConstruct` initialization, `refreshModelCache()` to reload
- **Connection Pool**: WebClient uses Reactor Netty pool - max 500 connections, 20s idle, 60s lifetime
- **Database Pool**: R2DBC pool with 5-20 connections, 30m idle timeout, 60m lifetime
- **Timeouts**: Connect 10s, Read 60s, Write 30s (`robella.webclient.timeout`)
- **Retry Logic**: Max 3 attempts, exponential backoff (1s initial, 10s max)
- **Stream Buffering**: 32MB in-memory buffer for large payloads

## Development Guidelines
### Backend (Java)
- **Reactive Programming**: Always use WebFlux `Mono/Flux`, avoid blocking operations
- **Stream Session Management**: Each stream transformation requires unique session ID
- **Error Handling**: Wrap exceptions as `ProviderException`, use reactive error operators
- **Type Safety**: Ensure consistent `ProviderType` enum values across config and factory switches
- **Security**: Never hardcode API keys, use `${ENV_VAR}` placeholders
- **Testing**: Use JUnit 5 with Reactor Test for reactive stream testing
- **Logging**: Use `log.debug`/`log.trace` levels, especially for stream state transitions
- **Model Mapping**: Use `ConfigUtils.getModelMapping()` for client→vendor model name translation
- **Database Access**: Use R2DBC reactive repositories, avoid blocking JDBC operations

### Frontend (React/TypeScript)
- **Component Structure**: Use functional components with hooks
- **TypeScript**: Always define types for props and state
- **Styling**: Use Tailwind CSS utility classes and Ant Design (antd) components
- **UI Components**: **PRIORITY ORDER** - Use antd components first, only create custom components when antd doesn't meet requirements
- **Component Guidelines**: 
  - First check antd component library for needed components
  - Use antd components from the `antd` package
  - Only write custom components when antd lacks the functionality
  - Follow antd patterns and conventions for consistency
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
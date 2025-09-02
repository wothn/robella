# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

**Build**: `mvn clean package`

## Architecture Overview

Robella is a Spring Boot WebFlux reactive AI API aggregation gateway that unifies multiple AI providers (OpenAI, Anthropic, etc.) into standardized API interfaces with advanced streaming capabilities.

### Key Architecture Components

**Data Flow**: `HTTP Request → Controller → ForwardingService → RoutingService → ApiClient → Vendor API → Dual-Layer Stream Transform → Response`

**Core Services**:
- `ForwardingServiceImpl` - Request dispatch with model name mapping (`mapModelName()`) and dual-layer stream orchestration
- `RoutingServiceImpl` - Provider routing decisions, client instance caching, model list management
- `TransformServiceImpl` - Delegates to VendorTransform via `VendorTransformRegistry`
- `StreamTransformerFactory` - Factory for dual-layer stream transformers with session management

**Key Design Principles**:
- Endpoint format separation (OpenAI format independent of backend provider)
- Dual-layer streaming: `Vendor → Unified → Endpoint` with stateful session tracking
- Unified conversion using `UnifiedChatRequest/Response/StreamChunk` as intermediate format
- Reactive stream processing with WebFlux `Mono/Flux` and backpressure

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

## Key Files
- **Core Services**: `service/ForwardingServiceImpl` (dispatch & model mapping), `service/RoutingServiceImpl` (routing & caching), `service/TransformServiceImpl` (transform routing)
- **Stream Processing**: `service/stream/StreamTransformerFactory` (transformer factory), `service/stream/*/` (vendor-specific transformers)
- **Transformers**: `service/transform/*VendorTransform` (conversion logic), `VendorTransformRegistry` (registry)
- **Models**: `model/internal/UnifiedStreamChunk` (unified stream format), `model/*/stream/` (vendor stream events)
- **Clients**: `client/*Client` (HTTP wrappers), `ClientFactory` (client creation)
- **Configuration**: `config/ProviderConfig` (config binding), `config/ProviderType` (provider enum), `application.yml` (main config)
- **Controllers**: `controller/OpenAIController`, `controller/AnthropicController`

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
- **Timeouts**: Connect 10s, Read 60s, Write 30s (`robella.webclient.timeout`)
- **Retry Logic**: Max 3 attempts, exponential backoff (1s initial, 10s max)
- **Stream Buffering**: 32MB in-memory buffer for large payloads

## Development Guidelines
- **Reactive Programming**: Always use WebFlux `Mono/Flux`, avoid blocking operations
- **Stream Session Management**: Each stream transformation requires unique session ID
- **Error Handling**: Wrap exceptions as `ProviderException`, use reactive error operators
- **Type Safety**: Ensure consistent `ProviderType` enum values across config and factory switches
- **Security**: Never hardcode API keys, use `${ENV_VAR}` placeholders
- **Testing**: Current test directory empty - add unit tests for transformers and integration tests for stream processing
- **Logging**: Use `log.debug`/`log.trace` levels, especially for stream state transitions
- **Model Mapping**: Use `ConfigUtils.getModelMapping()` for client→vendor model name translation

## Debugging Stream Processing
- **Session Tracking**: Each stream has UUID session ID in logs
- **Transform Layers**: Monitor both vendor→unified and unified→endpoint conversions
- **Empty Chunk Filtering**: Check for null/empty chunks being filtered in `ForwardingService`
- **WebFlux Debugging**: Enable `reactor.netty.http.client: DEBUG` for network-level issues
- **Stream State**: Monitor session-based state management in transformers
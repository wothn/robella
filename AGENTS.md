# Agent.md

This file provides guidance to Agent Code when working with code in this repository.

## Build and Development Commands

**Build**: `mvn clean package`
**Run**: `java -jar target/robella-1.0.0.jar`

## Architecture Overview

Robella is a Spring Boot WebFlux reactive AI API aggregation gateway that unifies multiple AI providers (OpenAI, Anthropic, Gemini, etc.) into standardized API interfaces.

### Key Architecture Components

**Data Flow**: `HTTP Request → Controller → ForwardingService → RoutingService → AIApiClient → Vendor API → TransformService`

**Core Services**:
- `ForwardingServiceImpl` - Request dispatch entry point, handles model mapping and stream response filtering
- `RoutingServiceImpl` - Model→provider routing decisions, adapter instance caching
- `TransformServiceImpl` - Routes to specific VendorTransform implementations via VendorTransformRegistry

**Key Design Principles**:
- Endpoint format separation (OpenAI format independent of backend provider type)
- Unified conversion layer using `UnifiedChatRequest/Response/StreamChunk` as internal intermediate format
- Reactive stream processing with WebFlux `Mono/Flux`

### Provider/Adapter System
- **Interface**: `AIApiClient` defines contract (`chatCompletion()`, `streamChatCompletion()`)
- **Factory**: `ClientFactory.createClient()` creates instances based on `ProviderType` enum
- **Built-in Adapters**:
    - `OpenAIClient`: Handles OpenAI and compatible providers (DeepSeek, ModelScope, AIHubMix). Azure support via `deploymentName` with URL pattern `/deployments/{name}/chat/completions`
    - `AnthropicClient`: Anthropic Messages API (`/messages`), handles SSE streaming

### Configuration & Routing
- **Config File**: `application.yml` with `providers:` section binding to `ProviderConfig`
- **Provider Structure**: `name, type, api-key, base-url, deploymentName?, models[]{name, vendor-model}`
- **Routing Strategy**: `RoutingServiceImpl.decideProviderByModel()` linear scan of all providers.models to match requested model, defaults to "openai" if no match
- **Environment Variables**: API Keys use `${ENV_VAR}` placeholders (e.g., `${OPENAI_API_KEY}`, `${ANTHROPIC_API_KEY}`)

### Unified Model Conversion
- **Format**: `model/internal/UnifiedChatRequest/Response/StreamChunk` as internal representation
- **Registry**: `VendorTransformRegistry` manually registers vendor-specific transformers
- **Transformers**: `service/transform/*VendorTransform` handle bidirectional conversions

### Stream Processing
- Uses WebFlux `Flux<UnifiedStreamChunk>` for reactive stream handling with backpressure support
- **Conversion Flow**: Vendor SSE events → `VendorTransform.vendorStreamEventToUnified()` → `UnifiedStreamChunk` → endpoint format
- **OpenAIClient**: Sets `request.setStream(true)`, receives `text/event-stream` fragments
- **AnthropicClient**: Uses `Accept: text/event-stream`, parses SSE events
- **Filtering**: `ForwardingService.streamUnified()` filters empty chunks, only returns content changes or end signals
- **End Marker**: OpenAI endpoints return `[DONE]` marker added via `concatWith(Flux.just("[DONE]"))`

### Error Handling
- All downstream HTTP exceptions wrapped as `ProviderException`
- Uses WebFlux `onErrorMap` and `onErrorResume` for reactive error handling
- Exception propagation maintains stack trace integrity

## Key Files
- **Services**: `service/ForwardingServiceImpl` (request dispatch), `service/RoutingServiceImpl` (routing & caching), `service/TransformServiceImpl` (transform routing)
- **Transformers**: `service/transform/*VendorTransform` (conversion logic), `VendorTransformRegistry` (transformer registry)
- **Clients**: `client/*Client` (HTTP client wrappers), `ClientFactory` (client creation)
- **Configuration**: `config/ProviderConfig` (config binding), `config/ProviderType` (provider enum), `application.yml` (main config)
- **Controllers**: `controller/OpenAIController` (OpenAI-compatible API), `controller/AnthropicController` (Anthropic API)

## Adding New Providers

### Adding Models (same provider)
Edit `application.yml` provider.models section → refresh cache via `ForwardingService.refreshModelCache()`

### Adding Compatible Providers (existing type)
Add new provider node in `application.yml` using existing type (`OpenAI` or `Anthropic`) → no code changes needed

### Adding New Protocol (new type)
1. Add `ProviderType` enum entry with `fromString()` parsing logic
2. Implement new `AIProviderAdapter` with `chatCompletion()` and `streamChatCompletion()` methods
3. Update `AdapterFactory` switch statement
4. Implement corresponding `VendorTransform` with bidirectional conversions and register in `VendorTransformRegistry`
5. Add provider configuration in `application.yml` with matching type

## Performance & Caching
- **Client Caching**: `RoutingServiceImpl.clientCache` (ConcurrentHashMap) caches client instances
- **Model List Caching**: AtomicReference ensures thread safety, initialized via @PostConstruct, call `refreshModelCache()` to reload
- **Connection Pool**: WebClient uses Reactor Netty connection pool configured in `robella.webclient.connection-pool` (max 500 connections)
- **Timeouts**: Connect (10s), Read (60s), Write (30s) configured in `robella.webclient.timeout`
- **Retry Logic**: Max 3 attempts with exponential backoff (1s initial, 10s max delay)

## Development Guidelines
- **Security**: Never hardcode API keys, use `${ENV_VAR}` placeholders in configuration
- **Type Consistency**: Ensure `ProviderConfig.Provider.type` matches `ProviderType` enum values
- **Reactive Programming**: Use WebFlux `Mono/Flux`, avoid blocking operations
- **Error Handling**: Wrap exceptions as `ProviderException` in adapters using `onErrorMap`
- **Logging**: Use `log.debug`/`log.trace` for new logs to avoid noise
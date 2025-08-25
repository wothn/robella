# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

**Build**: `mvn clean package`
**Run**: `java -jar target/robella-1.0.0.jar`
**Test**: `mvn test`

## Architecture Overview

Robella is a Spring Boot WebFlux reactive AI API aggregation gateway that unifies multiple AI providers (OpenAI, Anthropic, Gemini, etc.) into standardized API interfaces.

### Key Architecture Components

**Data Flow**: `HTTP Request → Controller → TransformService → ForwardingService → RoutingService → AIProviderAdapter → Vendor API → TransformService`

**Core Services**:
- `ForwardingServiceImpl` - Request dispatch entry point, handles model mapping and stream response filtering
- `RoutingServiceImpl` - Model→provider routing decisions, adapter instance caching
- `TransformServiceImpl` - Routes to specific VendorTransform implementations via VendorTransformRegistry

**Key Design Principles**:
- Endpoint format separation (OpenAI format independent of backend provider type)
- Unified conversion layer using `UnifiedChatRequest/Response/StreamChunk` as internal intermediate format
- Reactive stream processing with WebFlux `Mono/Flux`

### Provider/Adapter System
- **Interface**: `AIProviderAdapter` defines contract (`chatCompletion()`, `streamChatCompletion()`)
- **Factory**: `AdapterFactory.createAdapter()` creates instances based on `ProviderType` enum
- **Built-in Adapters**: `OpenAIAdapter` (OpenAI and compatible providers), `AnthropicAdapter` (Anthropic Messages API)

### Configuration & Routing
- **Config File**: `application.yml` with `providers:` section binding to `ProviderConfig`
- **Routing**: Linear scan of all providers.models to match requested model, defaults to "openai" if no match
- **Environment Variables**: API Keys use `${ENV_VAR}` placeholders (e.g., `${OPENAI_API_KEY}`)

### Unified Model Conversion
- **Format**: `model/internal/UnifiedChatRequest/Response/StreamChunk` as internal representation
- **Registry**: `VendorTransformRegistry` manually registers vendor-specific transformers
- **Transformers**: `service/transform/*VendorTransform` handle bidirectional conversions

### Stream Processing
- Uses WebFlux `Flux<UnifiedStreamChunk>` for reactive stream handling
- **OpenAIAdapter**: Sets `request.setStream(true)`, receives `text/event-stream` fragments
- **AnthropicAdapter**: Uses `Accept: text/event-stream`, parses SSE events
- **End Marker**: OpenAI endpoints return `[DONE]` marker added via `concatWith(Flux.just("[DONE]"))`

### Error Handling
- All downstream HTTP exceptions wrapped as `ProviderException`
- Uses WebFlux `onErrorMap` and `onErrorResume` for reactive error handling
- Exception propagation maintains stack trace integrity

## Key Files
- **Services**: `service/ForwardingServiceImpl`, `service/RoutingServiceImpl`, `service/TransformServiceImpl`
- **Transformers**: `service/transform/*VendorTransform`, `VendorTransformRegistry`
- **Adapters**: `adapter/*Adapter`, `AdapterFactory`
- **Configuration**: `config/ProviderConfig`, `config/ProviderType`, `application.yml`
- **Controllers**: `controller/OpenAIController`, `controller/AnthropicController`

## Adding New Providers
1. Add `ProviderType` enum entry with parsing logic
2. Implement new `AIProviderAdapter`
3. Update `AdapterFactory` switch statement
4. Implement corresponding `VendorTransform` and register in `VendorTransformRegistry`
5. Add provider configuration in `application.yml` with matching type

## Performance & Caching
- Adapter caching in `RoutingServiceImpl.adapterCache` (ConcurrentHashMap)
- Model list caching with AtomicReference, initialized via @PostConstruct
- WebClient uses Reactor Netty connection pool configured in `robella.webclient.connection-pool`
- Stream timeout configured in `robella.webclient.timeout.read` (default 60s)
# Qwen请求处理流程

本文档描述了一个请求从接收到响应的完整处理流程，以Qwen为例。

## 1. 请求接收

请求首先进入`OpenAIController`类中的`chatCompletions`方法。该方法负责接收HTTP请求，进行参数验证，并根据请求类型（流式或非流式）决定处理方式。

```java
@PostMapping(value = "/chat/completions",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
public Mono<ResponseEntity<?>> chatCompletions(
        @RequestBody @Valid OpenAIChatRequest request,
        HttpServletRequest httpRequest) {
    // 处理请求
}
```

## 2. 请求转换

在`OpenAIController`中，会调用`TransformService`将`OpenAIChatRequest`转换为统一的`UnifiedChatRequest`格式。

```java
UnifiedChatRequest unifiedRequest = transformService.toUnified(request);
```

## 3. 路由决策

接下来，`RoutingService`会根据`UnifiedChatRequest`中的信息决定使用哪个AI提供商。对于Qwen请求，会返回"qwen"作为目标提供商。

```java
String targetProvider = routingService.decideProvider(unifiedRequest);
```

## 4. 核心转发服务

`ForwardingService`根据目标提供商决定是进行流式还是非流式处理，并调用相应的适配器。

### 4.1 非流式处理

对于非流式请求，`ForwardingServiceImpl`会调用`QwenAdapter`的`chatCompletion`方法。

```java
Mono<UnifiedChatResponse> response = qwenAdapter.chatCompletion(unifiedRequest);
```

### 4.2 流式处理

对于流式请求，`ForwardingServiceImpl`会调用`QwenAdapter`的`streamChatCompletion`方法。

```java
Flux<ServerSentEvent<String>> stream = qwenAdapter.streamChatCompletion(unifiedRequest);
```

## 5. Qwen适配器处理

`QwenAdapter`是Qwen提供商的具体实现，它负责将统一格式的请求转换为Qwen特定格式，并调用`QwenClient`发送请求。

### 5.1 请求转换

`QwenAdapter`使用`QwenMessageTransformer`将`UnifiedChatRequest`转换为`QwenChatRequest`。

```java
QwenChatRequest qwenRequest = transformer.transformRequest(unifiedRequest);
```

### 5.2 发送请求

`QwenAdapter`调用`QwenClient`的相应方法发送请求到Qwen API。

```java
Mono<QwenChatResponse> response = qwenClient.chatCompletion(qwenRequest);
```

或者对于流式请求：

```java
Flux<QwenStreamEvent> stream = qwenClient.streamChatCompletion(qwenRequest);
```

## 6. 响应转换

当从Qwen API接收到响应后，`QwenAdapter`会使用`QwenMessageTransformer`将Qwen特定格式的响应转换为统一格式的响应。

### 6.1 非流式响应

```java
UnifiedChatResponse unifiedResponse = transformer.transformResponse(qwenResponse);
```

### 6.2 流式响应

```java
UnifiedStreamEvent unifiedEvent = transformer.transformStreamEvent(qwenEvent);
```

## 7. 响应返回

最后，`ForwardingService`将统一格式的响应转换为OpenAI兼容格式，并通过`OpenAIController`返回给客户端。

### 7.1 非流式响应

```java
OpenAIChatResponse openaiResponse = transformService.toOpenAI(unifiedResponse);
return ResponseEntity.ok(openaiResponse);
```

### 7.2 流式响应

流式响应通过Server-Sent Events(SSE)逐块发送给客户端。

```java
return ResponseEntity.ok()
    .contentType(MediaType.TEXT_EVENT_STREAM)
    .body(stream);
```

## 8. 监控统计

在整个处理过程中，`MonitoringService`会记录调用的开始和结束时间，以及相关的统计信息。

```java
monitoringService.recordCallStart(targetProvider, unifiedRequest.getModel());
// 处理完成后
monitoringService.recordCallEnd(targetProvider, unifiedRequest.getModel(), duration, success);
```
package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.exception.ProviderException;
import org.elmo.robella.model.openai.ChatCompletionRequest;
import org.elmo.robella.model.openai.ChatCompletionResponse;
import org.elmo.robella.model.openai.ModelListResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForwardingServiceImpl implements ForwardingService {

    private final TransformService transformService;
    private final RoutingService routingService;

    @Override
    public Mono<ChatCompletionResponse> forwardChatCompletion(ChatCompletionRequest request) {
        // 决定目标提供商
        String providerName = routingService.decideProvider(request);
        
        // 获取适配器
        AIProviderAdapter adapter = routingService.getAdapter(providerName);
        
        // 转换为提供商特定格式
        Object vendorRequest = transformService.toVendor(request, providerName);
        
        // 执行调用
        return adapter.chatCompletion(vendorRequest)
                .map(vendorResponse -> {
                    return transformService.toOpenAI(vendorResponse, providerName);
                })
                .onErrorMap(error -> new ProviderException("Provider call failed: " + error.getMessage(), error));
    }

    @Override
    public Flux<String> streamChatCompletion(ChatCompletionRequest request) {
        log.debug("进入Forwarding");
        
        // 决定目标提供商
        String providerName = routingService.decideProvider(request);
        
        // 获取适配器
        AIProviderAdapter adapter = routingService.getAdapter(providerName);
        
        // 转换为提供商特定格式
        Object vendorRequest = transformService.toVendor(request, providerName);
        
        // 执行流式调用
        return adapter.streamChatCompletion(vendorRequest)
                .<String>handle((event, sink) -> {
                    try {
                        // 转换事件为OpenAI格式JSON
                        String jsonData = transformService.toOpenAIStreamEvent(event, providerName);
                        
                        // 直接发送数据（如果不为空）
                        if (!jsonData.isEmpty()) {
                            sink.next(jsonData);
                        }
                    } catch (Exception e) {
                        log.error("Error converting stream event", e);
                        sink.error(new RuntimeException("Event conversion failed", e));
                    }
                })
                .doOnError(error -> log.error("Stream processing error", error))
                .onErrorMap(error -> new ProviderException("Provider stream failed: " + error.getMessage(), error));
    }

    @Override
    public Mono<ModelListResponse> listModels() {
        // 委托给 RoutingService 处理模型列表
        ModelListResponse models = routingService.getAvailableModels();
        return Mono.just(models);
    }
    
    @Override
    public void refreshModelCache() {
        // 委托给 RoutingService 处理缓存刷新
        log.debug("Delegating model cache refresh to RoutingService");
        routingService.refreshModelCache();
    }

}

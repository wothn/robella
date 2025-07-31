package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.exception.ProviderException;
import org.elmo.robella.model.request.OpenAIChatRequest;
import org.elmo.robella.model.request.UnifiedChatRequest;
import org.elmo.robella.model.response.openai.OpenAIChatResponse;
import org.elmo.robella.model.response.openai.OpenAIModelListResponse;
import org.elmo.robella.model.response.UnifiedChatResponse;
import org.elmo.robella.model.response.openai.OpenAIModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForwardingServiceImpl implements ForwardingService {

    private final TransformService transformService;
    private final RoutingService routingService;

    @Override
    public Mono<OpenAIChatResponse> forwardChatCompletion(OpenAIChatRequest request) {
        // 转换请求为统一格式
        UnifiedChatRequest unifiedRequest = transformService.toUnified(request);
        
        // 决定目标提供商
        String providerName = routingService.decideProvider(unifiedRequest);
        
        // 获取适配器
        AIProviderAdapter adapter = routingService.getAdapter(providerName);
        
        // 转换为提供商特定格式
        Object vendorRequest = transformService.toVendor(unifiedRequest, providerName);
        
        // 执行调用
        return adapter.chatCompletion(vendorRequest)
                .map(unifiedResponse -> {
                    return transformService.toOpenAI((UnifiedChatResponse) unifiedResponse);
                })
                .onErrorMap(error -> new ProviderException("Provider call failed: " + error.getMessage(), error));
    }

    @Override
    public Flux<String> streamChatCompletion(OpenAIChatRequest request) {
        log.debug("进入Forwarding");
        
        // 转换请求为统一格式
        UnifiedChatRequest unifiedRequest = transformService.toUnified(request);
        
        // 决定目标提供商
        String providerName = routingService.decideProvider(unifiedRequest);
        
        // 获取适配器
        AIProviderAdapter adapter = routingService.getAdapter(providerName);
        
        // 转换为提供商特定格式
        Object vendorRequest = transformService.toVendor(unifiedRequest, providerName);
        
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
    public Mono<OpenAIModelListResponse> listModels() {
        // 获取所有提供商配置
        var providers = routingService.getProviderConfigMap();
        
        log.info("Listing models from providers: {}", providers.keySet());
        
        // 收集所有提供商的模型
        List<Mono<List<org.elmo.robella.model.common.ModelInfo>>> modelRequests = 
            providers.keySet().stream()
                .map(providerName -> {
                    log.debug("Fetching models from provider: {}", providerName);
                    AIProviderAdapter adapter = routingService.getAdapter(providerName);
                    return adapter.listModels();
                })
                .collect(Collectors.toList());
        
        // 合并所有模型列表
        return Mono.zip(modelRequests, responses -> {
            return java.util.Arrays.stream(responses)
                    .flatMap(response -> {
                        @SuppressWarnings("unchecked")
                        List<org.elmo.robella.model.common.ModelInfo> modelList = 
                            (List<org.elmo.robella.model.common.ModelInfo>) response;
                        return modelList.stream();
                    })
                    .collect(Collectors.toList());
        }).map(models -> {
            OpenAIModelListResponse response = new OpenAIModelListResponse();
            response.setData(models.stream()
                    .map(model -> {
                        OpenAIModel openAIModel =
                            new OpenAIModel();
                        openAIModel.setId(model.getId());
                        openAIModel.setObject("model");
                        openAIModel.setOwnedBy(model.getVendor() != null ? model.getVendor() : "unknown");
                        return openAIModel;
                    })
                    .collect(Collectors.toList()));
            return response;
        });
    }
}
package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.exception.ProviderException;
import org.elmo.robella.model.request.OpenAIChatRequest;
import org.elmo.robella.model.request.UnifiedChatRequest;
import org.elmo.robella.model.response.OpenAIChatResponse;
import org.elmo.robella.model.response.OpenAIModelListResponse;
import org.elmo.robella.model.response.UnifiedChatResponse;
import org.springframework.http.codec.ServerSentEvent;
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
    private final MonitoringService monitoringService;

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
        
        // 记录调用开始
        String traceId = monitoringService.startCall(providerName, unifiedRequest.getModel());
        
        // 执行调用
        return adapter.chatCompletion(vendorRequest)
                .doOnSuccess(response -> monitoringService.endCall(traceId, true))
                .doOnError(error -> monitoringService.endCall(traceId, false))
                .map(unifiedResponse -> transformService.toOpenAI((UnifiedChatResponse) unifiedResponse))
                .onErrorMap(error -> new ProviderException("Provider call failed: " + error.getMessage(), error));
    }

    @Override
    public Flux<ServerSentEvent<String>> streamChatCompletion(OpenAIChatRequest request) {
        // 转换请求为统一格式
        UnifiedChatRequest unifiedRequest = transformService.toUnified(request);
        
        // 决定目标提供商
        String providerName = routingService.decideProvider(unifiedRequest);
        
        // 获取适配器
        AIProviderAdapter adapter = routingService.getAdapter(providerName);
        
        // 转换为提供商特定格式
        Object vendorRequest = transformService.toVendor(unifiedRequest, providerName);
        
        // 记录调用开始
        String traceId = monitoringService.startCall(providerName, unifiedRequest.getModel());
        
        // 执行流式调用
        return adapter.streamChatCompletion(vendorRequest)
                .map(event -> {
                    // 转换事件为OpenAI格式
                    Object openAIEvent = transformService.toOpenAIStreamEvent(event);
                    return ServerSentEvent.builder(openAIEvent.toString()).build();
                })
                .doOnComplete(() -> monitoringService.endCall(traceId, true))
                .doOnError(error -> monitoringService.endCall(traceId, false))
                .onErrorMap(error -> new ProviderException("Provider stream failed: " + error.getMessage(), error));
    }

    @Override
    public Mono<OpenAIModelListResponse> listModels() {
        // 获取所有提供商配置
        var providers = routingService.getProviderConfigMap();
        
        // 收集所有提供商的模型
        List<Mono<List<org.elmo.robella.model.common.ModelInfo>>> modelRequests = 
            providers.keySet().stream()
                .map(providerName -> {
                    AIProviderAdapter adapter = routingService.getAdapter(providerName);
                    return adapter.listModels();
                })
                .collect(Collectors.toList());
        
        // 合并所有模型列表
        return Mono.zip(modelRequests, responses -> {
            return java.util.Arrays.stream(responses)
                    .flatMap(response -> ((List<org.elmo.robella.model.common.ModelInfo>) response).stream())
                    .collect(Collectors.toList());
        }).map(models -> {
            OpenAIModelListResponse response = new OpenAIModelListResponse();
            response.setData(models.stream()
                    .map(model -> {
                        org.elmo.robella.model.response.OpenAIModel openAIModel = 
                            new org.elmo.robella.model.response.OpenAIModel();
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
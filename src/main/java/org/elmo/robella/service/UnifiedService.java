package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.elmo.robella.model.openai.model.ModelInfo;
import org.elmo.robella.repository.ModelRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedService {

    private final RoutingService routingService;
    private final ModelRepository modelRepository;

    public Mono<ModelListResponse> listModels() {
        return modelRepository.findByPublishedTrue()
            .map(model -> {
                ModelInfo modelInfo = new ModelInfo();
                modelInfo.setId(model.getModelKey());
                modelInfo.setObject("model");
                modelInfo.setOwnedBy(model.getOrganization() != null ? model.getOrganization() : "robella");
                return modelInfo;
            })
            .collectList()
            .map(modelInfos -> {
                ModelListResponse response = new ModelListResponse();
                response.setObject("list");
                response.setData(modelInfos);
                return response;
            });
    }

    /**
     * 发送聊天请求（已路由版本）
     * 直接使用传入的客户端和供应商信息，避免重复路由查询
     */
    public Mono<UnifiedChatResponse> sendChatRequestWithClient(UnifiedChatRequest request, RoutingService.ClientWithProvider clientWithProvider) {
        // 填充 ProviderType，优先使用 VendorModel 的 ProviderType
        if (clientWithProvider.getVendorModel().getProviderType() != null) {
            request.setProviderType(clientWithProvider.getVendorModel().getProviderType());
        } else if (clientWithProvider.getProvider().getProviderType() != null) {
            request.setProviderType(clientWithProvider.getProvider().getProviderType());
        }

        // 发起请求并传递日志上下文
        return clientWithProvider.getClient().chatCompletion(request, clientWithProvider.getProvider());
    }

    /**
     * 发送流式聊天请求（已路由版本）
     * 直接使用传入的客户端和供应商信息，避免重复路由查询
     */
    public Flux<UnifiedStreamChunk> sendStreamRequestWithClient(UnifiedChatRequest request, RoutingService.ClientWithProvider clientWithProvider) {
        // 填充 ProviderType，优先使用 VendorModel 的 ProviderType
        if (clientWithProvider.getVendorModel().getProviderType() != null) {
            request.setProviderType(clientWithProvider.getVendorModel().getProviderType());
        } else {
            request.setProviderType(clientWithProvider.getProvider().getProviderType());
        }

        // 发起流式请求并传递日志上下文
        return clientWithProvider.getClient().streamChatCompletion(request, clientWithProvider.getProvider())
                .filter(Objects::nonNull);
    }

    public Mono<UnifiedChatResponse> sendChatRequest(UnifiedChatRequest request) {
        String modelName = request.getModel();

        // 获取合适的客户端和 Provider
        return routingService.getClientWithProviderByModelKey(modelName)
                .flatMap(clientWithProvider -> {
                    // 填充 ProviderType，优先使用 VendorModel 的 ProviderType
                    if (clientWithProvider.getVendorModel().getProviderType() != null) {
                        request.setProviderType(clientWithProvider.getVendorModel().getProviderType());
                    } else if (clientWithProvider.getProvider().getProviderType() != null) {
                        request.setProviderType(clientWithProvider.getProvider().getProviderType());
                    }

                    // 发起请求并传递日志上下文
                    return clientWithProvider.getClient().chatCompletion(request, clientWithProvider.getProvider());
                });
    }

    public Flux<UnifiedStreamChunk> sendStreamRequest(UnifiedChatRequest request) {
        String modelName = request.getModel();

        // 获取合适的客户端和 Provider
        return routingService.getClientWithProviderByModelKey(modelName)
                .flatMapMany(clientWithProvider -> {
                    // 填充 ProviderType，优先使用 VendorModel 的 ProviderType
                    if (clientWithProvider.getVendorModel().getProviderType() != null) {
                        request.setProviderType(clientWithProvider.getVendorModel().getProviderType());
                    } else {
                        request.setProviderType(clientWithProvider.getProvider().getProviderType());
                    }

                    // 发起流式请求并传递日志上下文
                    return clientWithProvider.getClient().streamChatCompletion(request, clientWithProvider.getProvider());
                })
                .filter(Objects::nonNull);
    }

}
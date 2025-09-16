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
                modelInfo.setId(model.getName());
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

    public Mono<UnifiedChatResponse> sendChatRequest(UnifiedChatRequest request) {
        String modelName = request.getModel();
        
        // 获取合适的客户端和 Provider
        return routingService.getClientWithProviderByVendorModelName(modelName)
                .flatMap(clientWithProvider -> {
                    // 填充 ProviderType，优先使用 VendorModel 的 ProviderType
                    if (clientWithProvider.getVendorModel().getProviderType() != null) {
                        request.setProviderType(clientWithProvider.getVendorModel().getProviderType());
                    } else if (clientWithProvider.getProvider().getProviderType() != null) {
                        request.setProviderType(clientWithProvider.getProvider().getProviderType());
                    }
                    // 发起请求
                    return clientWithProvider.getClient().chatCompletion(request, clientWithProvider.getProvider());
                });
    }

    public Flux<UnifiedStreamChunk> sendStreamRequest(UnifiedChatRequest request) {
        String modelName = request.getModel();

        // 获取合适的客户端和 Provider
        return routingService.getClientWithProviderByVendorModelName(modelName)
                .flatMapMany(clientWithProvider -> {
                    // 填充 ProviderType，优先使用 VendorModel 的 ProviderType
                    if (clientWithProvider.getVendorModel().getProviderType() != null) {
                        request.setProviderType(clientWithProvider.getVendorModel().getProviderType());
                    } else {
                        request.setProviderType(clientWithProvider.getProvider().getProviderType());
                    }
                    // 发起流式请求
                    return clientWithProvider.getClient().streamChatCompletion(request, clientWithProvider.getProvider());
                })
                .filter(Objects::nonNull);
    }

}
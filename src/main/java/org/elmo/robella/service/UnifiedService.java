package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import org.elmo.robella.model.entity.Model;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.elmo.robella.model.openai.model.ModelInfo;
import org.elmo.robella.client.ApiClient;
import org.elmo.robella.context.RequestContextHolder;
import org.elmo.robella.context.RequestContextHolder.RequestContext;
import org.elmo.robella.exception.ResourceNotFoundException;
import org.elmo.robella.mapper.ModelMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedService {

    private final RoutingService routingService;
    private final ModelMapper modelMapper;

    public ModelListResponse listModels() {
        List<Model> models = modelMapper.findByPublishedTrue();
        
        // 添加空值检查
        if (models == null) {
            models = List.of();
        }
        
        List<ModelInfo> modelInfos = models.stream()
            .map(this::convertToModelInfo)
            .toList();

        ModelListResponse response = new ModelListResponse();
        response.setObject("list");
        response.setData(modelInfos);
        return response;
    }
    
    /**
     * 将Model实体转换为ModelInfo对象
     * @param model Model实体
     * @return ModelInfo对象
     */
    private ModelInfo convertToModelInfo(Model model) {
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setId(model.getModelKey());
        modelInfo.setObject("model");
        modelInfo.setOwnedBy(model.getOrganization() != null ? model.getOrganization() : "robella");
        return modelInfo;
    }

    public UnifiedChatResponse sendChatRequest(UnifiedChatRequest request) {
        String modelKey = request.getModel();
        RoutingService.ClientWithInfo clientWithInfo = routingService.routeAndClient(modelKey);
        if (clientWithInfo == null) {
            throw new ResourceNotFoundException("No available provider for model: " + modelKey);
        }
        request.setModel(clientWithInfo.getVendorModel().getVendorModelKey());
        ApiClient apiClient = clientWithInfo.getClient();

        RequestContext ctx = RequestContextHolder.getContext();
        ctx.setProviderId(clientWithInfo.getProvider().getId());
        ctx.setVendorModel(clientWithInfo.getVendorModel());

        return apiClient.chat(request, clientWithInfo.getProvider());
    }

    public Stream<UnifiedStreamChunk> sendStreamRequest(UnifiedChatRequest request) {
        String modelKey = request.getModel();
        RoutingService.ClientWithInfo clientWithInfo = routingService.routeAndClient(modelKey);
        if (clientWithInfo == null) {
            throw new ResourceNotFoundException("No available provider for model: " + modelKey);
        }
        request.setModel(clientWithInfo.getVendorModel().getVendorModelKey());
        ApiClient apiClient = clientWithInfo.getClient();

        RequestContext ctx = RequestContextHolder.getContext();
        ctx.setProviderId(clientWithInfo.getProvider().getId());
        ctx.setVendorModel(clientWithInfo.getVendorModel());

        return apiClient.chatStream(request, clientWithInfo.getProvider());
    }

}
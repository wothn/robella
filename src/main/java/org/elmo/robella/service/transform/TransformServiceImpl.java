package org.elmo.robella.service.transform;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class TransformServiceImpl implements TransformService {

    private final ProviderConfig providerConfig;
    private final VendorTransformRegistry registry;

    @Override
    public VendorTransform getVendorTransform(String providerType) {
        return registry.get(providerType == null ? "OpenAI" : providerType);
    }
    
    private String providerTypeByName(String providerName) {
        if (providerName == null) return "OpenAI";
        var p = providerConfig.getProviders().get(providerName);
        return p != null ? p.getType() : "OpenAI";
    }
    
    
    // ===== 通用 =====
    @Override
    public Object unifiedToVendorRequest(UnifiedChatRequest unified, String providerName) {
        String providerType = providerTypeByName(providerName);
        
        // 设置 providerName 到 unified 对象中
        unified.setProviderName(providerName);
        
        // 调用具体的转换器（模型映射已在ForwardingService中处理）
        return getVendorTransform(providerType).unifiedToVendorRequest(unified);
    }

    @Override
    public UnifiedChatResponse vendorResponseToUnified(Object vendorResp, String providerName) {
        String providerType = providerTypeByName(providerName);
        return getVendorTransform(providerType).vendorResponseToUnified(vendorResp);
    }
    
    /**
     * 根据端点格式转换请求到统一格式（与实际调用的provider无关）
     */
    @Override
    public UnifiedChatRequest endpointRequestToUnified(Object vendorRequest, String endpointType) {
        return getVendorTransform(endpointType).vendorRequestToUnified(vendorRequest);
    }
    
    /**
     * 根据端点格式从统一格式转换响应（与实际调用的provider无关）
     */
    @Override
    public Object unifiedToEndpointResponse(UnifiedChatResponse unifiedResponse, String endpointType) {
        return getVendorTransform(endpointType).unifiedToVendorResponse(unifiedResponse);
    }
}

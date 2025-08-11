package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.service.transform.VendorTransformRegistry;
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
        return getVendorTransform(providerTypeByName(providerName)).unifiedToVendorRequest(unified);
    }

    @Override
    public UnifiedChatResponse vendorResponseToUnified(Object vendorResp, String providerName) {
        return getVendorTransform(providerTypeByName(providerName)).vendorResponseToUnified(vendorResp);
    }

    @Override
    public UnifiedStreamChunk vendorStreamEventToUnified(Object vendorEvent, String providerName) {
        return getVendorTransform(providerTypeByName(providerName)).vendorStreamEventToUnified(vendorEvent);
    }

    @Override
    public Object unifiedStreamChunkToVendor(UnifiedStreamChunk chunk, String providerName) {
        return getVendorTransform(providerTypeByName(providerName)).unifiedStreamChunkToVendor(chunk);
    }

    @Override
    public UnifiedChatRequest vendorRequestToUnified(Object vendorRequest, String providerName) {
        return getVendorTransform(providerTypeByName(providerName)).vendorRequestToUnified(vendorRequest);
    }

    @Override
    public Object unifiedToVendorResponse(UnifiedChatResponse unifiedResponse, String providerName) {
        return getVendorTransform(providerTypeByName(providerName)).unifiedToVendorResponse(unifiedResponse);
    }
}

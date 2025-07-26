package org.elmo.robella.util;

import org.elmo.robella.config.ProviderConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigUtils {
    
    @Autowired
    private ProviderConfig providerConfig;
    
    public ProviderConfig.Provider getProvider(String providerName) {
        return providerConfig.getProviders().get(providerName);
    }
    
    public String getModelMapping(String providerName, String modelName) {
        ProviderConfig.Provider provider = getProvider(providerName);
        if (provider != null) {
            for (ProviderConfig.Model model : provider.getModels()) {
                if (model.getName().equals(modelName)) {
                    return model.getVendorModel();
                }
            }
        }
        return modelName; // 默认返回原始模型名
    }
}
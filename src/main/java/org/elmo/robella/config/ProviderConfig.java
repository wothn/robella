package org.elmo.robella.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "providers")
public class ProviderConfig {
    private Map<String, Provider> providers;

    @Data
    public static class Provider {
        private String name;
        private String type; // 用于区分适配器类型
        private String apiKey;
        private String baseUrl;
        private String deploymentName; // Azure OpenAI特有字段
        private List<Model> models;
    }

    @Data
    public static class Model {
        private String name;
        private String vendorModel;
    }
}
package org.elmo.robella.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties
public class ProviderConfig {
    private Map<String, Provider> providers;

    @Data
    public static class Provider {
        private String name;
        private String type; // 原始配置中的字符串，向后兼容
        private String apiKey;
        private String baseUrl;
        private String deploymentName; // Azure OpenAI特有字段
        private String apiVersion; // 可选的 API 版本（尤其用于 Azure OpenAI 或自建兼容服务）
        private Boolean enabled = true; // 是否启用此提供商，默认为true
        private List<Model> models;

        public ProviderType getProviderType() {
            return ProviderType.fromString(type);
        }
        
        public List<Model> getModels() {
            return models;
        }
    }

    @Data
    public static class Model {
        private String name;
        private String vendorModel;
        private String thinkingField; // 思考字段配置，如 "thinking"
        
        public String getName() {
            return name;
        }
    }
}
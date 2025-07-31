package org.elmo.robella.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.elmo.robella.RobellaApplication;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RobellaApplication.class)
@TestPropertySource(locations = "classpath:providers.yml")
class ProviderConfigTest {

    @Autowired
    private ProviderConfig providerConfig;

    @Test

    void testProviderConfigLoaded() {
        // 验证配置已加载
        assertNotNull(providerConfig, "ProviderConfig should not be null");
        assertNotNull(providerConfig.getProviders(), "Providers map should not be null");
        
        // 输出加载的提供者信息用于调试
        Map<String, ProviderConfig.Provider> providers = providerConfig.getProviders();
        System.out.println("Loaded providers: " + providers.keySet());
        
        // 验证至少有一个提供者被加载
        assertFalse(providers.isEmpty(), "Should have at least one provider loaded");
        
        // 验证提供者的属性
        for (Map.Entry<String, ProviderConfig.Provider> entry : providers.entrySet()) {
            String providerKey = entry.getKey();
            ProviderConfig.Provider provider = entry.getValue();
            
            assertNotNull(provider.getName(), "Provider " + providerKey + " name should not be null");
            assertNotNull(provider.getType(), "Provider " + providerKey + " type should not be null");
            
            System.out.println("Provider key: " + providerKey);
            System.out.println("Provider name: " + provider.getName());
            System.out.println("Provider type: " + provider.getType());
            System.out.println("Provider apiKey: " + (provider.getApiKey() != null ? "[PROVIDED]" : "null"));
            System.out.println("Provider baseUrl: " + provider.getBaseUrl());
            System.out.println("Provider deploymentName: " + provider.getDeploymentName());
            System.out.println("Provider models: " + provider.getModels());
            System.out.println("---");
        }
    }

    @Test
    void testProviderModelsLoaded() {
        Map<String, ProviderConfig.Provider> providers = providerConfig.getProviders();
        
        // 验证每个提供者的模型信息
        for (Map.Entry<String, ProviderConfig.Provider> entry : providers.entrySet()) {
            ProviderConfig.Provider provider = entry.getValue();
            
            if (provider.getModels() != null) {
                for (ProviderConfig.Model model : provider.getModels()) {
                    assertNotNull(model.getName(), "Model name should not be null");
                    assertNotNull(model.getVendorModel(), "Model vendorModel should not be null");
                    
                    System.out.println("Provider " + provider.getName() + " has model:");
                    System.out.println("  Name: " + model.getName());
                    System.out.println("  Vendor Model: " + model.getVendorModel());
                }
            }
        }
    }
    
    @Test
    void testSpecificProviderConfig() {
        Map<String, ProviderConfig.Provider> providers = providerConfig.getProviders();
        
        // 检查OpenAI提供者配置
        ProviderConfig.Provider openai = providers.get("openai");
        if (openai != null) {
            assertEquals("openai", openai.getName());
            assertEquals("OpenAI", openai.getType());
            assertNotNull(openai.getModels());
            assertFalse(openai.getModels().isEmpty());
        }
        
        // 检查DeepSeek提供者配置
        ProviderConfig.Provider deepseek = providers.get("deepseek");
        if (deepseek != null) {
            assertEquals("deepseek", deepseek.getName());
            assertEquals("OpenAI", deepseek.getType());
            if (deepseek.getBaseUrl() != null) {
                assertEquals("https://api.deepseek.com/v1", deepseek.getBaseUrl());
            }
            assertNotNull(deepseek.getModels());
            assertFalse(deepseek.getModels().isEmpty());
        }
        
        // 检查Azure OpenAI提供者配置
        ProviderConfig.Provider azure = providers.get("azure-openai");
        if (azure != null) {
            assertEquals("azure-openai", azure.getName());
            assertEquals("AzureOpenAI", azure.getType());
            assertNotNull(azure.getDeploymentName(), "Azure OpenAI should have deployment name");
        }
        
        // 检查Claude提供者配置
        ProviderConfig.Provider claude = providers.get("claude");
        if (claude != null) {
            assertEquals("claude", claude.getName());
            assertEquals("Anthropic", claude.getType());
            if (claude.getBaseUrl() != null) {
                assertEquals("https://api.anthropic.com/v1", claude.getBaseUrl());
            }
        }
        
        // 检查Gemini提供者配置
        ProviderConfig.Provider gemini = providers.get("gemini");
        if (gemini != null) {
            assertEquals("gemini", gemini.getName());
            assertEquals("Gemini", gemini.getType());
            if (gemini.getBaseUrl() != null) {
                assertEquals("https://generativelanguage.googleapis.com/v1beta", gemini.getBaseUrl());
            }
        }
        
        // 检查Qwen提供者配置
        ProviderConfig.Provider qwen = providers.get("qwen");
        if (qwen != null) {
            assertEquals("qwen", qwen.getName());
            assertEquals("OpenAI", qwen.getType()); // Qwen使用OpenAI兼容接口
            if (qwen.getBaseUrl() != null) {
                assertEquals("https://dashscope.aliyuncs.com/api/v1", qwen.getBaseUrl());
            }
        }
    }
    
    @Test
    void testModelMapping() {
        Map<String, ProviderConfig.Provider> providers = providerConfig.getProviders();
        
        // 检查特定模型映射
        ProviderConfig.Provider openai = providers.get("openai");
        if (openai != null && openai.getModels() != null) {
            assertTrue(openai.getModels().stream()
                .anyMatch(model -> "gpt-3.5-turbo".equals(model.getName()) && 
                                  "gpt-3.5-turbo".equals(model.getVendorModel())));
        }
        
        ProviderConfig.Provider deepseek = providers.get("deepseek");
        if (deepseek != null && deepseek.getModels() != null) {
            assertTrue(deepseek.getModels().stream()
                .anyMatch(model -> "deepseek-chat".equals(model.getName()) && 
                                  "deepseek-chat".equals(model.getVendorModel())));
        }
        
        ProviderConfig.Provider claude = providers.get("claude");
        if (claude != null && claude.getModels() != null) {
            assertTrue(claude.getModels().stream()
                .anyMatch(model -> "claude-3-opus".equals(model.getName()) && 
                                  "claude-3-opus-20240229".equals(model.getVendorModel())));
        }
        
        ProviderConfig.Provider gemini = providers.get("gemini");
        if (gemini != null && gemini.getModels() != null) {
            assertTrue(gemini.getModels().stream()
                .anyMatch(model -> "gemini-pro".equals(model.getName()) && 
                                  "gemini-pro".equals(model.getVendorModel())));
        }
    }
}
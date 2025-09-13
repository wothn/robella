package org.elmo.robella.client;

/**
 * Client 构建器接口
 * 用于创建特定类型的 ApiClient 实例
 */
public interface ClientBuilder {
    
    /**
     * 构建 ApiClient 实例
     * 
     * @return ApiClient 实例
     */
    ApiClient build();
    
}
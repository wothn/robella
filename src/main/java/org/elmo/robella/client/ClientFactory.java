package org.elmo.robella.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.common.EndpointType;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Client 工厂类
 * 负责根据 EndpointType 创建对应的 ApiClient 实例
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientFactory {

    private final Map<EndpointType, ClientBuilder> clientBuilderMap;

    /**
     * 根据端点类型获取 ApiClient 实例
     * 
     * @param endpointType 端点类型
     * @return ApiClient 实例
     * @throws IllegalArgumentException 如果不支持该端点类型
     */
    public ApiClient getClient(EndpointType endpointType) {
        ClientBuilder builder = clientBuilderMap.get(endpointType);
        if (builder == null) {
            throw new IllegalArgumentException("Unsupported endpoint type: " + endpointType);
        }
        
        log.debug("Creating client for endpoint type: {}", endpointType);
        return builder.build();
    }
}
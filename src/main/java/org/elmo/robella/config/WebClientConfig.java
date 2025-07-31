package org.elmo.robella.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final WebClientProperties webClientProperties;

    @Bean
    public WebClient.Builder webClientBuilder() {
        // 从配置读取连接池参数
        WebClientProperties.ConnectionPool poolConfig = webClientProperties.getConnectionPool();
        ConnectionProvider connectionProvider = ConnectionProvider.builder("ai-provider-pool")
                .maxConnections(poolConfig.getMaxConnections())
                .maxIdleTime(poolConfig.getMaxIdleTime())
                .maxLifeTime(poolConfig.getMaxLifetime())
                .pendingAcquireTimeout(poolConfig.getAcquireTimeout())
                .evictInBackground(poolConfig.getEvictInBackground())
                .build();

        // 从配置读取超时参数
        WebClientProperties.Timeout timeoutConfig = webClientProperties.getTimeout();
        WebClientProperties.Buffer bufferConfig = webClientProperties.getBuffer();
        
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeoutConfig.getConnect().toMillis())
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(timeoutConfig.getRead().toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeoutConfig.getWrite().toSeconds(), TimeUnit.SECONDS)))
                .keepAlive(bufferConfig.isKeepAlive())  // 从配置读取
                .compress(bufferConfig.isCompress());   // 从配置读取

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize((int) bufferConfig.getMaxInMemorySize().toBytes());
                    configurer.defaultCodecs().enableLoggingRequestDetails(false);  // 禁用请求详情日志
                })
                .filter(retryFilter());
    }

    /**
     * 重试过滤器 - 针对网络异常和5xx错误进行重试
     */
    private ExchangeFilterFunction retryFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().is5xxServerError()) {
                log.warn("Server error detected, status: {}", clientResponse.statusCode());
                return Mono.error(new RuntimeException("Server error: " + clientResponse.statusCode()));
            }
            return Mono.just(clientResponse);
        });
    }

    /**
     * 创建带重试机制的 WebClient
     */
    @Bean("retryableWebClient")
    public WebClient retryableWebClient(WebClient.Builder webClientBuilder) {
        // 从配置读取重试参数
        WebClientProperties.Retry retryConfig = webClientProperties.getRetry();
        
        return webClientBuilder
                .filter((request, next) -> 
                    next.exchange(request)
                        .retryWhen(Retry.backoff(retryConfig.getMaxAttempts(), retryConfig.getInitialDelay())
                            .maxBackoff(retryConfig.getMaxDelay())
                            .filter(throwable -> {
                                // 只对网络异常和临时性错误重试
                                return throwable instanceof java.net.ConnectException ||
                                       throwable instanceof java.util.concurrent.TimeoutException ||
                                       throwable instanceof reactor.netty.http.client.PrematureCloseException ||
                                       (throwable instanceof RuntimeException && 
                                        throwable.getMessage().contains("Server error"));
                            })
                            .doBeforeRetry(retrySignal -> 
                                log.warn("Retrying request due to: {}, attempt: {}", 
                                    retrySignal.failure().getMessage(), retrySignal.totalRetries() + 1)))
                )
                .build();
    }
}
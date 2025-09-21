package org.elmo.robella.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * OkHttp配置类
 * 对应application.yml中的robella.okhttp配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "robella.okhttp")
public class OkHttpConfig {

    private ConnectionPoolConfig connectionPool = new ConnectionPoolConfig();
    private TimeoutConfig timeout = new TimeoutConfig();
    private RetryConfig retry = new RetryConfig();
    private BufferConfig buffer = new BufferConfig();

    @Data
    public static class ConnectionPoolConfig {
        private int maxIdleConnections = 50;
        private Duration keepAliveDuration = Duration.ofSeconds(300);
    }

    @Data
    public static class TimeoutConfig {
        private Duration connect = Duration.ofSeconds(10);
        private Duration read = Duration.ofSeconds(60);
        private Duration write = Duration.ofSeconds(30);
        private Duration call = Duration.ofSeconds(120);
    }

    @Data
    public static class RetryConfig {
        private boolean enabled = true;
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofSeconds(1);
        private Duration maxDelay = Duration.ofSeconds(10);
    }

    @Data
    public static class BufferConfig {
        private String maxInMemorySize = "32MB";
        private boolean enableLogging = true;
    }
}
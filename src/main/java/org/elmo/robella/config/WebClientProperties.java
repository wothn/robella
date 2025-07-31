package org.elmo.robella.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "robella.webclient")
public class WebClientProperties {

    private ConnectionPool connectionPool = new ConnectionPool();
    private Timeout timeout = new Timeout();
    private Retry retry = new Retry();
    private Buffer buffer = new Buffer();

    @Data
    public static class ConnectionPool {
        private int maxConnections = 500;
        private Duration maxIdleTime = Duration.ofSeconds(20);
        private Duration maxLifetime = Duration.ofSeconds(60);
        private Duration acquireTimeout = Duration.ofSeconds(10);
        private Duration evictInBackground = Duration.ofSeconds(120);
    }

    @Data
    public static class Timeout {
        private Duration connect = Duration.ofSeconds(10);
        private Duration read = Duration.ofSeconds(60);
        private Duration write = Duration.ofSeconds(30);
    }

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofSeconds(1);
        private Duration maxDelay = Duration.ofSeconds(10);
    }

    @Data
    public static class Buffer {
        private DataSize maxInMemorySize = DataSize.ofMegabytes(32);
        private boolean enableLoggingRequestDetails = true;
        private boolean keepAlive = true;
        private boolean compress = true;
    }
}

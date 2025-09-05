package org.elmo.robella.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

/**
 * Flyway配置类
 * 由于项目使用R2DBC，Flyway需要单独的JDBC DataSource配置
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // 确保Flyway在应用启动时正确初始化
            flyway.migrate();
        };
    }
}
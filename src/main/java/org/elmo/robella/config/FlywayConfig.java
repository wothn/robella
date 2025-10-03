package org.elmo.robella.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

/**
 * Flyway配置类
 * 确保数据库迁移在应用启动时正确执行
 * 不同数据库的迁移脚本通过 profile 配置选择不同的 locations
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
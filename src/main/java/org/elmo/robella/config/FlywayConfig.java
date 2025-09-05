package org.elmo.robella.config;

import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.flywaydb.core.Flyway;
import javax.sql.DataSource;

/**
 * Flyway配置类
 * 由于项目使用R2DBC，Flyway需要单独的JDBC DataSource配置
 */
@Configuration
public class FlywayConfig {

    @Bean
    @FlywayDataSource
    public DataSource flywayDataSource() {
        // 这里会自动使用application.yml中的r2dbc配置
        // Flyway会自动检测并使用这些配置创建JDBC连接
        return org.springframework.boot.jdbc.DataSourceBuilder.create()
                .url("jdbc:postgresql://localhost:5432/robella")
                .username(System.getenv().getOrDefault("POSTGRES_USERNAME", "postgres"))
                .password(System.getenv().getOrDefault("POSTGRES_PASSWORD", "123654789"))
                .build();
    }

    @Bean
    public Flyway flyway(DataSource flywayDataSource) {
        return Flyway.configure()
                .dataSource(flywayDataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .table("flyway_schema_history")
                .validateOnMigrate(true)
                .cleanDisabled(true)
                .load();
    }
}
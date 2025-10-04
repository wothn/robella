package org.elmo.robella.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

class DatabaseProfileConfigTest {

    @Test
    @DisplayName("Base application config sets sqlite as the default profile")
    void baseConfigShouldDefaultToSqliteProfile() throws IOException {
        Properties properties = loadYaml("application.yml");
        Assertions.assertThat(properties.getProperty("spring.profiles.default"))
                .as("default Spring profile")
                .isEqualTo("sqlite");
    }

    @Test
    @DisplayName("PostgreSQL profile exposes JDBC driver and connection settings")
    void postgresProfileShouldProvideJdbcSettings() throws IOException {
        Properties properties = loadYaml("application-postgres.yml");
        Assertions.assertThat(properties.getProperty("spring.config.activate.on-profile"))
                .isEqualTo("postgres");
        Assertions.assertThat(properties.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.postgresql.Driver");
        Assertions.assertThat(properties.getProperty("spring.datasource.url"))
                .contains("jdbc:postgresql");
    }

    @Test
    @DisplayName("SQLite profile exposes JDBC driver and file-based database path")
    void sqliteProfileShouldProvideJdbcSettings() throws IOException {
        Properties properties = loadYaml("application-sqlite.yml");
        Assertions.assertThat(properties.getProperty("spring.config.activate.on-profile"))
                .isEqualTo("sqlite");
        Assertions.assertThat(properties.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.sqlite.JDBC");
        Assertions.assertThat(properties.getProperty("spring.datasource.url"))
                .contains("jdbc:sqlite");
    }

    private Properties loadYaml(String resourceName) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourceName);
        if (!resource.exists()) {
            throw new IOException("Resource not found: " + resourceName);
        }

        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = loader.load(resourceName, resource);
        Properties properties = new Properties();
        for (PropertySource<?> propertySource : propertySources) {
            Object source = propertySource.getSource();
            if (source instanceof Map<?, ?> map) {
                map.forEach((key, value) -> {
                    if (key != null && value != null) {
                        properties.setProperty(String.valueOf(key), String.valueOf(value));
                    }
                });
            }
        }
        return properties;
    }
}

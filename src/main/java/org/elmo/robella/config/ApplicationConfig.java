package org.elmo.robella.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.elmo.robella.config.ProviderConfig;

@Configuration
@EnableConfigurationProperties({ProviderConfig.class, SecurityProperties.class})
public class ApplicationConfig {
    // 应用级配置
}
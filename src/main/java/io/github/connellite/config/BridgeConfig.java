package io.github.connellite.config;

import io.github.connellite.model.MappingRegistry;
import io.github.connellite.service.MappingLoader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(BridgeProperties.class)
public class BridgeConfig {

    @Bean
    MappingRegistry mappingRegistry(MappingLoader mappingLoader) throws IOException {
        return mappingLoader.load();
    }
}

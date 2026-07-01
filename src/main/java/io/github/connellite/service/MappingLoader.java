package io.github.connellite.service;

import io.github.connellite.config.BridgeProperties;
import io.github.connellite.model.MappingDefinition;
import io.github.connellite.model.MappingRegistry;
import io.github.connellite.service.auto.AutoMappingGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MappingLoader {

    private final BridgeProperties bridgeProperties;
    private final ResourceLoader resourceLoader;
    private final AutoMappingGenerator autoMappingGenerator;

    public MappingRegistry load() throws IOException {
        if (bridgeProperties.isAutoMode()) {
            BridgeProperties.AutoMapping auto = requireAutoConfig();
            log.info("Loading mappings in AUTO mode from {}", auto.getServicesUrl());
            List<MappingDefinition> mappings = autoMappingGenerator.generate();
            return new MappingRegistry(mappings);
        }

        String mappingsFile = bridgeProperties.getMappingsFile();
        if (!StringUtils.hasText(mappingsFile)) {
            throw new IllegalStateException("bridge.mappings-file must be set in manual mode");
        }

        log.info("Loading mappings from {}", mappingsFile);
        Resource resource = resourceLoader.getResource(mappingsFile);
        String filename = resource.getFilename() != null ? resource.getFilename() : "mappings.yml";
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(filename, resource);
        if (sources.isEmpty()) {
            throw new IllegalStateException("Empty mappings file: " + mappingsFile);
        }

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(sources.get(0));
        List<MappingDefinition> mappings = Binder.get(environment)
                .bind("mappings", Bindable.listOf(MappingDefinition.class))
                .orElseThrow(() -> new IllegalStateException("No mappings found in " + mappingsFile));
        return new MappingRegistry(mappings);
    }

    private BridgeProperties.AutoMapping requireAutoConfig() {
        BridgeProperties.AutoMapping auto = bridgeProperties.getAuto();
        if (auto == null) {
            throw new IllegalStateException("bridge.auto section is required when mappings-file is auto");
        }
        requireText(auto.getServicesUrl(), "bridge.auto.services-url");
        requireText(auto.getPathPrefix(), "bridge.auto.path-prefix");
        requireText(auto.getHttpMethod(), "bridge.auto.http-method");
        return auto;
    }

    private void requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(propertyName + " must be set when mappings-file is auto");
        }
    }
}

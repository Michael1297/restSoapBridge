package io.github.connellite.service;

import io.github.connellite.config.BridgeProperties;
import io.github.connellite.model.MappingDefinition;
import io.github.connellite.model.MappingRegistry;
import io.github.connellite.service.auto.AutoMappingGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MappingLoader {

    private final BridgeProperties bridgeProperties;
    private final AutoMappingGenerator autoMappingGenerator;

    public MappingRegistry load() throws IOException {
        BridgeProperties.AutoMapping auto = requireAutoConfig();
        log.info("Loading mappings in AUTO mode from {}", auto.getServicesUrl());
        List<MappingDefinition> mappings = autoMappingGenerator.generate();
        return new MappingRegistry(mappings);
    }

    private BridgeProperties.AutoMapping requireAutoConfig() {
        BridgeProperties.AutoMapping auto = bridgeProperties.getAuto();
        if (auto == null) {
            throw new IllegalStateException("bridge.auto section is required");
        }
        requireText(auto.getServicesUrl(), "bridge.auto.services-url");
        requireText(auto.getPathPrefix(), "bridge.auto.path-prefix");
        requireText(auto.getHttpMethod(), "bridge.auto.http-method");
        return auto;
    }

    private void requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(propertyName + " must be set");
        }
    }
}

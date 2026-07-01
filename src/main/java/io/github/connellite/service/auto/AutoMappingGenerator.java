package io.github.connellite.service.auto;

import io.github.connellite.config.BridgeProperties;
import io.github.connellite.model.DiscoveredSoapService;
import io.github.connellite.model.MappingDefinition;
import io.github.connellite.model.WsdlOperationModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoMappingGenerator {

    private final BridgeProperties bridgeProperties;
    private final WsdlServiceDiscovery serviceDiscovery;

    public List<MappingDefinition> generate() throws IOException {
        BridgeProperties.AutoMapping auto = bridgeProperties.getAuto();
        List<DiscoveredSoapService> services = serviceDiscovery.discover(auto.getServicesUrl());
        List<MappingDefinition> mappings = new ArrayList<>();

        for (DiscoveredSoapService service : services) {
            for (WsdlOperationModel operation : service.operations()) {
                mappings.add(toMapping(service, operation, auto));
            }
        }

        log.info("Generated {} automatic mapping(s)", mappings.size());
        return mappings;
    }

    private MappingDefinition toMapping(
            DiscoveredSoapService service,
            WsdlOperationModel operation,
            BridgeProperties.AutoMapping auto
    ) {
        MappingDefinition mapping = new MappingDefinition();
        mapping.setPath(buildPath(auto.getPathPrefix(), service.name(), operation.name()));
        mapping.setMethod(auto.getHttpMethod());

        MappingDefinition.SoapTarget soap = new MappingDefinition.SoapTarget();
        soap.setService(service.name());
        soap.setOperation(operation.name());
        soap.setWsdl(service.wsdlUrl());
        mapping.setSoap(soap);

        Map<String, String> request = new LinkedHashMap<>();
        for (String field : operation.inputFields()) {
            request.put("$." + field, "$." + operation.inputElement() + "." + field);
        }
        mapping.setRequest(request);

        Map<String, String> response = new LinkedHashMap<>();
        for (String field : operation.outputFields()) {
            response.put("#root." + field, "$." + field);
        }
        mapping.setResponse(response);

        return mapping;
    }

    private String buildPath(String prefix, String serviceName, String operationName) {
        String normalizedPrefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        if (!normalizedPrefix.startsWith("/")) {
            normalizedPrefix = "/" + normalizedPrefix;
        }
        return normalizedPrefix + "/" + serviceName + "/" + operationName;
    }
}

package io.github.connellite.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.connellite.mapper.PathResolver;
import io.github.connellite.mapper.path.SoapArgument;
import io.github.connellite.model.MappingDefinition;
import io.github.connellite.service.BridgeService;
import io.github.connellite.service.soap.SoapInvoker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BridgeServiceImpl implements BridgeService {

    private final SoapInvoker soapInvoker;

    @Override
    public ObjectNode execute(MappingDefinition mapping, JsonNode requestBody) {
        MappingDefinition.SoapTarget soap = mapping.getSoap();
        List<SoapArgument> arguments = PathResolver.buildSoapArguments(mapping.getRequest(), requestBody);
        Object soapResult = soapInvoker.invoke(soap.getWsdl(), soap.getOperation(), arguments);
        return toJsonBody(mapping.getResponse(), soapResult);
    }

    private ObjectNode toJsonBody(Map<String, String> responseMappings, Object soapResult) {
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, String> entry : responseMappings.entrySet()) {
            Object value = PathResolver.readFromSoapResult(soapResult, entry.getKey());
            PathResolver.writeToJsonBody(body, entry.getValue(), value);
        }
        return body;
    }
}

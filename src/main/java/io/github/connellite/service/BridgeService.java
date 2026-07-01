package io.github.connellite.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.connellite.model.MappingDefinition;

public interface BridgeService {

    ObjectNode execute(MappingDefinition mapping, JsonNode requestBody) throws Exception;
}

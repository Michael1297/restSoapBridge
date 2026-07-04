package io.github.connellite.mapper.path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonPathReader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Configuration JSON_PATH = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .build();

    public static Object read(JsonNode body, String jsonPath) {
        if (body == null || body.isNull()) {
            return null;
        }
        try {
            Object value = JsonPath.using(JSON_PATH).parse(body).read(normalize(jsonPath));
            return OBJECT_MAPPER.convertValue(value, Object.class);
        } catch (PathNotFoundException ignored) {
            return null;
        }
    }

    static String normalize(String jsonPath) {
        if (jsonPath == null || jsonPath.isBlank()) {
            return "$";
        }
        return jsonPath.startsWith("$") ? jsonPath : "$." + jsonPath;
    }
}

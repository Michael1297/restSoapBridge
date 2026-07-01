package io.github.connellite.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lombok.experimental.UtilityClass;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@UtilityClass
public final class PathResolver {

    private static final Configuration JSON_PATH = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .build();

    private static final ExpressionParser SPEL = new SpelExpressionParser();

    public static Object readFromJsonBody(JsonNode body, String jsonPath) {
        if (body == null || body.isNull()) {
            return null;
        }
        return readJsonValue(body, jsonPath);
    }

    public static void writeToJsonBody(ObjectNode body, String jsonPath, Object value) {
        writeJsonValue(body, jsonPath, value);
    }

    public static Object readFromSoapResult(Object soapResult, String spelExpression) {
        if (soapResult == null) {
            return null;
        }
        Object root = unwrapSoapArray(soapResult);
        StandardEvaluationContext context = new StandardEvaluationContext(root);
        context.setRootObject(root);
        try {
            return SPEL.parseExpression(spelExpression).getValue(context);
        } catch (EvaluationException exception) {
            throw new IllegalStateException(
                    "Cannot evaluate SpEL '" + spelExpression + "' on " + root.getClass().getName(),
                    exception);
        }
    }

    public static List<SoapArgument> buildSoapArguments(Map<String, String> requestMappings, JsonNode body) {
        if (requestMappings.isEmpty()) {
            return List.of();
        }

        DocumentContext soapContext = JsonPath.using(JSON_PATH).parse("{}");
        for (Map.Entry<String, String> entry : requestMappings.entrySet()) {
            soapContext.set(entry.getValue(), readFromJsonBody(body, entry.getKey()));
        }

        Map<String, Object> soapFields = soapContext.read("$");
        if (soapFields.isEmpty()) {
            return List.of();
        }

        if (soapFields.size() == 1) {
            Map.Entry<String, Object> only = soapFields.entrySet().iterator().next();
            if (only.getValue() instanceof Map<?, ?> nested) {
                return toOrderedArguments(nested);
            }
        }

        return toOrderedArguments(soapFields);
    }

    private static Object unwrapSoapArray(Object soapResult) {
        if (soapResult.getClass().isArray()) {
            Object[] array = (Object[]) soapResult;
            if (array.length == 1) {
                return array[0];
            }
        }
        return soapResult;
    }

    private static Object readJsonValue(JsonNode body, String jsonPath) {
        try {
            return JsonPath.using(JSON_PATH).parse(body).read(normalizeJsonPath(jsonPath));
        } catch (PathNotFoundException ignored) {
            return null;
        }
    }

    private static void writeJsonValue(ObjectNode body, String jsonPath, Object value) {
        JsonPath.using(JSON_PATH).parse(body).set(normalizeJsonPath(jsonPath), value);
    }

    private static String normalizeJsonPath(String jsonPath) {
        if (jsonPath == null || jsonPath.isBlank()) {
            return "$";
        }
        if (jsonPath.startsWith("$")) {
            return jsonPath;
        }
        return "$." + jsonPath;
    }

    private static List<SoapArgument> toOrderedArguments(Map<?, ?> fields) {
        List<SoapArgument> arguments = new ArrayList<>();
        for (Map.Entry<?, ?> entry : fields.entrySet()) {
            arguments.add(new SoapArgument(String.valueOf(entry.getKey()), entry.getValue()));
        }
        return arguments;
    }

    public record SoapArgument(String name, Object value) {
    }
}

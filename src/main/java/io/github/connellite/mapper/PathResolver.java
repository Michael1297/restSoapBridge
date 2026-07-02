package io.github.connellite.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lombok.experimental.UtilityClass;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import jakarta.activation.DataHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public final class PathResolver {

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

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
        Object root = unwrapSoapContainer(soapResult);
        StandardEvaluationContext context = new StandardEvaluationContext(root);
        context.setRootObject(root);
        context.addPropertyAccessor(new MapAccessor());
        context.addPropertyAccessor(new SyntheticSoapWrapperAccessor());
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

        Map<String, Object> soapRoot = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : requestMappings.entrySet()) {
            setNestedValue(soapRoot, entry.getValue(), readFromJsonBody(body, entry.getKey()));
        }

        if (soapRoot.isEmpty()) {
            return List.of();
        }

        if (soapRoot.size() == 1) {
            Map.Entry<String, Object> only = soapRoot.entrySet().iterator().next();
            if (only.getValue() instanceof Map<?, ?> nested) {
                return toOrderedArguments(nested);
            }
        }

        return toOrderedArguments(soapRoot);
    }

    private static Object unwrapSoapContainer(Object soapResult) {
        if (soapResult.getClass().isArray()) {
            Object[] array = (Object[]) soapResult;
            if (array.length == 1) {
                return unwrapSoapContainer(array[0]);
            }
        }
        if (soapResult instanceof Collection<?> collection && collection.size() == 1) {
            return unwrapSoapContainer(collection.iterator().next());
        }
        return soapResult;
    }

    private static Object readJsonValue(JsonNode body, String jsonPath) {
        try {
            Object value = JsonPath.using(JSON_PATH).parse(body).read(normalizeJsonPath(jsonPath));
            return unwrapJsonValue(value);
        } catch (PathNotFoundException ignored) {
            return null;
        }
    }

    private static Object unwrapJsonValue(Object value) {
        return OBJECT_MAPPER.convertValue(value, Object.class);
    }

    private static void writeJsonValue(ObjectNode body, String jsonPath, Object value) {
        String relative = toRelativePath(jsonPath);
        if (relative.isEmpty()) {
            return;
        }

        String[] segments = relative.split("\\.");
        ObjectNode current = body;
        for (int index = 0; index < segments.length - 1; index++) {
            String segment = stripArraySuffix(segments[index]);
            JsonNode child = current.get(segment);
            if (!(child instanceof ObjectNode)) {
                child = OBJECT_MAPPER.createObjectNode();
                current.set(segment, child);
            }
            current = (ObjectNode) child;
        }

        current.set(stripArraySuffix(segments[segments.length - 1]), toJsonNode(value));
    }

    private static JsonNode toJsonNode(Object value) {
        return OBJECT_MAPPER.valueToTree(value);
    }

    private static ObjectMapper createObjectMapper() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(DataHandler.class, new DataHandlerSerializer());
        return new ObjectMapper().registerModule(module);
    }

    @SuppressWarnings("unchecked")
    private static void setNestedValue(Map<String, Object> root, String jsonPath, Object value) {
        String relative = toRelativePath(jsonPath);
        if (relative.isEmpty()) {
            if (value instanceof Map<?, ?> map) {
                root.putAll((Map<String, Object>) map);
            }
            return;
        }

        String[] segments = relative.split("\\.");
        Map<String, Object> current = root;
        for (int index = 0; index < segments.length - 1; index++) {
            String segment = stripArraySuffix(segments[index]);
            Object child = current.get(segment);
            if (!(child instanceof Map<?, ?>)) {
                child = new LinkedHashMap<String, Object>();
                current.put(segment, child);
            }
            current = (Map<String, Object>) child;
        }

        String lastSegment = stripArraySuffix(segments[segments.length - 1]);
        current.put(lastSegment, value);
    }

    private static String toRelativePath(String jsonPath) {
        String normalized = normalizeJsonPath(jsonPath);
        if ("$".equals(normalized)) {
            return "";
        }
        return normalized.startsWith("$.") ? normalized.substring(2) : normalized;
    }

    private static String stripArraySuffix(String segment) {
        int bracketIndex = segment.indexOf('[');
        return bracketIndex >= 0 ? segment.substring(0, bracketIndex) : segment;
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

    private static final class SyntheticSoapWrapperAccessor implements PropertyAccessor {

        private static final List<String> WRAPPER_FIELDS = List.of("return", "result");

        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return null;
        }

        @Override
        public boolean canRead(EvaluationContext context, Object target, String name) {
            return target != null
                    && !(target instanceof Map<?, ?>)
                    && WRAPPER_FIELDS.contains(name)
                    && !new BeanWrapperImpl(target).isReadableProperty(name);
        }

        @Override
        public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
            if (!canRead(context, target, name)) {
                throw new AccessException("Synthetic SOAP wrapper field is not readable: " + name);
            }
            return new TypedValue(target);
        }

        @Override
        public boolean canWrite(EvaluationContext context, Object target, String name) {
            return false;
        }

        @Override
        public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
            throw new AccessException("Synthetic SOAP wrapper fields are read-only");
        }
    }

    private static final class DataHandlerSerializer extends JsonSerializer<DataHandler> {

        @Override
        public void serialize(DataHandler value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
            generator.writeStartObject();
            generator.writeStringField("contentType", value.getContentType());
            generator.writeStringField("name", value.getName());
            generator.writeFieldName("content");
            try (var inputStream = value.getInputStream()) {
                generator.writeBinary(inputStream.readAllBytes());
            }
            generator.writeEndObject();
        }
    }
}

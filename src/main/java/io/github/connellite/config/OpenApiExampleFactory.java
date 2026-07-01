package io.github.connellite.config;

import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import lombok.experimental.UtilityClass;
import net.datafaker.Faker;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@UtilityClass
public class OpenApiExampleFactory {

    private static final Faker FAKER = new Faker();

    public static ObjectSchema schemaWithExamples(Iterable<String> bodyPaths) {
        Map<String, Object> example = exampleMap(bodyPaths);
        ObjectSchema schema = new ObjectSchema();
        for (Map.Entry<String, Object> entry : example.entrySet()) {
            schema.addProperty(entry.getKey(), propertySchema(entry.getValue()));
        }
        schema.example(example);
        return schema;
    }

    public static Map<String, Object> exampleMap(Iterable<String> bodyPaths) {
        Map<String, Object> example = new LinkedHashMap<>();
        for (String path : bodyPaths) {
            String field = extractBodyField(path);
            if (!field.isBlank()) {
                example.put(field, sampleValue(field));
            }
        }
        return example;
    }

    private static Schema<?> propertySchema(Object sample) {
        if (sample instanceof Integer) {
            IntegerSchema schema = new IntegerSchema();
            schema.setExample(sample);
            return schema;
        }
        if (sample instanceof Boolean) {
            BooleanSchema schema = new BooleanSchema();
            schema.setExample(sample);
            return schema;
        }
        StringSchema schema = new StringSchema();
        schema.setExample(String.valueOf(sample));
        return schema;
    }

    public static Object sampleValue(String fieldName) {
        String normalized = fieldName.toLowerCase(Locale.ROOT);
        if (normalized.equals("user") || normalized.equals("username") || normalized.equals("login")) {
            return FAKER.internet().username();
        }
        if (normalized.equals("pass") || normalized.equals("password")) {
            return FAKER.internet().password();
        }
        if (normalized.equals("token") || normalized.equals("sid") || normalized.equals("sessionid")) {
            return FAKER.internet().uuid();
        }
        if (normalized.equals("return") || normalized.equals("result")) {
            return "ok";
        }
        if (normalized.endsWith("uuid") || normalized.equals("uuid") || normalized.equals("id")) {
            return FAKER.internet().uuid();
        }
        if (normalized.contains("email")) {
            return FAKER.internet().emailAddress();
        }
        if (normalized.contains("name")) {
            return FAKER.name().fullName();
        }
        if (normalized.contains("date") || normalized.contains("time")) {
            return Instant.now().toString();
        }
        if (normalized.contains("count") || normalized.contains("number") || normalized.contains("size")) {
            return FAKER.number().numberBetween(1, 100);
        }
        if (normalized.startsWith("is") || normalized.startsWith("has") || normalized.contains("enabled")) {
            return FAKER.bool().bool();
        }
        if (normalized.contains("url") || normalized.contains("uri") || normalized.contains("path")) {
            return FAKER.internet().url();
        }
        return FAKER.lorem().word();
    }

    public static String extractBodyField(String jsonPath) {
        if (jsonPath == null || jsonPath.isBlank()) {
            return "";
        }
        String path = jsonPath.startsWith("$.") ? jsonPath.substring(2) : jsonPath;
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }
}

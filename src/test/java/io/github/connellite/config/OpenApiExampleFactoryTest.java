package io.github.connellite.config;

import io.github.connellite.model.SchemaField;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class OpenApiExampleFactoryTest {

    @Test
    void exampleMap_buildsNestedObjectFromDottedPaths() {
        Map<String, Object> example = OpenApiExampleFactory.exampleMap(List.of("$.customer.name", "$.customer.phone"));

        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) example.get("customer");
        assertEquals(2, customer.size());
        assertEquals(true, customer.containsKey("name"));
        assertEquals(true, customer.containsKey("phone"));
    }

    @Test
    void exampleMap_buildsCollectionItemExample() {
        Map<String, Object> example = OpenApiExampleFactory.exampleMap(List.of("$.products[].sku", "$.products[].qty"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) example.get("products");
        assertEquals(1, products.size());
        assertEquals(true, products.get(0).containsKey("sku"));
        assertEquals(true, products.get(0).containsKey("qty"));
    }

    @Test
    void exampleMap_treatsRemovedFieldsAsBooleanFallback() {
        Map<String, Object> example = OpenApiExampleFactory.exampleMap(List.of("$.searchRemoved"));

        assertInstanceOf(Boolean.class, example.get("searchRemoved"));
    }

    @Test
    void schemaWithXsdExamples_usesTypesFromXsdMetadata() {
        ObjectSchema schema = OpenApiExampleFactory.schemaWithXsdExamples(List.of(
                new SchemaField("$.customer.name", "string", true, false),
                new SchemaField("$.customer.age", "int", true, false),
                new SchemaField("$.customer.enabled", "boolean", false, false),
                new SchemaField("$.searchRemoved", "boolean", false, false),
                new SchemaField("$.products", "object", true, true),
                new SchemaField("$.products[].sku", "string", true, false),
                new SchemaField("$.products[].qty", "int", true, false)
        ), List.of());

        Schema<?> customerSchema = schema.getProperties().get("customer");
        assertEquals(ObjectSchema.class, customerSchema.getClass());
        @SuppressWarnings("unchecked")
        Map<String, Schema> customerProperties = ((ObjectSchema) customerSchema).getProperties();
        assertEquals(IntegerSchema.class, customerProperties.get("age").getClass());
        assertEquals(BooleanSchema.class, customerProperties.get("enabled").getClass());
        assertEquals(BooleanSchema.class, schema.getProperties().get("searchRemoved").getClass());

        @SuppressWarnings("unchecked")
        Map<String, Object> example = (Map<String, Object>) schema.getExample();
        assertInstanceOf(Boolean.class, example.get("searchRemoved"));

        Schema<?> productsSchema = schema.getProperties().get("products");
        assertEquals(ArraySchema.class, productsSchema.getClass());
        Schema<?> productItemSchema = ((ArraySchema) productsSchema).getItems();
        @SuppressWarnings("unchecked")
        Map<String, Schema> productProperties = ((ObjectSchema) productItemSchema).getProperties();
        assertEquals(IntegerSchema.class, productProperties.get("qty").getClass());
    }
}

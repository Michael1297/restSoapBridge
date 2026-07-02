package io.github.connellite.model;

public record SchemaField(
        String path,
        String xsdType,
        boolean required,
        boolean collection
) {
}

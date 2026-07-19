package io.github.connellite.mapper.path;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
#if SPRING_BOOT_3
import jakarta.activation.DataHandler;
#else
import javax.activation.DataHandler;
#endif

import java.io.IOException;

final class DataHandlerSerializer extends JsonSerializer<DataHandler> {

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

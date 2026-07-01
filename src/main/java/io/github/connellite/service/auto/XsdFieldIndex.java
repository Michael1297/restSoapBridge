package io.github.connellite.service.auto;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.w3c.dom.Element;

import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.extensions.schema.Schema;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class XsdFieldIndex {

    private XsdFieldIndex() {
    }

    static Map<String, List<String>> readElementFields(Definition definition) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        Types types = definition.getTypes();
        if (types == null) {
            return result;
        }

        XmlSchemaCollection collection = new XmlSchemaCollection();
        for (Object extension : types.getExtensibilityElements()) {
            if (extension instanceof Schema wsdlSchema) {
                Element schemaElement = wsdlSchema.getElement();
                XmlSchema xmlSchema = collection.read(schemaElement, wsdlSchema.getDocumentBaseURI());
                indexSchema(xmlSchema, result);
            }
        }
        return result;
    }

    private static void indexSchema(XmlSchema schema, Map<String, List<String>> result) {
        for (XmlSchemaType type : schema.getSchemaTypes().values()) {
            if (type instanceof XmlSchemaComplexType complexType && complexType.getName() != null) {
                List<String> fields = sequenceFieldNames(complexType);
                if (!fields.isEmpty()) {
                    result.put(complexType.getName(), fields);
                }
            }
        }

        for (XmlSchemaElement element : schema.getElements().values()) {
            if (element.getName() == null) {
                continue;
            }
            XmlSchemaComplexType complexType = resolveComplexType(element, schema);
            if (complexType == null) {
                continue;
            }
            List<String> fields = sequenceFieldNames(complexType);
            if (!fields.isEmpty()) {
                result.putIfAbsent(element.getName(), fields);
            }
        }
    }

    private static XmlSchemaComplexType resolveComplexType(XmlSchemaElement element, XmlSchema schema) {
        XmlSchemaType inlineType = element.getSchemaType();
        if (inlineType instanceof XmlSchemaComplexType complexType) {
            return complexType;
        }
        if (element.getSchemaTypeName() == null) {
            return null;
        }
        XmlSchemaType resolvedType = schema.getTypeByName(element.getSchemaTypeName());
        return resolvedType instanceof XmlSchemaComplexType complexType ? complexType : null;
    }

    private static List<String> sequenceFieldNames(XmlSchemaComplexType complexType) {
        Set<String> fields = new LinkedHashSet<>();
        collectElementNames(complexType.getParticle(), fields);
        return List.copyOf(fields);
    }

    private static void collectElementNames(XmlSchemaParticle particle, Set<String> fields) {
        if (particle == null) {
            return;
        }
        if (particle instanceof XmlSchemaElement element && element.getName() != null) {
            fields.add(element.getName());
            return;
        }
        if (particle instanceof XmlSchemaSequence sequence) {
            for (XmlSchemaSequenceMember item : sequence.getItems()) {
                if (item instanceof XmlSchemaElement child && child.getName() != null) {
                    fields.add(child.getName());
                }
            }
        }
    }
}

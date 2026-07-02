package io.github.connellite.service.auto;

import io.github.connellite.model.SchemaField;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAllMember;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaChoiceMember;
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
import javax.xml.namespace.QName;
import java.util.ArrayList;
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
        readElementSchemaFields(definition).forEach((element, fields) ->
                result.put(element, fields.stream().map(SchemaField::path).toList()));
        return result;
    }

    static Map<String, List<SchemaField>> readElementSchemaFields(Definition definition) {
        Map<String, List<SchemaField>> schemaResult = new LinkedHashMap<>();
        Types types = definition.getTypes();
        if (types == null) {
            return schemaResult;
        }

        XmlSchemaCollection collection = new XmlSchemaCollection();
        for (Object extension : types.getExtensibilityElements()) {
            if (extension instanceof Schema wsdlSchema) {
                Element schemaElement = wsdlSchema.getElement();
                XmlSchema xmlSchema = collection.read(schemaElement, wsdlSchema.getDocumentBaseURI());
                indexSchema(xmlSchema, schemaResult);
            }
        }
        return schemaResult;
    }

    private static void indexSchema(XmlSchema schema, Map<String, List<SchemaField>> result) {
        for (XmlSchemaType type : schema.getSchemaTypes().values()) {
            if (type instanceof XmlSchemaComplexType complexType && complexType.getName() != null) {
                List<SchemaField> fields = fieldPaths(complexType, schema, "", true, new LinkedHashSet<>());
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
            List<SchemaField> fields = fieldPaths(complexType, schema, "", true, new LinkedHashSet<>());
            if (!fields.isEmpty()) {
                result.putIfAbsent(element.getName(), fields);
            }
        }
    }

    private static XmlSchemaComplexType resolveComplexType(XmlSchemaElement element, XmlSchema schema) {
        XmlSchemaElement resolvedElement = resolveElementReference(element);
        if (resolvedElement != element) {
            return resolveComplexType(resolvedElement, schema);
        }
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

    private static XmlSchemaElement resolveElementReference(XmlSchemaElement element) {
        if (!element.isRef()) {
            return element;
        }
        XmlSchemaElement target = element.getRef().getTarget();
        return target != null ? target : element;
    }

    private static List<SchemaField> fieldPaths(
            XmlSchemaComplexType complexType,
            XmlSchema schema,
            String prefix,
            boolean parentRequired,
            Set<String> visiting
    ) {
        List<SchemaField> fields = new ArrayList<>();
        collectFieldPaths(complexType.getParticle(), schema, prefix, parentRequired, fields, visiting);
        return fields;
    }

    private static void collectFieldPaths(
            XmlSchemaParticle particle,
            XmlSchema schema,
            String prefix,
            boolean parentRequired,
            List<SchemaField> fields,
            Set<String> visiting
    ) {
        if (particle == null) {
            return;
        }
        if (particle instanceof XmlSchemaElement element && element.getName() != null) {
            collectElementField(element, schema, prefix, parentRequired, fields, visiting);
            return;
        }
        if (particle instanceof XmlSchemaSequence sequence) {
            for (XmlSchemaSequenceMember item : sequence.getItems()) {
                collectSequenceMember(item, schema, prefix, parentRequired, fields, visiting);
            }
        }
        if (particle instanceof XmlSchemaChoice choice) {
            for (XmlSchemaChoiceMember item : choice.getItems()) {
                collectChoiceMember(item, schema, prefix, false, fields, visiting);
            }
        }
        if (particle instanceof XmlSchemaAll all) {
            for (XmlSchemaAllMember item : all.getItems()) {
                collectAllMember(item, schema, prefix, parentRequired, fields, visiting);
            }
        }
    }

    private static void collectSequenceMember(
            XmlSchemaSequenceMember item,
            XmlSchema schema,
            String prefix,
            boolean parentRequired,
            List<SchemaField> fields,
            Set<String> visiting
    ) {
        if (item instanceof XmlSchemaElement child) {
            collectElementField(child, schema, prefix, parentRequired, fields, visiting);
        } else if (item instanceof XmlSchemaParticle particle) {
            collectFieldPaths(particle, schema, prefix, parentRequired, fields, visiting);
        }
    }

    private static void collectChoiceMember(
            XmlSchemaChoiceMember item,
            XmlSchema schema,
            String prefix,
            boolean parentRequired,
            List<SchemaField> fields,
            Set<String> visiting
    ) {
        if (item instanceof XmlSchemaElement child) {
            collectElementField(child, schema, prefix, parentRequired, fields, visiting);
        } else if (item instanceof XmlSchemaParticle particle) {
            collectFieldPaths(particle, schema, prefix, parentRequired, fields, visiting);
        }
    }

    private static void collectAllMember(
            XmlSchemaAllMember item,
            XmlSchema schema,
            String prefix,
            boolean parentRequired,
            List<SchemaField> fields,
            Set<String> visiting
    ) {
        if (item instanceof XmlSchemaElement child) {
            collectElementField(child, schema, prefix, parentRequired, fields, visiting);
        } else if (item instanceof XmlSchemaParticle particle) {
            collectFieldPaths(particle, schema, prefix, parentRequired, fields, visiting);
        }
    }

    private static void collectElementField(
            XmlSchemaElement element,
            XmlSchema schema,
            String prefix,
            boolean parentRequired,
            List<SchemaField> fields,
            Set<String> visiting
    ) {
        XmlSchemaElement resolvedElement = resolveElementReference(element);
        String name = element.getName() != null ? element.getName() : resolvedElement.getName();
        if (name == null) {
            return;
        }

        String path = prefix.isEmpty() ? name : prefix + name;
        boolean collection = isRepeating(element);
        boolean required = parentRequired && element.getMinOccurs() > 0;
        XmlSchemaComplexType complexType = resolveComplexType(resolvedElement, schema);

        if (complexType != null) {
            String typeKey = complexType.getName() != null ? complexType.getName() : path;
            if (!visiting.add(typeKey)) {
                fields.add(new SchemaField(collection ? path + "[]" : path, "object", required, collection));
                return;
            }

            List<SchemaField> nestedFields = new ArrayList<>();
            String nestedPrefix = collection ? path + "[]." : path + ".";
            collectFieldPaths(complexType.getParticle(), schema, nestedPrefix, required, nestedFields, visiting);
            visiting.remove(typeKey);

            if (collection) {
                fields.add(new SchemaField(path + "[]", "object", required, true));
            }
            if (!nestedFields.isEmpty()) {
                fields.addAll(nestedFields);
            } else if (!collection) {
                fields.add(new SchemaField(path, "object", required, false));
            }
            return;
        }

        fields.add(new SchemaField(collection ? path + "[]" : path, xsdTypeName(resolvedElement), required, collection));
    }

    private static boolean isRepeating(XmlSchemaElement element) {
        long maxOccurs = element.getMaxOccurs();
        return maxOccurs > 1 || maxOccurs == Long.MAX_VALUE;
    }

    private static String xsdTypeName(XmlSchemaElement element) {
        QName typeName = element.getSchemaTypeName();
        if (typeName != null) {
            return typeName.getLocalPart();
        }
        XmlSchemaType schemaType = element.getSchemaType();
        if (schemaType != null && schemaType.getQName() != null) {
            return schemaType.getQName().getLocalPart();
        }
        if (schemaType != null && schemaType.getName() != null) {
            return schemaType.getName();
        }
        return "string";
    }
}

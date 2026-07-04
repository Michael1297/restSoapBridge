package io.github.connellite.service.auto.xsd;

import lombok.experimental.UtilityClass;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaType;

@UtilityClass
public class ComplexTypeResolver {

    public static XmlSchemaComplexType resolve(XmlSchemaElement element, XmlSchema schema) {
        XmlSchemaElement resolvedElement = resolveElementReference(element);
        if (resolvedElement != element) {
            return resolve(resolvedElement, schema);
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

    public static XmlSchemaElement resolveElementReference(XmlSchemaElement element) {
        if (!element.isRef()) {
            return element;
        }
        XmlSchemaElement target = element.getRef().getTarget();
        return target != null ? target : element;
    }
}

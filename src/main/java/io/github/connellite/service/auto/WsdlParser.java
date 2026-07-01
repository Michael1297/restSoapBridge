package io.github.connellite.service.auto;

import io.github.connellite.model.WsdlOperationModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.wsdl.Definition;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WsdlParser {

    public List<WsdlOperationModel> parseOperations(String wsdlUrl) {
        try {
            WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
            reader.setFeature("javax.wsdl.verbose", false);
            Definition definition = reader.readWSDL(wsdlUrl);

            Map<String, String> messageToElement = readMessageElements(definition);
            Map<String, List<String>> elementFields = XsdFieldIndex.readElementFields(definition);

            List<WsdlOperationModel> result = new ArrayList<>();
            for (Object value : definition.getAllPortTypes().values()) {
                PortType portType = (PortType) value;
                for (Object operationValue : portType.getOperations()) {
                    Operation operation = (Operation) operationValue;
                    String operationName = operation.getName();
                    if (operationName == null || operationName.isBlank()) {
                        continue;
                    }

                    String inputMessage = messageName(operation.getInput());
                    String outputMessage = messageName(operation.getOutput());
                    String inputElement = messageToElement.getOrDefault(inputMessage, operationName);
                    String outputElement = messageToElement.getOrDefault(outputMessage, operationName + "Response");

                    List<String> inputFields = elementFields.getOrDefault(inputElement, List.of());
                    List<String> outputFields = elementFields.getOrDefault(outputElement, List.of());
                    result.add(new WsdlOperationModel(
                            operationName,
                            inputElement,
                            inputFields,
                            outputElement,
                            outputFields
                    ));
                }
            }
            return result;
        } catch (WSDLException exception) {
            throw new IllegalStateException("Failed to parse WSDL: " + wsdlUrl, exception);
        }
    }

    private Map<String, String> readMessageElements(Definition definition) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Object value : definition.getMessages().values()) {
            Message message = (Message) value;
            if (message.getParts().isEmpty()) {
                continue;
            }
            Part part = (Part) message.getParts().values().iterator().next();
            QName elementName = part.getElementName();
            if (elementName != null) {
                result.put(message.getQName().getLocalPart(), elementName.getLocalPart());
            }
        }
        return result;
    }

    private String messageName(Input input) {
        if (input == null || input.getMessage() == null) {
            return "";
        }
        return input.getMessage().getQName().getLocalPart();
    }

    private String messageName(Output output) {
        if (output == null || output.getMessage() == null) {
            return "";
        }
        return output.getMessage().getQName().getLocalPart();
    }
}

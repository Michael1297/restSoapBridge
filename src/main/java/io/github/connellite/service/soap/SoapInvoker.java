package io.github.connellite.service.soap;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.connellite.exception.SoapFaultException;
import io.github.connellite.mapper.path.SoapArgument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class SoapInvoker {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

    private final SoapClientPool clientPool;

    public Object invoke(String wsdlUrl, String operation, List<SoapArgument> arguments) {
        Client client = clientPool.getClient(wsdlUrl);
        Object[] params = convertArguments(client, operation, arguments);
        log.debug("Invoking SOAP operation '{}' with {} parameter(s)", operation, params.length);
        try {
            Object[] response = client.invoke(operation, params);
            if (response == null) {
                return null;
            }
            if (response.length == 1) {
                return response[0];
            }
            return response;
        } catch (Exception exception) {
            throw SoapFaultException.from(exception);
        }
    }

    private Object[] convertArguments(Client client, String operation, List<SoapArgument> arguments) {
        List<Class<?>> parameterTypes = parameterTypes(client, operation, arguments.size());
        Object[] params = new Object[arguments.size()];
        for (int index = 0; index < arguments.size(); index++) {
            Object value = arguments.get(index).value();
            Class<?> targetType = index < parameterTypes.size() ? parameterTypes.get(index) : null;
            params[index] = convertArgument(value, targetType);
        }
        return params;
    }

    private List<Class<?>> parameterTypes(Client client, String operation, int argumentCount) {
        BindingOperationInfo operationInfo = findOperation(client, operation);
        if (operationInfo == null) {
            return List.of();
        }

        if (operationInfo.isUnwrappedCapable()) {
            List<Class<?>> unwrappedTypes = inputPartTypes(operationInfo.getUnwrappedOperation());
            if (unwrappedTypes.size() == argumentCount) {
                return unwrappedTypes;
            }
        }

        List<Class<?>> wrappedTypes = inputPartTypes(operationInfo);
        if (wrappedTypes.size() == argumentCount) {
            return wrappedTypes;
        }
        return List.of();
    }

    private BindingOperationInfo findOperation(Client client, String operation) {
        return client.getEndpoint().getEndpointInfo().getBinding().getOperations().stream()
                .filter(bindingOperation -> operation.equals(bindingOperation.getName().getLocalPart()))
                .findFirst()
                .orElse(null);
    }

    private List<Class<?>> inputPartTypes(BindingOperationInfo operationInfo) {
        if (operationInfo == null || operationInfo.getInput() == null) {
            return List.of();
        }
        List<Class<?>> types = new ArrayList<>();
        for (MessagePartInfo part : operationInfo.getInput().getMessageParts()) {
            types.add(part.getTypeClass());
        }
        return types.stream().filter(Objects::nonNull).toList();
    }

    static Object convertArgument(Object value, Class<?> targetType) {
        if (value == null || targetType == null || targetType.isInstance(value)) {
            return value;
        }
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            return OBJECT_MAPPER.convertValue(value, targetType);
        }
        if (isSimpleType(targetType)) {
            return OBJECT_MAPPER.convertValue(value, targetType);
        }
        return value;
    }

    private static boolean isSimpleType(Class<?> targetType) {
        return targetType.isPrimitive()
                || Number.class.isAssignableFrom(targetType)
                || CharSequence.class.isAssignableFrom(targetType)
                || Boolean.class == targetType
                || Character.class == targetType
                || Enum.class.isAssignableFrom(targetType);
    }
}

package io.github.connellite.service.soap;

import io.github.connellite.exception.SoapFaultException;
import io.github.connellite.mapper.PathResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.endpoint.Client;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SoapInvoker {

    private final SoapClientPool clientPool;

    public Object invoke(String wsdlUrl, String operation, List<PathResolver.SoapArgument> arguments) {
        Client client = clientPool.getClient(wsdlUrl);
        Object[] params = arguments.stream().map(PathResolver.SoapArgument::value).toArray();
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
}

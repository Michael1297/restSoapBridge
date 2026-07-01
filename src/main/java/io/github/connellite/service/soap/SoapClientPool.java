package io.github.connellite.service.soap;

import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SoapClientPool {

    private final Map<String, Client> clients = new ConcurrentHashMap<>();

    public Client getClient(String wsdlUrl) {
        return clients.computeIfAbsent(wsdlUrl, this::createClient);
    }

    private Client createClient(String wsdlUrl) {
        log.info("Creating dynamic SOAP client for WSDL: {}", wsdlUrl);
        JaxWsDynamicClientFactory factory = JaxWsDynamicClientFactory.newInstance();
        return factory.createClient(wsdlUrl);
    }
}

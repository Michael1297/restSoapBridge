package io.github.connellite.service.soap;

import io.github.connellite.config.BridgeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SoapClientPool {

    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    private final BridgeProperties bridgeProperties;

    public Client getClient(String wsdlUrl) {
        return clients.computeIfAbsent(wsdlUrl, this::createClient);
    }

    private Client createClient(String wsdlUrl) {
        log.info("Creating dynamic SOAP client for WSDL: {}", wsdlUrl);
        JaxWsDynamicClientFactory factory = JaxWsDynamicClientFactory.newInstance();
        Client client = factory.createClient(wsdlUrl);
        configureTimeouts(client);
        return client;
    }

    private void configureTimeouts(Client client) {
        if (!(client.getConduit() instanceof HTTPConduit conduit)) {
            return;
        }
        BridgeProperties.AutoMapping auto = bridgeProperties.getAuto();
        if (auto == null) {
            return;
        }
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setConnectionTimeout(auto.getConnectTimeout().toMillis());
        policy.setReceiveTimeout(auto.getReceiveTimeout().toMillis());
        conduit.setClient(policy);
    }
}

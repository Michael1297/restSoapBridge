package io.github.connellite.service.auto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WsdlServiceDiscoveryTest {

    @Test
    void serviceNameFromWsdlUrl_extractsServiceNameBeforeQuery() {
        assertEquals(
                "Auth",
                WsdlServiceDiscovery.serviceNameFromWsdlUrl("http://127.0.0.1:8080/services/Auth?wsdl")
        );
    }

    @Test
    void serviceNameFromWsdlUrl_stripsWsdlFileExtension() {
        assertEquals(
                "OrderService",
                WsdlServiceDiscovery.serviceNameFromWsdlUrl("http://example.com/wsdl/OrderService.wsdl")
        );
    }
}

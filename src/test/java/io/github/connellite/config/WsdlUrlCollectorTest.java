package io.github.connellite.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WsdlUrlCollectorTest {

    @Test
    void collectsWsdlUrlPropertiesWithOptionalSuffix() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("bridge.auto.wsdl-url", "http://localhost/Auth?wsdl")
                .withProperty("bridge.auto.wsdl-1-url", "http://localhost/One?wsdl")
                .withProperty("bridge.auto.wsdl-auth-url", "http://localhost/Auth2?wsdl")
                .withProperty("bridge.auto.services-url", "http://localhost/services/");

        WsdlUrlCollector collector = new WsdlUrlCollector(environment);

        assertEquals(
                3,
                collector.wsdlUrls().size()
        );
        assertEquals(
                java.util.Set.of(
                        "http://localhost/Auth?wsdl",
                        "http://localhost/One?wsdl",
                        "http://localhost/Auth2?wsdl"
                ),
                new java.util.LinkedHashSet<>(collector.wsdlUrls())
        );
    }
}

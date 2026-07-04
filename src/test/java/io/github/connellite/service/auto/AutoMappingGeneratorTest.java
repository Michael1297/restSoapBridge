package io.github.connellite.service.auto;

import io.github.connellite.config.BridgeProperties;
import io.github.connellite.config.WsdlUrlCollector;
import io.github.connellite.model.DiscoveredSoapService;
import io.github.connellite.model.MappingDefinition;
import io.github.connellite.model.SchemaField;
import io.github.connellite.model.WsdlOperationModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutoMappingGeneratorTest {

    @Test
    void generate_mapsNestedFieldsAndCollectionParents() throws Exception {
        BridgeProperties properties = new BridgeProperties();
        BridgeProperties.AutoMapping auto = new BridgeProperties.AutoMapping();
        auto.setServicesUrl("http://localhost:8080/services/");
        auto.setPathPrefix("api");
        auto.setHttpMethod("POST");
        properties.setAuto(auto);

        WsdlServiceDiscovery discovery = mock(WsdlServiceDiscovery.class);
        WsdlUrlCollector wsdlUrlCollector = mock(WsdlUrlCollector.class);
        when(wsdlUrlCollector.wsdlUrls()).thenReturn(List.of());
        when(discovery.discover(auto.getServicesUrl(), List.of())).thenReturn(List.of(
                new DiscoveredSoapService(
                        "OrderService",
                        "http://localhost:8080/services/OrderService?wsdl",
                        List.of(new WsdlOperationModel(
                                "CreateOrder",
                                "createOrder",
                                List.of("customer.name", "customer.phone", "products[]", "products[].sku"),
                                List.of(
                                        new SchemaField("customer.name", "string", true, false),
                                        new SchemaField("customer.phone", "string", true, false),
                                        new SchemaField("products[]", "object", true, true),
                                        new SchemaField("products[].sku", "string", true, false)
                                ),
                                "createOrderResponse",
                                List.of("return"),
                                List.of(new SchemaField("return", "string", true, false))
                        ))
                )
        ));

        AutoMappingGenerator generator = new AutoMappingGenerator(properties, discovery, wsdlUrlCollector);

        List<MappingDefinition> mappings = generator.generate();

        assertEquals(1, mappings.size());
        MappingDefinition mapping = mappings.get(0);
        assertEquals("/api/OrderService/CreateOrder", mapping.getPath());
        assertEquals(Map.of(
                "$.customer.name", "$.createOrder.customer.name",
                "$.customer.phone", "$.createOrder.customer.phone",
                "$.products", "$.createOrder.products"
        ), mapping.getRequest());
        assertEquals(Map.of("#root.return", "$.return"), mapping.getResponse());
        assertEquals(List.of("$.customer.name", "$.customer.phone", "$.products", "$.products[].sku"),
                mapping.getRequestSchemaPaths());
        assertEquals(List.of(
                new SchemaField("$.customer.name", "string", true, false),
                new SchemaField("$.customer.phone", "string", true, false),
                new SchemaField("$.products", "object", true, true),
                new SchemaField("$.products[].sku", "string", true, false)
        ), mapping.getRequestSchemaFields());
    }

    @Test
    void generate_returnsEmptyMappingsWhenNoDiscoverySourcesConfigured() throws Exception {
        BridgeProperties properties = new BridgeProperties();
        BridgeProperties.AutoMapping auto = new BridgeProperties.AutoMapping();
        auto.setPathPrefix("/api");
        auto.setHttpMethod("POST");
        properties.setAuto(auto);

        WsdlServiceDiscovery discovery = mock(WsdlServiceDiscovery.class);
        WsdlUrlCollector wsdlUrlCollector = mock(WsdlUrlCollector.class);
        when(wsdlUrlCollector.wsdlUrls()).thenReturn(List.of());
        when(discovery.discover(null, List.of())).thenReturn(List.of());

        AutoMappingGenerator generator = new AutoMappingGenerator(properties, discovery, wsdlUrlCollector);

        assertEquals(0, generator.generate().size());
    }
}

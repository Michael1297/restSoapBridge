package io.github.connellite.model;

import java.util.List;

public record DiscoveredSoapService(String name, String wsdlUrl, List<WsdlOperationModel> operations) {
}

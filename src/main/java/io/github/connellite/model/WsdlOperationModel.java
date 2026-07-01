package io.github.connellite.model;

import java.util.List;

public record WsdlOperationModel(
        String name,
        String inputElement,
        List<String> inputFields,
        String outputElement,
        List<String> outputFields
) {
}

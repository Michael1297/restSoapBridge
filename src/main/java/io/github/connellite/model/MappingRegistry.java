package io.github.connellite.model;

import lombok.Getter;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MappingRegistry {

    @Getter
    private final List<MappingDefinition> mappings;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public MappingRegistry(List<MappingDefinition> mappings) {
        this.mappings = List.copyOf(mappings);
    }

    public Optional<MappingDefinition> find(String httpMethod, String requestPath) {
        HttpMethod method = HttpMethod.valueOf(httpMethod.toUpperCase(Locale.ROOT));
        String normalizedPath = normalizePath(requestPath);

        return mappings.stream()
                .filter(mapping -> HttpMethod.valueOf(mapping.getMethod().toUpperCase(Locale.ROOT)) == method)
                .filter(mapping -> pathMatcher.match(mapping.getPath(), normalizedPath))
                .findFirst();
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
    }
}

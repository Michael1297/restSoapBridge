package io.github.connellite.config;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class WsdlUrlCollector {

    private static final Pattern WSDL_URL_PROPERTY = Pattern.compile("bridge\\.auto\\.wsdl(?:-([\\w-]+))?-url");

    private final List<String> wsdlUrls;

    public WsdlUrlCollector(Environment environment) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        if (environment instanceof ConfigurableEnvironment configurable) {
            for (PropertySource<?> propertySource : configurable.getPropertySources()) {
                collectFromPropertySource(environment, propertySource, urls);
            }
        }
        this.wsdlUrls = List.copyOf(urls);
    }

    public List<String> wsdlUrls() {
        return wsdlUrls;
    }

    private static void collectFromPropertySource(Environment environment, PropertySource<?> propertySource, LinkedHashSet<String> urls) {
        if (!(propertySource instanceof EnumerablePropertySource<?> enumerable)) {
            return;
        }
        for (String propertyName : enumerable.getPropertyNames()) {
            if (!WSDL_URL_PROPERTY.matcher(propertyName).matches()) {
                continue;
            }
            String value = environment.getProperty(propertyName);
            if (StringUtils.hasText(value)) {
                urls.add(value.trim());
            }
        }
    }
}

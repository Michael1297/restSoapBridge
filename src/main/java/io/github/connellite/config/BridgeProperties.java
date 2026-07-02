package io.github.connellite.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "bridge")
public class BridgeProperties {

    private AutoMapping auto;

    @Getter
    @Setter
    public static class AutoMapping {
        private String servicesUrl;
        private String pathPrefix;
        private String httpMethod;
    }
}

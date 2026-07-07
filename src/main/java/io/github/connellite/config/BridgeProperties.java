package io.github.connellite.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

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
        private Duration connectTimeout = Duration.ofSeconds(30);
        private Duration receiveTimeout = Duration.ofSeconds(60);
    }
}

package io.github.connellite.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "bridge")
public class BridgeProperties {

    public static final String AUTO_MODE = "auto";

    private String mappingsFile;
    private AutoMapping auto;

    public boolean isAutoMode() {
        return mappingsFile != null && AUTO_MODE.equalsIgnoreCase(mappingsFile.trim());
    }

    @Getter
    @Setter
    public static class AutoMapping {
        private String servicesUrl;
        private String pathPrefix;
        private String httpMethod;
    }
}

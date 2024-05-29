package org.gislogic.geosever.business.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "geoserver")
@Data
@Component
public class GeoServerConfig {
    private String url;

    private String username;

    private String password;

    private String iconUrl;

}

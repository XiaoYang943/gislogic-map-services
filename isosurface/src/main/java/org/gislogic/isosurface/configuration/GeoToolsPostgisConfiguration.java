package org.gislogic.isosurface.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "postgis")
@Data
public class GeoToolsPostgisConfiguration {

    private String dbtype;

    private String host;

    private String port;

    private String database;

    private String schema;

    private String username;

    private String password;

    private Integer maxconn;

    private Integer minconn;

    private Integer maxwait;

    private Boolean exposePk;

    private String pkMetadataTable;
}

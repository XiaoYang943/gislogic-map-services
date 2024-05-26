package org.gislogic.common.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @description: GeoTools连接PostGIS的配置类
 * @author: hyy
 * @create: 2024-02-12
 **/

@Configuration
@Component
@Data
public class GeoToolsPostgisConfiguration {

    @Value("${postgis.dbtype}")
    private String dbtype;

    @Value("${postgis.host}")
    private String host;

    @Value("${postgis.port}")
    private String port;

    @Value("${postgis.database}")
    private String database;

    @Value("${postgis.schema}")
    private String schema;

    @Value("${postgis.username}")
    private String username;

    @Value("${postgis.password}")
    private String password;

    @Value("${postgis.maxconn}")
    private Integer maxconn;

    @Value("${postgis.minconn}")
    private Integer minconn;

    @Value("${postgis.maxwait}")
    private Integer maxwait;

    @Value("${postgis.exposePk}")
    private Boolean exposePk;

    @Value("${postgis.pkMetadataTable}")
    private String pkMetadataTable;
}

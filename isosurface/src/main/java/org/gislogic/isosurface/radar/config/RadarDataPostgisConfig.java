package org.gislogic.isosurface.radar.config;

import lombok.Data;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "postgis.radar")
@Data
@Component
public class RadarDataPostgisConfig {
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

    public DataStore getPostgisDataStore() {
        Map<String, Object> params = new HashMap<>();
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, this.dbtype);
        params.put(PostgisNGDataStoreFactory.HOST.key, this.host);
        params.put(PostgisNGDataStoreFactory.PORT.key, this.port);
        params.put(PostgisNGDataStoreFactory.DATABASE.key, this.database);
        params.put(PostgisNGDataStoreFactory.SCHEMA.key, this.schema);
        params.put(PostgisNGDataStoreFactory.USER.key, this.username);
        params.put(PostgisNGDataStoreFactory.PASSWD.key, this.password);
        params.put(PostgisNGDataStoreFactory.MAXCONN.key, this.maxconn);
        params.put(PostgisNGDataStoreFactory.MINCONN.key, this.minconn);
        params.put(PostgisNGDataStoreFactory.MAXWAIT.key, this.maxwait);
        params.put(PostgisNGDataStoreFactory.EXPOSE_PK.key, this.exposePk);
        params.put(PostgisNGDataStoreFactory.PK_METADATA_TABLE.key, this.pkMetadataTable);
        try {
            return DataStoreFinder.getDataStore(params);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

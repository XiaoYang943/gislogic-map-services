package org.gislogic.isosurface.radar.configuration;

import lombok.Data;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "postgis.radar")
@Data
public class RadarDataPostgisConfig {
    private static RadarDataPostgisConfig radarDataPostgisConfig;

    static {
        radarDataPostgisConfig = new RadarDataPostgisConfig();
    }

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
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, radarDataPostgisConfig.getDbtype());
        params.put(PostgisNGDataStoreFactory.HOST.key, radarDataPostgisConfig.getHost());
        params.put(PostgisNGDataStoreFactory.PORT.key, radarDataPostgisConfig.getPort());
        params.put(PostgisNGDataStoreFactory.DATABASE.key, radarDataPostgisConfig.getDatabase());
        params.put(PostgisNGDataStoreFactory.SCHEMA.key, radarDataPostgisConfig.getSchema());
        params.put(PostgisNGDataStoreFactory.USER.key, radarDataPostgisConfig.getUsername());
        params.put(PostgisNGDataStoreFactory.PASSWD.key, radarDataPostgisConfig.getPassword());
        params.put(PostgisNGDataStoreFactory.MAXCONN.key, radarDataPostgisConfig.getMaxconn());
        params.put(PostgisNGDataStoreFactory.MINCONN.key, radarDataPostgisConfig.getMinconn());
        params.put(PostgisNGDataStoreFactory.MAXWAIT.key, radarDataPostgisConfig.getMaxwait());
        params.put(PostgisNGDataStoreFactory.EXPOSE_PK.key, radarDataPostgisConfig.getExposePk());
        params.put(PostgisNGDataStoreFactory.PK_METADATA_TABLE.key, radarDataPostgisConfig.getPkMetadataTable());
        try {
            return DataStoreFinder.getDataStore(params);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

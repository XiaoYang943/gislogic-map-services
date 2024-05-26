package org.gislogic.isosurface.utils;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.gislogic.isosurface.configuration.GeoToolsPostgisConfiguration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class PostgisDataStoreUtil {
    public DataStore getPostgisDataStore(GeoToolsPostgisConfiguration geoToolsPostgisConfiguration) {
        Map<String, Object> params = new HashMap<>();
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, geoToolsPostgisConfiguration.getDbtype());
        params.put(PostgisNGDataStoreFactory.HOST.key, geoToolsPostgisConfiguration.getHost());
        params.put(PostgisNGDataStoreFactory.PORT.key, geoToolsPostgisConfiguration.getPort());
        params.put(PostgisNGDataStoreFactory.DATABASE.key, geoToolsPostgisConfiguration.getDatabase());
        params.put(PostgisNGDataStoreFactory.SCHEMA.key, geoToolsPostgisConfiguration.getSchema());
        params.put(PostgisNGDataStoreFactory.USER.key, geoToolsPostgisConfiguration.getUsername());
        params.put(PostgisNGDataStoreFactory.PASSWD.key, geoToolsPostgisConfiguration.getPassword());
        params.put(PostgisNGDataStoreFactory.MAXCONN.key, geoToolsPostgisConfiguration.getMaxconn());
        params.put(PostgisNGDataStoreFactory.MINCONN.key, geoToolsPostgisConfiguration.getMinconn());
        params.put(PostgisNGDataStoreFactory.MAXWAIT.key, geoToolsPostgisConfiguration.getMaxwait());
        params.put(PostgisNGDataStoreFactory.EXPOSE_PK.key, geoToolsPostgisConfiguration.getExposePk());
        params.put(PostgisNGDataStoreFactory.PK_METADATA_TABLE.key, geoToolsPostgisConfiguration.getPkMetadataTable());
        try {
            return DataStoreFinder.getDataStore(params);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

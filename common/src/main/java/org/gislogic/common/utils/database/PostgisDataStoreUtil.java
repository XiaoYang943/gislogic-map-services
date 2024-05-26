package org.gislogic.common.utils.database;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.gislogic.common.configuration.GeoToolsPostgisConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @description: PostGIS数据源工具类
 * @author: hyy
 * @create: 2024-02-12
 **/
@Component
public class PostgisDataStoreUtil {
    @Autowired
    private GeoToolsPostgisConfiguration geoToolsPostgisConfiguration;
    private static String dbtype;
    private static String host;
    private static String port;
    private static String database;
    private static String schema;
    private static String username;
    private static String password;
    /**
     * 场景：频繁给pg表中写入大量数据
     * 报错：java.lang.IllegalStateException: DataSource not available after calling dispose() or before being set.
     * 解决：连接池设置最大连接数
     */
    private static Integer maxconn; // 最大连接数
    private static Integer minconn; // 最小连接数
    private static Integer maxwait; // 超时时间
    /**
     * 场景：pg表设置了分区，主表和子表的关系键是id和日期，需要开启该项，否则不会给日期字段写入值
     */
    private static Boolean exposePk;
    private static String pkMetadataTable;

    public static DataStore getPostgisDataStore() {
        Map<String, Object> params = new HashMap<>();
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, dbtype);
        params.put(PostgisNGDataStoreFactory.HOST.key, host);
        params.put(PostgisNGDataStoreFactory.PORT.key, port);
        params.put(PostgisNGDataStoreFactory.DATABASE.key, database);
        params.put(PostgisNGDataStoreFactory.SCHEMA.key, schema);
        params.put(PostgisNGDataStoreFactory.USER.key, username);
        params.put(PostgisNGDataStoreFactory.PASSWD.key, password);
        params.put(PostgisNGDataStoreFactory.MAXCONN.key, maxconn);
        params.put(PostgisNGDataStoreFactory.MINCONN.key, minconn);
        params.put(PostgisNGDataStoreFactory.MAXWAIT.key, maxwait);
        params.put(PostgisNGDataStoreFactory.EXPOSE_PK.key, exposePk);
        params.put(PostgisNGDataStoreFactory.PK_METADATA_TABLE.key, pkMetadataTable);
        try {
            DataStore dataStore = DataStoreFinder.getDataStore(params);
            return dataStore;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void init() {
        dbtype = geoToolsPostgisConfiguration.getDbtype();
        host = geoToolsPostgisConfiguration.getHost();
        port = geoToolsPostgisConfiguration.getPort();
        database = geoToolsPostgisConfiguration.getDatabase();
        schema = geoToolsPostgisConfiguration.getSchema();
        username = geoToolsPostgisConfiguration.getUsername();
        password = geoToolsPostgisConfiguration.getPassword();
        maxconn = geoToolsPostgisConfiguration.getMaxconn();
        minconn = geoToolsPostgisConfiguration.getMinconn();
        maxwait = geoToolsPostgisConfiguration.getMaxwait();
        exposePk = geoToolsPostgisConfiguration.getExposePk();
        pkMetadataTable = geoToolsPostgisConfiguration.getPkMetadataTable();
    }
}

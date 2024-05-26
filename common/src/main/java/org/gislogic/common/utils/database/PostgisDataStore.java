package org.gislogic.common.utils.database;


import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @description: psvm测试时，geotools连接PostGIS
 * @author: hyy
 * @create: 2024-02-22
 **/


public class PostgisDataStore {
    private static DataStore postgisDataStore = null;
    private static String dbtype = "postgis";
//    private static String dbtype = "postgis-partitioned";

    private static String host = "loclahost";
    private static String port = "5432";
    private static String database = "gislogic";
    private static String schema = "poublic";
    private static String username = "postgres";
    private static String password = "admin";
    private static Integer maxconn = 50;    // 最大连接数(高并发、频繁写入的场景，最好设置大一些，否则报错)
    private static Integer minconn = 1;    // 最小连接数
    private static Integer maxwait = 5;    // 超时时间

    public static DataStore getInstance() {
        if (postgisDataStore == null) {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put(PostgisNGDataStoreFactory.DBTYPE.key, dbtype);
            params.put(PostgisNGDataStoreFactory.HOST.key, host);
            params.put(PostgisNGDataStoreFactory.PORT.key, new Integer(port));
            params.put(PostgisNGDataStoreFactory.DATABASE.key, database);
            params.put(PostgisNGDataStoreFactory.SCHEMA.key, schema);
            params.put(PostgisNGDataStoreFactory.USER.key, username);
            params.put(PostgisNGDataStoreFactory.PASSWD.key, password);
            /**
             * 场景：频繁给pg表中写入大量数据
             * 报错：java.lang.IllegalStateException: DataSource not available after calling dispose() or before being set.
             * 解决：连接池设置最大连接数
             */
            params.put(PostgisNGDataStoreFactory.MAXCONN.key, maxconn);
            params.put(PostgisNGDataStoreFactory.MINCONN.key, minconn);
            params.put(PostgisNGDataStoreFactory.MAXWAIT.key, maxwait);
            /**
             * 由于file time加入主键了，要是不开这个 就不给filetime写入值
             */
            params.put(PostgisNGDataStoreFactory.EXPOSE_PK.key, true);
            params.put(PostgisNGDataStoreFactory.PK_METADATA_TABLE.key, "gis_radar.gt_pk_metadata");
            try {
                postgisDataStore = DataStoreFinder.getDataStore(params);
                System.out.println("PostgisDataStore 初始化geotools中的 Datastore成功");
            } catch (IOException e) {
                System.out.println("PostgisDataStore 初始化geotools中的 Datastore失败");
                System.out.println(e.getMessage());
            }
        }
        return postgisDataStore;
    }

}
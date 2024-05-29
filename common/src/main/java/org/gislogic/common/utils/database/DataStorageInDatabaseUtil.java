package org.gislogic.common.utils.database;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureWriter;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * @description: 数据入pg库工具类
 * @author: hyy
 * @create: 2024-03-06
 **/

public class DataStorageInDatabaseUtil {
    /**
     * @param featureCollection 要素集合
     * @param tableName         表名
     * @param isNewTable        写入到已有的表还是新建表写入
     * @return boolean
     * @description 将 SimpleFeatureCollection 写入pg数据库(迭代器写入)
     * @date 2024-02-15
     * @author hyy
     * 优点：写入时可以修改属性、
     * 缺点：写入较慢(4000条数据，每条四个字段，写入时间：13063 ms)
     **/
    public static boolean writeSimpleFeatureCollection2pgByIterator(SimpleFeatureCollection featureCollection, String tableName, DataStore dataStore, Boolean isNewTable) {
        if (isNewTable) {
            SimpleFeatureType featureType = featureCollection.getSchema();
            CoordinateReferenceSystem crs = featureType.getCoordinateReferenceSystem();

            SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
            typeBuilder.init(featureType);
            typeBuilder.setName(tableName);
            if (crs == null) {
                try {
                    CoordinateReferenceSystem decode = CRS.decode("EPSG:4326");
                    typeBuilder.setCRS(decode);
                    // typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
                } catch (FactoryException e) {
                    throw new RuntimeException(e);
                }
            }

            /**
             * 场景：给pg分区表写入数据
             * 传入的 tableName 是分区表的总表表名 不需要新建分区子表
             */
            SimpleFeatureType newtype = typeBuilder.buildFeatureType();
            try {
                dataStore.createSchema(newtype);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        SimpleFeatureIterator iterator = featureCollection.features();
        try {
            /**
             * 场景：给pg分区表写入数据时，报错：table is readOnly
             * 原因：pkey == null || pkey instanceof NullPrimaryKey || virtualTable != null （pkey：主键）
             * 详情见源码：JDBCFeatureSource
             */
            FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter = dataStore.getFeatureWriterAppend(tableName, Transaction.AUTO_COMMIT);

            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                SimpleFeature simpleFeature = featureWriter.next();
                Collection<Property> properties = feature.getProperties();
                Iterator<Property> propertyIterator = properties.iterator();
                while (propertyIterator.hasNext()) {
                    Property property = propertyIterator.next();
                    simpleFeature.setAttribute(property.getName().toString(), property.getValue());
                }
                /**
                 * 场景：给pg分区表写入数据时，报错：org.postgresql.util.PSQLException: ERROR: no partition of relation "radar_cr_zlxy3" found for row 详细：Partition key of the failing row contains (file_time) = (202402010602).
                 * 解决：需要先创建分区子表
                 */
                featureWriter.write();
            }
            iterator.close();
            featureWriter.close();
            dataStore.dispose();
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 SimpleFeatureCollection 写入 pg 数据库(迭代器写入)，并且过滤无效值[!5-70]
     *
     * @param featureCollection 要素集合
     * @param tableName         表名
     * @return 4000条数据，每条四个字段，写入时间：13063 ms
     * 11137 ms
     */
    public static boolean writeSimpleFeatureCollection2pgByIteratorAndFilter(SimpleFeatureCollection featureCollection, String tableName, DataStore dataStore, Boolean isNewTable) {
        if (isNewTable) {
            SimpleFeatureType featureType = featureCollection.getSchema();
            CoordinateReferenceSystem crs = featureType.getCoordinateReferenceSystem();

            SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
            typeBuilder.init(featureType);
            typeBuilder.setName(tableName);
            if (crs == null) {
                try {
                    CoordinateReferenceSystem decode = CRS.decode("EPSG:4326");
                    typeBuilder.setCRS(decode);
                    // typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
                } catch (FactoryException e) {
                    throw new RuntimeException(e);
                }
            }

            /**
             * 场景：给pg分区表写入数据
             * 传入的 tableName 是分区表的总表表名 不需要新建分区子表
             */
            SimpleFeatureType newtype = typeBuilder.buildFeatureType();
            try {
                dataStore.createSchema(newtype);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        SimpleFeatureIterator iterator = featureCollection.features();
        try {
            /**
             * 场景：给pg分区表写入数据时，报错：table is readOnly
             * 原因：pkey == null || pkey instanceof NullPrimaryKey || virtualTable != null （pkey：主键）
             * 详情见源码：JDBCFeatureSource
             */
            FeatureWriter<SimpleFeatureType, SimpleFeature> featureWriter =
                    dataStore.getFeatureWriterAppend(tableName, Transaction.AUTO_COMMIT);

            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                SimpleFeature simpleFeature = featureWriter.next();

                boolean hasValidValue = false; // 标记是否找到了有效的"value"属性

                for (Property property : feature.getProperties()) {
                    String propertyName = property.getName().toString();
                    Object value = property.getValue();
                    if (propertyName.equals("value")) { // 如果字段名是 value
//                        TODO-hyy 如何解耦
//                        if (value instanceof Number) {
//                            double doubleValue = ((Number) value).doubleValue();
//                            if (doubleValue >= RadarColorEnum.getMinValue() && doubleValue <= RadarColorEnum.getMaxValue()) { // 如果value的值在[5-70]之间
//                                simpleFeature.setAttribute(propertyName, value);
//                                hasValidValue = true; // 标记找到了有效的"value"属性
//                            }
//                        }
                    } else {
                        simpleFeature.setAttribute(propertyName, value);
                    }
                }

                // TODO-hyy 如何解耦
                // 如果找到了有效的 value 属性，则写入要素，否则跳过
                if (hasValidValue) {
                    /**
                     * 场景：给pg分区表写入数据时，报错：org.postgresql.util.PSQLException: ERROR: no partition of relation "radar_cr_zlxy3" found for row 详细：Partition key of the failing row contains (file_time) = (202402010602).
                     * 解决：需要先创建分区子表
                     */
                    featureWriter.write();
                }
            }
            iterator.close();
            featureWriter.close();
            dataStore.dispose();
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据 SimpleFeatureCollection postgis建表
     *
     * @param featureCollection 要素集合
     * @param tableName         表名
     * @param dataStore         数据库
     * @return
     */
    public static boolean dataStoreCreateSchemaBySimpleFeatureCollection(SimpleFeatureCollection featureCollection, String tableName, DataStore dataStore) {
        SimpleFeatureType featureType = featureCollection.getSchema();
        CoordinateReferenceSystem crs = featureType.getCoordinateReferenceSystem();

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.init(featureType);
        typeBuilder.setName(tableName);
        if (crs == null) {  // 警告: Couldn't determine CRS of table naip_20240223 with srid: 0.
            //                CoordinateReferenceSystem decode = CRS.decode("EPSG:4326");
//                typeBuilder.setCRS(decode);
            typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
        }

        SimpleFeatureType newtype = typeBuilder.buildFeatureType();
        try {
            dataStore.createSchema(newtype);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param featureCollection 要素集合
     * @param tableName         表名
     * @param dataStore         数据存储
     * @param isNewTable        写入到已有的表还是新建表写入
     * @return boolean
     * @description 将 SimpleFeatureCollection 写入pg数据库(批量写入)
     * @date 2024-02-15
     * @author hyy
     * 优点：批量写入，减少io次数，写入较快(4000条数据，每条四个字段，写入时间：6718 ms)
     * 缺点：写入时不能修改属性
     **/
    public static boolean writeSimpleFeatureCollection2pgByBatch(SimpleFeatureCollection featureCollection, String tableName, DataStore dataStore, Boolean isNewTable) {
        if (isNewTable) {
            boolean b = dataStoreCreateSchemaBySimpleFeatureCollection(featureCollection, tableName, dataStore);
            if (!b) {
                return false;
            }
        }
        DefaultTransaction transaction = new DefaultTransaction("faster");
        // 批量写入
        try {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) dataStore.getFeatureSource(tableName);
            featureStore.setTransaction(transaction);
            SimpleFeatureCollection collection = new ListFeatureCollection(featureCollection);
            featureStore.addFeatures(collection);
            transaction.commit();
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            dataStore.dispose();
            transaction.close();
        }
    }
}

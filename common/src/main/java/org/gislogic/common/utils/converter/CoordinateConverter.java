package org.gislogic.common.utils.converter;

import cn.hutool.core.util.StrUtil;
import org.geotools.api.data.FeatureWriter;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: gislogic-map-service
 * @description: 坐标转换工具类
 * @author: hyy
 * @create: 2024-03-22
 **/
public class CoordinateConverter {

    /**
     * 定义地球半径（米）
     */
    private static final double R_EARTH = 6371000;
    /**
     * 定义地球赤道周长（米）
     */
    private static final double P_EARTH = 2 * Math.PI * R_EARTH;

    /**
     * 将Y轴的长度（米）转换成纬度
     *
     * @param length
     * @return
     */
    public static double parseYLengthToDegree(double length) {
        //将length长度转换为度数
        double yDegree = length / P_EARTH * 360;
        return yDegree;
    }

    /**
     * 度分秒转度
     *
     * @param degrees 度分秒形式的度数(.eg:E1083754 或 N342344)
     * @return E1083754 -> 经度   N342344 -> 纬度
     */
    public static Double dmsToDecimalDegrees(String degrees) {
        String str = StrUtil.sub(degrees, 1, degrees.length());
        String secondsStr = StrUtil.sub(str, str.length() - 2, str.length());
        String minutesStr = StrUtil.sub(str, str.length() - 4, str.length() - 2);
        String degreesStr = StrUtil.sub(str, 0, str.length() - 4);
        double result = Double.parseDouble(degreesStr) + (Double.parseDouble(minutesStr) / 60.0) + (Double.parseDouble(secondsStr) / 3600.0);
        return new BigDecimal(result).setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue();  // 保留6位小数，四舍五入
    }

    /**
     * E107°58′16″ 转为 E1075816
     *
     * @param dms
     * @return
     */
    public static String convertDMStoString(String dms) {
        // 移除度分秒符号并替换为空格
        String cleaned = dms.replaceAll("[°′″]", " ").trim();

        // 分割字符串为度、分、秒
        String[] parts = cleaned.split("\\s+");

        // 验证是否包含三个部分
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid DMS format: " + dms);
        }

        // 如果分钟或秒不足两位，则在前面补零
        String degrees = parts[0];
        String minutes = parts.length > 1 ? String.format("%02d", Integer.parseInt(parts[1])) : "00";
        String seconds = parts.length > 2 ? String.format("%02d", Integer.parseInt(parts[2])) : "00";

        return degrees + minutes + seconds;
    }

    //坐标转换(基于矢量 Geometry)
    public static Geometry transformGeometryCRSBySRID(Geometry srcGeom, String srcEpsgId, String dstEpsgId) {
        try {
            CoordinateReferenceSystem srcCRS = CRS.decode(srcEpsgId);
            CoordinateReferenceSystem dstCRS = CRS.decode(dstEpsgId);
            Geometry geom = transformGeometryCRS(srcGeom, srcCRS, dstCRS);
            return geom;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Geometry坐标转换
     *
     * @param srcGeom geometry
     * @param srcCRS  源CRS
     * @param dstCRS  目标CRS
     * @return 转换后的 geometry
     */
    public static Geometry transformGeometryCRS(Geometry srcGeom, CoordinateReferenceSystem srcCRS, CoordinateReferenceSystem dstCRS) {
        try {
            MathTransform transform = CRS.findMathTransform(srcCRS, dstCRS, true);
            return JTS.transform(srcGeom, transform);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Shp坐标转换
     *
     * @return
     */
    public static boolean transformFile(String shpFile, String epsgId, String dstFile) {

        try {
            CoordinateReferenceSystem dstCRS = CRS.decode(epsgId);

            //打开shp文件
            File file = new File(shpFile);
            ShapefileDataStore featureSource = new ShapefileDataStore(file.toURL());
            CoordinateReferenceSystem srcCRS = featureSource.getSchema().getCoordinateReferenceSystem();
            SimpleFeatureType featureType = featureSource.getSchema();
            SimpleFeatureTypeImpl typeImpl = (SimpleFeatureTypeImpl) featureType;

            //创建新的shapefile
            File newFile = new File(dstFile);
            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put("url", newFile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);
            ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            SimpleFeatureType newType = SimpleFeatureTypeBuilder.retype(featureType, dstCRS);
            newDataStore.createSchema(newType);

            //处理单个要素
            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource newFeatureSource = newDataStore.getFeatureSource(typeName);
            Transaction transaction = new DefaultTransaction("Reproject");
            FeatureWriter<SimpleFeatureType, SimpleFeature> writer = newDataStore.getFeatureWriter(newDataStore.getTypeNames()[0], Transaction.AUTO_COMMIT);
            SimpleFeatureIterator iter = featureSource.getFeatureSource().getFeatures().features();
            while (iter.hasNext()) {
                SimpleFeature feature = (SimpleFeature) iter.next();

                SimpleFeature newFeature = writer.next();
                newFeature.setAttributes(feature.getAttributes());

                Geometry geometry = (Geometry) feature.getDefaultGeometry();

                newFeature.setDefaultGeometry(transformGeometryCRS(geometry, srcCRS, dstCRS));
            }

            writer.write();
            writer.close();

            iter.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

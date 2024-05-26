package org.gislogic.isosurface;


import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.gislogic.isosurface.business.domain.RadarTrainDataEntity;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import wcontour.Contour;
import wcontour.global.Border;
import wcontour.global.PointD;
import wcontour.global.PolyLine;
import wcontour.global.Polygon;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author hyy
 * @description 使用 wContour 生成等值面
 * @date 2024-03-09
 **/
public class CreateIsosurfaceUtil {
    private static GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * @param trainData    数据
     * @param dataInterval 数据间隔规范
     * @param radarEntity  实体类
     * @return SimpleFeatureCollection 等值面
     * @description 生成等值面
     * @date 2024-03-09
     * @author hyy
     **/
    public static SimpleFeatureCollection equiSurface(RadarTrainDataEntity trainData, double[] dataInterval, RadarEntity radarEntity) {
        double[][] _gridData = trainData.getData();
        double[] _X = trainData.get_X();
        double[] _Y = trainData.get_Y();
        double _undefData = Double.MIN_VALUE;
        int nc = dataInterval.length;
        int[][] S1 = new int[_gridData.length][_gridData[0].length];

        List<Border> _borders = Contour.tracingBorders(_gridData, _X, _Y, S1, _undefData);  // 生成边界

        List<PolyLine> cPolylineList = Contour.tracingContourLines(_gridData, _X, _Y, nc, dataInterval, _undefData, _borders, S1);  // 生成等值线

        cPolylineList = Contour.smoothLines(cPolylineList); // 平滑

        List<Polygon> cPolygonList = Contour.tracingPolygons(_gridData, cPolylineList, _borders, dataInterval); // 生成等值面

        return wContourPolygonList2FeatureCollection(cPolygonList, radarEntity);    // 等值面结果转换
    }

    /**
     * @param cPolygonList 等值面列表
     * @param radarEntity  实体类
     * @return SimpleFeatureCollection 简单要素集合
     * @description 等值面转 SimpleFeatureCollection
     * @date 2024-03-09
     * @author hyy
     **/
    public static SimpleFeatureCollection wContourPolygonList2FeatureCollection(List<Polygon> cPolygonList, RadarEntity radarEntity) {
        if (cPolygonList == null || cPolygonList.size() == 0) {
            return null;
        }
        SimpleFeatureBuilder simpleFeatureBuilder = createSimpleFeatureBuilder(radarEntity.getClass());
        DefaultFeatureCollection collection = new DefaultFeatureCollection();

        for (Polygon pPolygon : cPolygonList) {
            // 外圈
            Coordinate[] coordinates = new Coordinate[pPolygon.OutLine.PointList.size()];
            for (int i = 0, len = pPolygon.OutLine.PointList.size(); i < len; i++) {
                PointD ptd = pPolygon.OutLine.PointList.get(i);
                coordinates[i] = new Coordinate(ptd.X, ptd.Y);
            }
            LinearRing mainRing = geometryFactory.createLinearRing(FeatureExceptionHand.isClosedLine(coordinates));

            //孔洞
            LinearRing[] holeRing = new LinearRing[pPolygon.HoleLines.size()];
            for (int i = 0; i < pPolygon.HoleLines.size(); i++) {
                PolyLine hole = pPolygon.HoleLines.get(i);
                Coordinate[] coordinates_h = new Coordinate[hole.PointList.size()];
                for (int j = 0, len = hole.PointList.size(); j < len; j++) {
                    PointD ptd = hole.PointList.get(j);
                    coordinates_h[j] = new Coordinate(ptd.X, ptd.Y);
                }
                holeRing[i] = geometryFactory.createLinearRing(FeatureExceptionHand.isClosedLine(coordinates_h));
            }

            org.locationtech.jts.geom.Polygon polygon = geometryFactory.createPolygon(mainRing, holeRing);

            if (pPolygon.LowValue >= RadarColorEnum.getMinValue() && pPolygon.LowValue <= RadarColorEnum.getMaxValue()) {
                /**
                 * add的顺序要和实体类的字段顺序保持一致
                 */
                simpleFeatureBuilder.add(polygon);
                simpleFeatureBuilder.add(pPolygon.LowValue);
                simpleFeatureBuilder.add(radarEntity.getFile_time());
                simpleFeatureBuilder.add(radarEntity.getData_time());
                simpleFeatureBuilder.add(radarEntity.getFile_station());

                SimpleFeature feature = simpleFeatureBuilder.buildFeature(null);
                collection.add(feature);
            }
        }
        return collection;
    }

    /**
     * @param clazz 实体类
     * @return SimpleFeatureBuilder pg数据库表结构构建器
     * @description 根据实体类创建 SimpleFeatureBuilder
     * @date 2024-03-09
     * @author hyy
     * 注意：该表的字段顺序严格遵循传入的实体类的字段顺序
     **/
    public static SimpleFeatureBuilder createSimpleFeatureBuilder(Class<?> clazz) {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("type");  // 必须设置，否则报错：java.lang.NullPointerException: Name is required for PropertyType
        Map<String, Class<?>> fieldsAndTypesOrdered = getFieldsAndTypesOrdered(clazz);
        fieldsAndTypesOrdered.forEach(typeBuilder::add);
        SimpleFeatureType type = typeBuilder.buildFeatureType();
        return new SimpleFeatureBuilder(type);
    }

    /**
     * @param clazz 实体类
     * @return Map<String, Class < ?>> 和实体类中字段顺序相同的Map
     * @description 获取实体类的 字段名-字段类型 的有序map映射
     * @date 2024-03-09
     * @author hyy
     **/
    public static Map<String, Class<?>> getFieldsAndTypesOrdered(Class<?> clazz) {
        List<String> fieldOrder = Arrays.asList(clazz.getDeclaredFields()).stream() // 通过反射获取实体类所有声明的字段并转换为数组
                .map(Field::getName)    // 调用每个字段的 getName 方法来提取字段名，并映射到一个新的流中
                .collect(Collectors.toList());  // 将流中的字段名收集到一个列表中，并赋值给 fieldOrder
        Map<String, Class<?>> fieldsAndTypes = new LinkedHashMap<>();   // 不能用普通的 HashMap ，使用 LinkedHashMap 是为了保持插入顺序与实体类中保持一致
        for (String fieldName : fieldOrder) {
            try {
                Field field = clazz.getDeclaredField(fieldName);    // 字段
                Class<?> fieldType = field.getType();   // 字段类型
                fieldsAndTypes.put(fieldName, fieldType);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        return fieldsAndTypes;
    }
}

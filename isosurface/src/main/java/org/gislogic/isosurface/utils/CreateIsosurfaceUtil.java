package org.gislogic.isosurface.utils;


import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.gislogic.common.utils.feature.SimpleFeatureHelper;
import org.gislogic.isosurface.radar.business.entity.RadarEntity;
import org.gislogic.isosurface.radar.business.pojo.GridData;
import org.gislogic.isosurface.radar.enums.RadarColorEnum;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.opengis.feature.simple.SimpleFeature;
import wcontour.Contour;
import wcontour.global.Border;
import wcontour.global.PointD;
import wcontour.global.PolyLine;
import wcontour.global.Polygon;

import java.util.List;

/**
 * @author hyy
 * @description 使用 wContour 生成等值面
 * @date 2024-03-09
 **/
public class CreateIsosurfaceUtil {
    private static GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * @param gridData     数据
     * @param dataInterval 数据间隔规范
     * @param radarEntity  实体类
     * @return SimpleFeatureCollection 等值面
     * @description 生成等值面
     * @date 2024-03-09
     * @author hyy
     **/
    public static SimpleFeatureCollection equiSurface(GridData gridData, double[] dataInterval, RadarEntity radarEntity) {
        double[][] _gridData = gridData.getData();
        double[] _X = gridData.get_X();
        double[] _Y = gridData.get_Y();
        double _undefData = Double.MIN_VALUE;
        int nc = dataInterval.length;
        int[][] S1 = new int[_gridData.length][_gridData[0].length];

        List<Border> _borders = Contour.tracingBorders(_gridData, _X, _Y, S1, _undefData);  // 生成边界

        List<PolyLine> cPolylineList = Contour.tracingContourLines(_gridData, _X, _Y, nc, dataInterval, _undefData, _borders, S1);  // 生成等值线

        cPolylineList = Contour.smoothLines(cPolylineList); // 平滑

        List<Polygon> contourPolygonList = Contour.tracingPolygons(_gridData, cPolylineList, _borders, dataInterval); // 生成等值面

        return wContourPolygonList2FeatureCollection(contourPolygonList, radarEntity);    // 等值面结果转换
    }

    /**
     * @param contourPolygonList 等值面列表
     * @param radarEntity        实体类
     * @return SimpleFeatureCollection 简单要素集合
     * @description 等值面转 SimpleFeatureCollection
     * @date 2024-03-09
     * @author hyy
     **/
    public static SimpleFeatureCollection wContourPolygonList2FeatureCollection(List<Polygon> contourPolygonList, RadarEntity radarEntity) {
        if (contourPolygonList == null || contourPolygonList.size() == 0) {
            return null;
        }
        SimpleFeatureBuilder simpleFeatureBuilder = SimpleFeatureHelper.createSimpleFeatureBuilder(radarEntity.getClass());
        DefaultFeatureCollection collection = new DefaultFeatureCollection();

        for (Polygon pPolygon : contourPolygonList) {
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

            // TODO-hyy:解耦
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

}

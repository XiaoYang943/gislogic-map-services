package org.gislogic.isosurface.utils;

import org.gislogic.common.utils.geom.GeometryValidator;
import org.gislogic.isosurface.radar.business.pojo.GridData;
import org.gislogic.isosurface.radar.business.pojo.IsosurfaceFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import wcontour.Contour;
import wcontour.global.Border;
import wcontour.global.PointD;
import wcontour.global.PolyLine;
import wcontour.global.Polygon;

import java.util.ArrayList;
import java.util.List;

public class CalculateIsosurface {
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * 计算等值面
     *
     * @param gridData     网格数据
     * @param dataInterval 数据间隔
     * @return 等值面要素列表
     */
    public ArrayList<IsosurfaceFeature> calculate(GridData gridData, double[] dataInterval) {
        double[] _X = gridData.get_X();
        double[] _Y = gridData.get_Y();

        double _undefData = Double.MIN_VALUE;

        double[][] _gridData = gridData.getData();

        int[][] S1 = new int[_gridData.length][_gridData[0].length];

        List<Border> _borders = Contour.tracingBorders(_gridData, _X, _Y, S1, _undefData);  // 生成边界

        List<PolyLine> cPolylineList = Contour.tracingContourLines(_gridData, _X, _Y, dataInterval.length, dataInterval, _undefData, _borders, S1);  // 生成等值线

        cPolylineList = Contour.smoothLines(cPolylineList); // 平滑

        List<Polygon> contourPolygonList = Contour.tracingPolygons(_gridData, cPolylineList, _borders, dataInterval); // 生成等值面

        return wContourPolygonList2FeatureCollection(contourPolygonList);    // 等值面结果转换
    }

    /**
     * wContour多边形列表转换为自定义的等值面要素集合列表
     *
     * @param contourPolygonList wContour多边形列表
     * @return 自定义的等值面要素集合列表
     */
    private static ArrayList<IsosurfaceFeature> wContourPolygonList2FeatureCollection(List<Polygon> contourPolygonList) {
        if (contourPolygonList == null || contourPolygonList.isEmpty()) {
            return null;
        }

        ArrayList<IsosurfaceFeature> isosurfaceFeatureList = new ArrayList<>();
        for (Polygon pPolygon : contourPolygonList) {

            // 外圈
            Coordinate[] coordinates = new Coordinate[pPolygon.OutLine.PointList.size()];
            for (int i = 0, len = pPolygon.OutLine.PointList.size(); i < len; i++) {
                PointD ptd = pPolygon.OutLine.PointList.get(i);
                coordinates[i] = new Coordinate(ptd.X, ptd.Y);
            }
            LinearRing mainRing = geometryFactory.createLinearRing(GeometryValidator.fixPolygonCoordinates(coordinates));

            //孔洞
            LinearRing[] holeRing = new LinearRing[pPolygon.HoleLines.size()];
            for (int i = 0; i < pPolygon.HoleLines.size(); i++) {
                PolyLine hole = pPolygon.HoleLines.get(i);
                Coordinate[] coordinates_h = new Coordinate[hole.PointList.size()];
                for (int j = 0, len = hole.PointList.size(); j < len; j++) {
                    PointD ptd = hole.PointList.get(j);
                    coordinates_h[j] = new Coordinate(ptd.X, ptd.Y);
                }
                holeRing[i] = geometryFactory.createLinearRing(GeometryValidator.fixPolygonCoordinates(coordinates_h));
            }

            org.locationtech.jts.geom.Polygon polygon = geometryFactory.createPolygon(mainRing, holeRing);

            IsosurfaceFeature isosurfaceFeature = new IsosurfaceFeature();
            isosurfaceFeature.setPolygon(polygon);
            isosurfaceFeature.setValue(pPolygon.LowValue);
            isosurfaceFeatureList.add(isosurfaceFeature);
        }
        return isosurfaceFeatureList;
    }

}

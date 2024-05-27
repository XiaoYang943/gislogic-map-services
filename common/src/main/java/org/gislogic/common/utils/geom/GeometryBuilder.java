package org.gislogic.common.utils.geom;


import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.gislogic.common.utils.converter.CoordinateConverter;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.util.ArrayList;
import java.util.List;


public class GeometryBuilder {
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * 创建圆
     *
     * @param longitude   圆心经度
     * @param latitude    圆心纬度
     * @param radius      半径(单位：米)
     * @param circleSides 圆的边个数
     * @return Geometry
     * 如果传入的半径的单位是长度，则必须先转为3857，再带入半径计算 buffer 圆
     * 情况1. 先将4326圆心点转换为3857，再计算 buffer
     * 结果：底图为4326坐标系中，圆是矮胖的椭圆，而底图为3857坐标系中，圆为正圆
     * 情况2. 若直接使用4326的圆心点计算 buffer：
     * 结果：底图为4326坐标系中，圆是正圆，而底图为3857坐标系中，圆为瘦高的椭圆
     */
    public static Geometry buildCircle(double longitude, double latitude, double radius, Integer circleSides) {
        Coordinate circleCenterPointCoord = new Coordinate(longitude, latitude);
        Point circleCenterPoint = geometryFactory.createPoint(circleCenterPointCoord);  // 圆心点
        CRSAuthorityFactory factory = CRS.getAuthorityFactory(true);
        try {
            CoordinateReferenceSystem crs4326 = factory.createCoordinateReferenceSystem("EPSG:4326");
            CoordinateReferenceSystem crs3857 = factory.createCoordinateReferenceSystem("EPSG:3857");
            MathTransform transformTo3857 = CRS.findMathTransform(crs4326, crs3857);
            Geometry circleCenterPoint3857 = JTS.transform(circleCenterPoint, transformTo3857);
            Geometry buffer3857 = circleCenterPoint3857.buffer(radius, circleSides);  // buffer计算缓冲区圆，传入的半径的单位是：米
            MathTransform transformTo4326 = CRS.findMathTransform(crs3857, crs4326);
            return JTS.transform(buffer3857, transformTo4326);    // 再将计算出的 buffer 圆转换为4326;
        } catch (FactoryException | TransformException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 计算二阶贝塞尔曲线
     *
     * @param startPoint   起始点
     * @param controlPoint 控制点
     * @param endPoint     终点
     * @param num          构成曲线的点的数量
     */
    public static LineString buildCubicBezierCurve(Point startPoint, Point controlPoint, Point endPoint, Integer num) {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        List<Coordinate> coordinates = new ArrayList<>();
        for (int i = 0; i <= num; i++) {
            double t = i / new Double(num);
            double x = (Math.pow(1 - t, 2) * startPoint.getX() + 2 * t * (1 - t) * controlPoint.getX() + Math.pow(t, 2) * endPoint.getX());
            double y = (Math.pow(1 - t, 2) * startPoint.getY() + 2 * t * (1 - t) * controlPoint.getY() + Math.pow(t, 2) * endPoint.getY());
            Coordinate coordinate = new Coordinate();
            coordinate.setX(x);
            coordinate.setY(y);
            coordinates.add(coordinate);
        }
        return geometryFactory.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
    }

    /**
     * 根据中心点经纬度、半径、起始角度角度生成扇形
     *
     * @param x         经度
     * @param y         纬度
     * @param radius    半径（米）
     * @param bAngle    起始角度
     * @param eAngle    终止角度
     * @param pointsNum 点数
     * @return
     */
    public static Polygon buildSector(double x, double y, double radius, double bAngle, double eAngle, int pointsNum) {
        //将半径转换为度数
        double radiusDegree = CoordinateConverter.parseYLengthToDegree(radius);
        //将起始角度转换为弧度
        double bAngleRadian = Math.toRadians(bAngle);
        //将终止角度-起始角度计算扇形夹角
        double angleRadian = Math.toRadians((eAngle - bAngle + 360) % 360);

        GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
        shapeFactory.setNumPoints(pointsNum);
        shapeFactory.setCentre(new Coordinate(x, y));
        shapeFactory.setSize(radiusDegree);
        Polygon sector = shapeFactory.createArcPolygon(bAngleRadian, angleRadian);
        return sector;
    }

}

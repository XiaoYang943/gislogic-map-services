package org.gislogic.common.utils.geom;

import cn.hutool.core.collection.CollectionUtil;
import org.gislogic.common.enums.GeometryEnum;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import java.math.BigDecimal;
import java.util.List;

public class GeometryValidator {


    /**
     * 判断几何的真实类型，是线还是面
     *
     * @param coordinates 坐标
     * @return 几何类型
     * 某些数据是闭合的面 Polygon，但是kml中是 LineString
     */
    public static String judgeIsLinestringOrPolygon(List<Coordinate> coordinates) {
        if (coordinates.size() == 0) return "";
        Coordinate startCoordinate = coordinates.get(0);
        double startX = startCoordinate.getX();
        double startY = startCoordinate.getY();

        Coordinate endCoordinate = coordinates.get(coordinates.size() - 1);
        double endX = endCoordinate.getX();
        double endY = endCoordinate.getY();

        if (startX == endX && startY == endY) {
            return GeometryEnum.POLYGON;
        } else {
            return GeometryEnum.LINESTRING;
        }
    }

    /**
     * 修复 Polygon 首尾不闭合的情况
     *
     * @param coordinates
     * @return 解决以下报错:java.lang.IllegalArgumentException: Points of LinearRing do not form a closed linestring
     */
    public static List<Coordinate> fixPolygonCoordinates(List<Coordinate> coordinates) {
        Coordinate startCoordinate = coordinates.get(0);
        double startX = startCoordinate.getX();
        double startY = startCoordinate.getY();

        Coordinate endCoordinate = coordinates.get(coordinates.size() - 1);
        double endX = endCoordinate.getX();
        double endY = endCoordinate.getY();

        // 非闭合
        if (startX != endX || startY != endY) coordinates.add(startCoordinate);
        return coordinates;
    }

    /**
     * 修复 Polygon 首尾不闭合的情况
     *
     * @param coordinates 坐标
     * @return 解决以下报错:java.lang.IllegalArgumentException: Points of LinearRing do not form a closed linestring
     */
    public static Coordinate[] fixPolygonCoordinates(Coordinate[] coordinates) {
        // 起点，终点
        Coordinate startPoint = coordinates[0];
        Coordinate endPoint = coordinates[coordinates.length - 1];
        if (new BigDecimal(startPoint.x).compareTo(new BigDecimal(endPoint.x)) != 0
                || new BigDecimal(startPoint.y).compareTo(new BigDecimal(endPoint.y)) != 0) {
            // 起点，终点位置不一致
            Coordinate[] newCoordinates = new Coordinate[coordinates.length + 1];
            System.arraycopy(coordinates, 0, newCoordinates, 0, coordinates.length);
            // 将起点作为终点
            newCoordinates[newCoordinates.length - 1] = new Coordinate(startPoint.x, startPoint.y);
            return newCoordinates;
        }
        return coordinates;
    }

    /**
     * 让 Polygon 遵循右手规则
     *
     * @param coordinates
     * @return 防止 Polygon 在一些校验软件中报警告：Polygons and MultiPolygons should follow the right-hand rule
     */
    public static List<Coordinate> reversePolygonListToFollowRightHandRule(List<Coordinate> coordinates) {
        int lowestPointIndex = findLowestPointIndex(coordinates);
        boolean b = polygonIsRightHandRule(coordinates, lowestPointIndex);
        if (!b) {
            List<Coordinate> reverse = CollectionUtil.reverse(coordinates);
            return reverse;
        }
        return coordinates;
    }

    /**
     * 求 LPI (Lowest Point Index)，即纬度最小的点的索引，若有两个，则取其中经度最小的点的索引
     *
     * @param coordinates
     * @return LPI
     */
    public static int findLowestPointIndex(List<Coordinate> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            return -1;
        }

        // 初始化最低纬度和对应索引
        double minLatitude = Double.MAX_VALUE;
        int minIndex = -1;

        for (int i = 0; i < coordinates.size(); i++) {
            Coordinate coordinate = coordinates.get(i);
            double latitude = coordinate.getY();
            double longitude = coordinate.getX();

            // 如果当前纬度小于已知最低纬度，或者纬度相同但经度更小，则更新索引
            if (latitude < minLatitude || (latitude == minLatitude && longitude < coordinates.get(minIndex).getX())) {
                minLatitude = latitude;
                minIndex = i;
            }
        }

        return minIndex;
    }

    /**
     * 判断多边形是否遵循右手规则
     *
     * @param coordinates
     * @param lowestPointIndex LPI
     * @return true: 逆时针(遵循右手规则)。false: 顺时针(不遵循右手规则)
     */
    public static boolean polygonIsRightHandRule(List<Coordinate> coordinates, Integer lowestPointIndex) {
        Coordinate lastPoint;
        Coordinate curPoint;
        Coordinate nextPoint;
        // LPI在数组中间的情况
        if (lowestPointIndex == 0) {
            lastPoint = coordinates.get(lowestPointIndex);
            curPoint = coordinates.get(lowestPointIndex + 1);
            nextPoint = coordinates.get(lowestPointIndex + 2);
        } else if (lowestPointIndex == coordinates.size() - 1) {
            // LPI在数组尾部的情况
            lastPoint = coordinates.get(lowestPointIndex - 2);
            curPoint = coordinates.get(lowestPointIndex - 1);
            nextPoint = coordinates.get(lowestPointIndex);
        } else {
            // LPI在数组开头的情况
            lastPoint = coordinates.get(lowestPointIndex - 1);
            curPoint = coordinates.get(lowestPointIndex);
            nextPoint = coordinates.get(lowestPointIndex + 1);
        }

        double x1 = curPoint.getX() - lastPoint.getX();
        double y1 = curPoint.getY() - lastPoint.getY();
        double x2 = nextPoint.getX() - lastPoint.getX();
        double y2 = nextPoint.getY() - lastPoint.getY();
        // 计算向量的叉积
        double crossProduct = x1 * y2 - x2 * y1;

        // 判断是否遵循右手规则
        if (crossProduct > 0) {
            return true;   // 逆时针
        } else if (crossProduct < 0) {
            return false;   // 顺时针
        }
        return false;

    }

    /**
     * 几何是否应该闭合
     *
     * @param geometry 几何
     * @return boolean
     */
    public boolean shouldClosePath(Geometry geometry) {
        return (geometry instanceof Polygon) || (geometry instanceof LinearRing);
    }
}

package org.gislogic.geosever.business.publishshp;

/**
 * @program: gislogic-map-service
 * @description: 公用枚举类
 * @author: hyy
 * @create: 2024-03-22
 **/
public class CommonEnum {
    public static final String SRID = "EPSG:4326";   // 坐标系
    public static final String DEFAULT_TYPE_POLYGON = "polygon";    // 面
    public static final String DEFAULT_TYPE_POLYLINE = "polyline";  // 线
    public static final String DEFAULT_TYPE_POINT = "point";    // 点
    public static final String DEFAULT_TYPE_ARC = "arc";    // 角度作弧
    public static final String DEFAULT_TYPE_RUNWAY = "runway";  // 跑道
    public static final String DEFAULT_TYPE_TEXT = "text";  // 文本
    public static final String DEFAULT_TYPE_MARKER = "marker";  // 图标
    public static final String DEFAULT_TYPE_ELLIPSE = "ellipse";    // 圆
    public static final String DEFAULT_TYPE_ARC3 = "arc3";  // 三点作弧
    public static final Integer DEFAULT_MARKER_SIZE = 10;   // SLD图标大小
    public static final String DEFAULT_FONT_FAMILY = "SimHei";  // SLD字体类型
    public static final Integer DEFAULT_FONT_SIZE = 10; // SLD字体大小
    public static final Double DEFAULT_LINE_WIDTH = 0.5;    // SLD线宽
    public static final String DEFAULT_ICON_URL = "/styles/markericon/"; // 生产 服务器 icon 地址
    public static final String LONGITUDE = "longitude";  // 经度
    public static final String LATITUDE = "latitude";  // 经度
    public static final Integer DEFAULT_ELLIPSE_NSIDES = 50;    // 构造圆的边的个数
    public static final String CUSTOMID = "customId";  // GeoJSON自定义属性
    public static final String SOURCETYPE = "sourceType";  // GeoJSON自定义属性
    public static final String MARKERTYPE = "markerType";  // GeoJSON重命名属性
}

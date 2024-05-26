package org.gislogic.geosever.business.publishshp;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.GeodeticCalculator;
import org.gislogic.common.utils.converter.CoordinateConverter;
import org.gislogic.common.utils.converter.DataFormatConverter;
import org.gislogic.common.utils.geom.BuildGeometryUtil;
import org.gislogic.common.utils.validator.DataQualityInspectionUtil;
import org.locationtech.jts.geom.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @program: gislogic-map-service
 * @description: JSON转GeoJSON
 * @author: hyy
 * @create: 2024-03-20
 **/
public class JSON2GeoJSON {
    public static void getAllGeometryType(String inputFolderPath, Set<String> geometryTypeSet) {
        File file = new File(inputFolderPath);
        File[] files = file.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                String path = f.getAbsolutePath();
                getAllGeometryType(path, geometryTypeSet);
            } else {
                String extension = f.getName().substring(f.getName().lastIndexOf('.') + 1);
                if (extension.equals("json")) {
                    JSONObject jsonObject = JSONUtil.readJSONObject(f, Charset.forName("UTF-8"));
                    Set<String> set = getGeometryType(jsonObject);
                    geometryTypeSet.addAll(set);
                }
            }
        }
    }

    /**
     * 获取几何类型
     *
     * @param jsonObject
     * @return .eg:[arc, polygon, marker, runway, ellipse, text, arc3, polyline]
     */
    private static Set<String> getGeometryType(JSONObject jsonObject) {
        return jsonObject.getJSONObject("map").getJSONObject("content").keySet();
    }

    private static void getGeometryType(JSONObject jsonObject, Set<String> allGeoJsonGeometryType) {
        Set<String> strings = jsonObject.getJSONObject("map").getJSONObject("content").keySet();
        Console.log(strings);
        allGeoJsonGeometryType.addAll(strings);
    }

    public static void executeJson2GeoJSON(String inputFolderPath, String outputFolderPath) {
        File file = new File(inputFolderPath);
        File[] files = file.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                String path = f.getAbsolutePath();
                executeJson2GeoJSON(path, outputFolderPath);
            } else {
                String path = f.getAbsolutePath();
                Set<String> allGeoJsonPropertiesSet = new HashSet<>();
                Set<String> allGeoJsonGeometryType = new HashSet<>();
                String extension = f.getName().substring(f.getName().lastIndexOf('.') + 1);
                if (extension.equals("json")) {
                    JSONObject jsonObject = JSONUtil.readJSONObject(f, Charset.forName("UTF-8"));
                    getGeoJsonProperties(jsonObject, allGeoJsonPropertiesSet);
                    getGeometryType(jsonObject, allGeoJsonGeometryType);
//                    Console.log(f.getName() + "_" + allGeoJsonPropertiesSet);
                    JSONObject content = jsonObject.getJSONObject("map").getJSONObject("content");
                    /**
                     * 不能用Set集合，虽然Set元素不重复，但是是无序的，而构造 SimpleFeatureCollection 对字段顺序有严格的要求
                     */
                    ArrayList<String> allGeoJsonPropertiesList = new ArrayList<>(allGeoJsonPropertiesSet);
                    ArrayList<String> allGeoJsonGeometryTypeList = new ArrayList<>(allGeoJsonGeometryType);
                    System.out.println("正在处理" + path + "...");
                    SimpleFeatureCollection simpleFeatureCollection = createSimpleFeatureCollection(f, content, allGeoJsonPropertiesList, allGeoJsonGeometryTypeList);
//                    Console.log(f.getName() + "_" + allGeoJsonGeometryType);
                    String geojsonStr = DataFormatConverter.convertFeatureCollection2String(simpleFeatureCollection);

                    try {
                        String nameWithoutExtension = f.getName().substring(0, f.getName().lastIndexOf('.'));
                        FileWriter fileWriter = new FileWriter(outputFolderPath + "\\" + nameWithoutExtension + ".geojson");
                        fileWriter.write(geojsonStr);
                        fileWriter.close();
//                        System.out.println("json数据：" + file.getAbsolutePath() + " 转换为geojson成功！");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 获取GeoJSON所需的properties的字段名
     *
     * @param jsonObject
     * @param allGeoJsonPropertiesSet .eg:05CHANGYONG.json:[cpoint, string, brushcolor, linetype, filltype, type, point, rect, pos, width, name, pencolor, font]
     */
    private static void getGeoJsonProperties(JSONObject jsonObject, Set<String> allGeoJsonPropertiesSet) {
        JSONObject content = jsonObject.getJSONObject("map").getJSONObject("content");
        /**
         * 自定义属性
         * GeoJSON中每个Feature都是全属性，通过该自定义属性判断该Feature属于什么几何类型(见下列关系映射)，
         *
         * [arc, polygon, marker, runway, ellipse, text, arc3, polyline]
         * 点: marker、text
         * 线：arc、runway、arc3、polyline
         * 面：polygon、ellipse
         */
        allGeoJsonPropertiesSet.add(CommonEnum.SOURCETYPE);
        /**
         * 自定义属性id
         * 1. 用于手工验证、排查问题(某个文件的某个几何的第几个要素)
         * 2. 用于解析SLD时，设置Filter，因为一份GeoJSON可能有多条线(或点或面)，这些线的样式可能不同，需要用id区分
         * .eg:
         *       ********************************第一条线********************************
         *       <sld:FeatureTypeStyle>
         *         <sld:Name>name</sld:Name>
         *         <sld:Rule>
         *           <ogc:Filter>
         *             <ogc:PropertyIsEqualTo>
         *               <ogc:PropertyName>source</ogc:PropertyName>
         *               ********************************id********************************
         *               <ogc:Literal>05CHANGYONG_polyline_1</ogc:Literal>
         *             </ogc:PropertyIsEqualTo>
         *           </ogc:Filter>
         *           <sld:LineSymbolizer>
         *             <sld:Stroke>
         *                 ********************************不同的样式********************************
         *               <sld:CssParameter name="stroke">#aa0000</sld:CssParameter>
         *               <sld:CssParameter name="stroke-width">0.5</sld:CssParameter>
         *             </sld:Stroke>
         *           </sld:LineSymbolizer>
         *         </sld:Rule>
         *       </sld:FeatureTypeStyle>
         *
         *       ********************************第二条线********************************
         *       <sld:FeatureTypeStyle>
         *         <sld:Name>name</sld:Name>
         *         <sld:Rule>
         *           <ogc:Filter>
         *             <ogc:PropertyIsEqualTo>
         *               <ogc:PropertyName>source</ogc:PropertyName>
         *               ********************************id********************************
         *               <ogc:Literal>05CHANGYONG_polyline_2</ogc:Literal>
         *             </ogc:PropertyIsEqualTo>
         *           </ogc:Filter>
         *           <sld:LineSymbolizer>
         *             <sld:Stroke>
         *                 ********************************不同的样式********************************
         *               <sld:CssParameter name="stroke">#7c57c5</sld:CssParameter>
         *               <sld:CssParameter name="stroke-width">0.5</sld:CssParameter>
         *             </sld:Stroke>
         *           </sld:LineSymbolizer>
         *         </sld:Rule>
         *       </sld:FeatureTypeStyle>
         */
        allGeoJsonPropertiesSet.add(CommonEnum.CUSTOMID);
        for (String key : content.keySet()) {
            Object value = content.get(key);
            /**
             * xml转json后，如果 content 中的几何类型的value是有多个，则该几何类型是JSONArray，若只有一个，则是JSONObject
             * 且hutool中JSONObject和JSONArray不能相互转换，所以要分别判断
             * .eg:
             * 一个文件中面类型只有一个要素
             *  CommonEnum.DEFAULT_TYPE_POLYGON: {
             *     ...
             *  },
             *  一个文件中线类型有多个要素
             *  CommonEnum.DEFAULT_TYPE_POLYLINE: [
             *    {
             *      ...
             *    },
             *    {
             *      ...
             *    },
             *  ],
             */
            if (value instanceof JSONObject) {
                Set<String> set = ((JSONObject) value).keySet();
                allGeoJsonPropertiesSet.addAll(set);
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                for (Object obj : array) {
                    if (obj instanceof JSONObject) {
                        Set<String> set = ((JSONObject) obj).keySet();
                        allGeoJsonPropertiesSet.addAll(set);
                    }
                }
            }
        }

        // 收集需要重命名的键
        Set<String> toRename = new HashSet<>();
        for (String propName : allGeoJsonPropertiesSet) {
            if (propName.equals("type")) {
                toRename.add(propName);
            }
        }

        // 重命名键
        for (String propName : toRename) {
            allGeoJsonPropertiesSet.remove(propName);
            allGeoJsonPropertiesSet.add(CommonEnum.MARKERTYPE);
        }
    }

    /**
     * 同一个 GeoJSON 中， 不同类型的要素属性名也不同，需要求并集，没值的设为”“(不能设置为null，geotools底层会排除掉，必须要用""占位)，否则属性值赋值时，会出现错误
     * {
     * "type": "Feature",
     * "properties": {
     * "brushcolor": 4278190080,
     * "linetype": "dashline",
     * "filltype": "nobrush",
     * "width": 1,
     * "point": 4286414205,
     * "pencolor": "Bitstream Vera Sans,11,-1,5,50,0,0,0,0,0"
     * },
     * "id": "fid--18687125_18e5aab9cbf_-7fff"
     * },
     * {
     * "type": "Feature",
     * "properties": {
     * "rect": 4278190080,
     * "cpoint": "solidline",
     * "brushcolor": "nobrush",
     * "linetype": 1,
     * "width": 4286414205,
     * "point": "Bitstream Vera Sans,11,-1,5,50,0,0,0,0,0"
     * },
     * "id": "fid--18687125_18e5aab9cbf_-8000"
     * }
     */
    /**
     * 一个文件创建一个 SimpleFeatureCollection
     */
    private static SimpleFeatureCollection createSimpleFeatureCollection(File file, JSONObject jsonObject, ArrayList<String> allGeoJsonPropertiesSet, ArrayList<String> allGeoJsonGeometryType) {
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();  // 一个文件公用一个 SimpleFeatureTypeBuilder
        typeBuilder.setName(file.getName());  // 必须设置，否则报错：java.lang.NullPointerException: Name is required for PropertyType

        allGeoJsonPropertiesSet.forEach((item) -> {
            typeBuilder.add(item, Object.class);    // 同一份 GeoJSON properties 是全属性
        });
        typeBuilder.add("the_geom", Geometry.class);    // 几何类型
        SimpleFeatureType type = typeBuilder.buildFeatureType();
        String fileName = FileNameUtil.mainName(file);
        allGeoJsonGeometryType.forEach((item) -> {
            Object o = jsonObject.get(item);
            if (o instanceof JSONArray) {
                // 某个几何类型有多个 Feature
                JSONArray jsonArray = jsonObject.getJSONArray(item);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonFeature = jsonArray.getJSONObject(i);
                    SimpleFeature simpleFeature = getSimpleFeature(allGeoJsonPropertiesSet, jsonFeature, type, item, fileName, i);
                    if (simpleFeature != null) {
                        featureCollection.add(simpleFeature);
                    }
                }
            } else if (o instanceof JSONObject) {
                // 某个几何类型只有一个 Feature
                JSONObject jsonFeature = (JSONObject) o;
                SimpleFeature simpleFeature = getSimpleFeature(allGeoJsonPropertiesSet, jsonFeature, type, item, fileName, 0);
                if (simpleFeature != null) {
                    featureCollection.add(simpleFeature);
                }
            }
        });
        return featureCollection;
    }


    private static SimpleFeature getSimpleFeature(ArrayList<String> allGeoJsonPropertiesSet, JSONObject jsonFeature, SimpleFeatureType type, String geometryType, String fileName, Integer featureIndex) {
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        for (int i = 0; i < allGeoJsonPropertiesSet.size(); i++) {
            String propName = allGeoJsonPropertiesSet.get(i);
            Object property = jsonFeature.get(propName);

            if (property == null) {
                if (propName.equals(CommonEnum.CUSTOMID)) {
                    featureBuilder.set(i, fileName + "_" + geometryType + "_" + featureIndex);    // 自定义属性
                } else {
                    featureBuilder.set(i, "");  // 属性值为null表示该字段不属于该Feature
                    if (propName.equals(CommonEnum.SOURCETYPE)) {
                        featureBuilder.set(i, geometryType);    // 自定义属性
                    } else if (propName.equals(CommonEnum.MARKERTYPE)) {
                        Object o = jsonFeature.get("type");
                        if (o instanceof JSONObject) {  // ACC-FIX的markertype是JSONObject，需要特殊判断
                            featureBuilder.set(i, ((JSONObject) o).getStr("content"));
                        } else if (o instanceof String) {
                            if (o == null) {
                                featureBuilder.set(i, "");    // 给重命名的属性赋值
                            } else {
                                featureBuilder.set(i, o);    // 给重命名的属性赋值
                            }
                        }
                    }
                }
            } else {
                if (propName.equals("point") || propName.equals("pos") || propName.equals("cpoint") || propName.equals("rect")) {
                    featureBuilder.set(i, "");  // 这个字段无用了，置空
                } else {
                    featureBuilder.set(i, property); // 赋值属于该Feature的属性值
                }
            }
            if (geometryType.equals(CommonEnum.DEFAULT_TYPE_POLYLINE) || geometryType.equals(CommonEnum.DEFAULT_TYPE_RUNWAY)) {
                if (propName.equals("point")) {
                    try {
                        JSONArray points = jsonFeature.getJSONArray(CommonEnum.DEFAULT_TYPE_POINT);
                        List<Coordinate> coordinates = pointsJSONArray2CoordinatesList(points);
                        /**
                         * 报错：class java.util.ArrayList cannot be cast to class org.locationtech.jts.geom.CoordinateSequence
                         * 解决：toArray转换一下
                         */
                        LineString lineString = geometryFactory.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
                        featureBuilder.set(allGeoJsonPropertiesSet.size(), lineString);
                    } catch (Exception e) {
                        /**
                         * 特殊情况：线的 Feature point 是一个对象,不合法，属于垃圾数据，直接跳过
                         * {
                         *           "pencolor": 4279966491,
                         *           "width": 1,
                         *           "filltype": "nobrush",
                         *           "linetype": "solidline",
                         *           "brushcolor": 4278190080,
                         *           "font": "Bitstream Charter,-1,16,5,50,0,0,0,0,0",
                         *           "point": { CommonEnum.LONGITUDE: "E1070048", CommonEnum.LATITUDE: "N342446" }
                         *         }
                         */
                        e.printStackTrace();
                        return null;
                    }

                }
            } else if (geometryType.equals(CommonEnum.DEFAULT_TYPE_MARKER) || geometryType.equals(CommonEnum.DEFAULT_TYPE_TEXT)) {
                if (propName.equals("pos")) {
                    Coordinate coordinate = new Coordinate();
                    JSONObject jsonObject = convertCoord(jsonFeature.getJSONObject("pos"));
                    double longitude = jsonObject.getDouble(CommonEnum.LONGITUDE);
                    double latitude = jsonObject.getDouble(CommonEnum.LATITUDE);
                    coordinate.setX(longitude);
                    coordinate.setY(latitude);
                    Point point = geometryFactory.createPoint(coordinate);
                    featureBuilder.set(allGeoJsonPropertiesSet.size(), point);
                }
            } else if (geometryType.equals(CommonEnum.DEFAULT_TYPE_ELLIPSE)) {
                if (propName.equals("rect")) {
                    Double aDouble = jsonFeature.getJSONObject("rect").getDouble("width");
                    featureBuilder.add(aDouble);   // ellipse 半径
                } else if (propName.equals("cpoint")) {
                    JSONObject cpoint = convertCoord(jsonFeature.getJSONObject("cpoint"));
                    double longitude = cpoint.getDouble(CommonEnum.LONGITUDE);
                    double latitude = cpoint.getDouble(CommonEnum.LATITUDE);
                    Double width = jsonFeature.getJSONObject("rect").getDouble("width") / 2;    // 单位：米
                    Geometry buffer4326 = BuildGeometryUtil.buildCircle(longitude, latitude, width, CommonEnum.DEFAULT_ELLIPSE_NSIDES);
                    Coordinate[] coordinates = buffer4326.getCoordinates();
                    LineString lineString = geometryFactory.createLineString(coordinates);
                    featureBuilder.set(allGeoJsonPropertiesSet.size(), lineString);
                }
            } else if (geometryType.equals(CommonEnum.DEFAULT_TYPE_POLYGON)) {
                JSONArray points = jsonFeature.getJSONArray(CommonEnum.DEFAULT_TYPE_POINT);
                List<Coordinate> coordinates = pointsJSONArray2CoordinatesList(points);
                if (DataQualityInspectionUtil.fixPolygonCoordinates(coordinates)) {
                    List<Coordinate> reversed = DataQualityInspectionUtil.reversePolygonListToFollowRightHandRule(coordinates);
                    Coordinate[] array = reversed.toArray(new Coordinate[reversed.size()]);
                    Polygon polygon = geometryFactory.createPolygon(array);
                    featureBuilder.set(allGeoJsonPropertiesSet.size(), polygon);
                }
            } else if (geometryType.equals(CommonEnum.DEFAULT_TYPE_ARC3)) {
                JSONArray points = jsonFeature.getJSONArray(CommonEnum.DEFAULT_TYPE_POINT);
                Point startPoint = convertPointJson2PointGeo(points.getJSONObject(0));
                Point controlPoint = convertPointJson2PointGeo(points.getJSONObject(1));
                Point endPoint = convertPointJson2PointGeo(points.getJSONObject(2));
                if (startPoint != null && controlPoint != null && endPoint != null) {
                    LineString cubicBezierCurve = BuildGeometryUtil.buildCubicBezierCurve(startPoint, controlPoint, endPoint, 100);
                    featureBuilder.set(allGeoJsonPropertiesSet.size(), cubicBezierCurve);
                }
            } else if (geometryType.equals(CommonEnum.DEFAULT_TYPE_ARC)) {
                Integer starta = jsonFeature.getInt("starta");
                Integer lengtha = jsonFeature.getInt("lengtha");
                if (
                        (starta == -26 && lengtha == 0) || (starta == 156 && lengtha == 0) || (starta == 155 && lengtha == 0) || (starta == -30 && lengtha == 0)    // RWY05L-ARR
                                || (starta == -24 && lengtha == 184) || (starta == 62 && lengtha == 0) || (starta == 42 && lengtha == 0)    // RWY23R-ARR
                                || (starta == -24 && lengtha == 0)  // RWY23L-ARR
                                || (starta == -17 && lengtha == 0)  // RWY05-S-ARR
                                || (starta == 213 && lengtha == 0)  // RNAV05L_ARR
                )
                    break; // 过滤无效数据

                JSONObject jsonObject = convertCoord(jsonFeature.getJSONObject("cpoint"));
                double longitude = jsonObject.getDouble(CommonEnum.LONGITUDE);
                double latitude = jsonObject.getDouble(CommonEnum.LATITUDE);

                List<Coordinate> lineCoordinates = new ArrayList<>();

                /**
                 * 圆
                 */
                if (jsonFeature.getJSONObject("rect").getDouble("width") == 68248) break;  // 过滤无效数据(RNAV23R_ARR)
                Double width = jsonFeature.getJSONObject("rect").getDouble("width") / 2;    // 单位：米
                Double angle = 0.0;
                Double angle1 = 0.0;

                if (starta > 0 && starta < 90) {
                    angle = (double) (90 - starta); // 北偏东
                    angle1 = angle + 180;
                } else if (starta > 180 && starta < 270) {
                    angle = (double) (starta - 180);
                    angle = 90 - angle; // 东偏北转北偏东
                    angle1 = angle + 180;
                } else if (starta > 90 && starta < 180) {
                    angle = (double) (starta - 90);
                    angle = 360 - angle; // 北偏西转北偏东
                    angle1 = 180 - (double) (starta - 90);
                } else if (starta > -90 && starta < 0) {
                    angle = (double) (90 + Math.abs(starta));   // 北偏东
                    angle1 = angle + 180;
                }


                GeodeticCalculator calculator = new GeodeticCalculator();
                calculator.setStartingGeographicPoint(longitude, latitude);

                calculator.setDirection(angle, width); // azimuth：方位角(北偏东)
                Point2D dest = calculator.getDestinationGeographicPoint();
                double x = dest.getX();
                double y = dest.getY();
                Coordinate coordinate = new Coordinate();
                coordinate.setX(x);
                coordinate.setY(y);
                Point point = geometryFactory.createPoint(coordinate);


                GeodeticCalculator calculator1 = new GeodeticCalculator();
                calculator1.setStartingGeographicPoint(longitude, latitude);

                calculator1.setDirection(angle1, width); // azimuth：方位角(北偏东)
                Point2D dest1 = calculator1.getDestinationGeographicPoint();
                double x1 = dest1.getX();
                double y1 = dest1.getY();
                Coordinate coordinate1 = new Coordinate();
                coordinate1.setX(x1);
                coordinate1.setY(y1);
                Point point1 = geometryFactory.createPoint(coordinate1);


                lineCoordinates.add(coordinate);
                lineCoordinates.add(coordinate1);
                LineString towPointsLineString = geometryFactory.createLineString(lineCoordinates.toArray(new Coordinate[lineCoordinates.size()]));


                // 计算二阶贝塞尔曲线所需的控制点
                GeodeticCalculator controlPointCalculator = new GeodeticCalculator();
                controlPointCalculator.setStartingGeographicPoint(longitude, latitude);

                if (starta > 0 && starta < 180) {
                    controlPointCalculator.setDirection(angle1 + 90, width); // azimuth：方位角(北偏东)
                } else if (starta > -90 && starta < 0) {
                    controlPointCalculator.setDirection(angle1 + 90, width); // azimuth：方位角(北偏东)
                } else {
                    controlPointCalculator.setDirection(angle1 - 90, width); // azimuth：方位角(北偏东)
                }

                Point2D controlPointDest = controlPointCalculator.getDestinationGeographicPoint();
                double controlPointX = controlPointDest.getX();
                double controlPointY = controlPointDest.getY();
                Coordinate controlPointCoordinate = new Coordinate();
                controlPointCoordinate.setX(controlPointX);
                controlPointCoordinate.setY(controlPointY);
                Point controlPoint = geometryFactory.createPoint(controlPointCoordinate);

                LineString cubicBezierCurve = BuildGeometryUtil.buildCubicBezierCurve(point, controlPoint, point1, 100);
                featureBuilder.set(allGeoJsonPropertiesSet.size(), cubicBezierCurve);

                /**
                 * 以下set方法仅供测试使用，且循环内唯一
                 */
//              featureBuilder.set(allGeoJsonPropertiesSet.size(), point1);
//              featureBuilder.set(allGeoJsonPropertiesSet.size(), towPointsLineString);
//              testFeatureBuilderSetArcCenterPoint(longitude,latitude,geometryFactory,allGeoJsonPropertiesSet,featureBuilder);
//              testFeatureBuilderSetArcCenterPointBuffer(longitude,latitude,geometryFactory,allGeoJsonPropertiesSet,featureBuilder,width);
            }
        }
        SimpleFeature simpleFeature = featureBuilder.buildFeature(null);
        return simpleFeature;
    }

    /**
     * 测试构造arc的中心点
     */
    private static void testFeatureBuilderSetArcCenterPoint(double longitude, double latitude, GeometryFactory geometryFactory, ArrayList<String> allGeoJsonPropertiesSet, SimpleFeatureBuilder featureBuilder) {
        Coordinate centerPointCoordinate = new Coordinate();
        centerPointCoordinate.setX(longitude);
        centerPointCoordinate.setY(latitude);
        Point centerPoint = geometryFactory.createPoint(centerPointCoordinate);
        featureBuilder.set(allGeoJsonPropertiesSet.size(), centerPoint);
    }

    /**
     * 测试构造arc的中心点的buffer
     */
    private static void testFeatureBuilderSetArcCenterPointBuffer(double longitude, double latitude, GeometryFactory geometryFactory, ArrayList<String> allGeoJsonPropertiesSet, SimpleFeatureBuilder featureBuilder, Double width) {
        Geometry buffer4326 = BuildGeometryUtil.buildCircle(longitude, latitude, width, CommonEnum.DEFAULT_ELLIPSE_NSIDES);
        Coordinate[] bufferCoordinates = buffer4326.getCoordinates();
        LineString lineString = geometryFactory.createLineString(bufferCoordinates);
        featureBuilder.set(allGeoJsonPropertiesSet.size(), lineString);
    }

    private static List<Coordinate> pointsJSONArray2CoordinatesList(JSONArray jsonArray) {
        List<Coordinate> coordinates = new ArrayList<>();
        for (int j = 0; j < jsonArray.size(); j++) {
            JSONObject coord = convertCoord(jsonArray.getJSONObject(j));
            double longitude = coord.getDouble(CommonEnum.LONGITUDE);
            double latitude = coord.getDouble(CommonEnum.LATITUDE);
            Coordinate coordinate = new Coordinate();
            coordinate.setX(longitude);
            coordinate.setY(latitude);
            coordinates.add(coordinate);
        }
        return coordinates;
    }

    private static Point convertPointJson2PointGeo(JSONObject jsonObject) {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        JSONObject pointObject = convertCoord(jsonObject);
        Coordinate pointCoordinate = new Coordinate();
        pointCoordinate.setX(pointObject.getDouble(CommonEnum.LONGITUDE));
        pointCoordinate.setY(pointObject.getDouble(CommonEnum.LATITUDE));
        return geometryFactory.createPoint(pointCoordinate);
    }

    private static JSONObject convertCoord(JSONObject point) {
        String lon = point.getStr(CommonEnum.LONGITUDE);  // E1084813
        String lat = point.getStr(CommonEnum.LATITUDE);
        JSONObject jsonObject = new JSONObject();
        jsonObject.set(CommonEnum.LONGITUDE, CoordinateConverter.dmsToDecimalDegrees(lon));
        jsonObject.set(CommonEnum.LATITUDE, CoordinateConverter.dmsToDecimalDegrees(lat));
        return jsonObject;
    }
}

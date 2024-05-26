package org.gislogic.mvt.vectortile.core;


import org.gislogic.mvt.vectortile.pojo.CustomFeature;
import org.gislogic.mvt.vectortile.pojo.MapboxVectorTileFeature;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import java.util.*;


public final class MapboxVectorTileLayer {
    public final List<MapboxVectorTileFeature> mapboxVectorTileFeatureList = new LinkedList<>();
    private final Map<String, Integer> attributeNameLinkedHashMap = new LinkedHashMap<>();   // 存储字符串键和它们对应的整数值，键的插入顺序将被保留
    private final Map<Object, Integer> attributeValueLinkedHashMap = new LinkedHashMap<>();
    private final MapboxVectorTileBuilder mapboxVectorTileBuilder;

    public MapboxVectorTileLayer(MapboxVectorTileBuilder mapboxVectorTileBuilder) {
        this.mapboxVectorTileBuilder = mapboxVectorTileBuilder;
    }

    /**
     * 给图层添加Feature
     *
     * @param customFeature                   自定义要素，包含properties和geometry
     * @param simplificationDistanceTolerance DP简化阈值
     * @param curZ                            当前zoom
     * @param minZ                            最小zoom
     */
    public void addFeature(CustomFeature customFeature, Double simplificationDistanceTolerance, byte curZ, byte minZ) {
        // 先简化再裁剪比先裁剪再简化效率要高
        simplifyGeometry(customFeature, simplificationDistanceTolerance, curZ, minZ);
        addMapboxVectorTileFeature2List(customFeature.getProperties(), getIntersectionOfTileAndFeature(customFeature.getGeometry()));
    }

    /**
     * 给图层添加Feature
     *
     * @param customFeature 自定义要素，包含properties和geometry
     */
    public void addFeature(CustomFeature customFeature) {
        addMapboxVectorTileFeature2List(customFeature.getProperties(), getIntersectionOfTileAndFeature(customFeature.getGeometry()));
    }

    /**
     * 简化几何
     *
     * @param customFeature                   自定义要素，包含properties和geometry
     * @param simplificationDistanceTolerance DP简化阈值
     * @param curZ                            当前zoom
     * @param minZ                            最小zoom
     */
    private void simplifyGeometry(CustomFeature customFeature, Double simplificationDistanceTolerance, byte curZ, byte minZ) {
        Geometry geometry = customFeature.getGeometry();
        /**
         * Geometry类型：
         * "LineString"、"MultiLineString" DouglasPeuckerSimplifier简化
         * "Polygon"、"MultiPolygon" 先根据 DouglasPeuckerSimplifier 简化，若简化后的结果 属于 "Polygon"、"MultiPolygon" ，则保存简化结果，否则使用 TopologyPreservingSimplifier 简化
         * "Point" 不简化
         * "MultiPoint"、"LinearRing"、"GeometryCollection"   使用 TopologyPreservingSimplifier 简化
         */
        if (simplificationDistanceTolerance > 0.0 && !(geometry instanceof Point)) {
            if (curZ < minZ) {  // 当前瓦片的 zoom 小于最小 zoom 时才会简化
                if (geometry instanceof LineString || geometry instanceof MultiLineString) {
                    geometry = DouglasPeuckerSimplifier.simplify(geometry, simplificationDistanceTolerance);
                } else if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
                    Geometry simplified = DouglasPeuckerSimplifier.simplify(geometry, simplificationDistanceTolerance);
                    // extra check to prevent polygon converted to line
                    if (simplified instanceof Polygon || simplified instanceof MultiPolygon) {
                        geometry = simplified;
                    } else {
                        geometry = TopologyPreservingSimplifier.simplify(geometry, simplificationDistanceTolerance);
                    }
                } else {
                    geometry = TopologyPreservingSimplifier.simplify(geometry, simplificationDistanceTolerance);
                }
                customFeature.setGeometry(geometry);
            }
        }
    }


    private void addMapboxVectorTileFeature2List(Map<String, ?> attributes, Geometry clipedGeometry) {
        if (null == clipedGeometry || clipedGeometry.isEmpty()) {
            return;//裁剪完没有交集则直接return
        }
        // 转换并添加feature
        ArrayList<Integer> tags = attributeMap2TagsList(attributes);
        List<Geometry> resultGeometries = new LinkedList<>();
        resolveGeometryCollection(clipedGeometry, resultGeometries);
        for (Geometry resultGeometry : resultGeometries) {
            MapboxVectorTileFeature feature = new MapboxVectorTileFeature();
            feature.geometry = resultGeometry;
            feature.tags = tags;
            mapboxVectorTileFeatureList.add(feature);
        }
    }

    /**
     * 处理 GeometryCollection ，拆分为若干个 Geometry 加入到List中
     */
    private void resolveGeometryCollection(Geometry geometry, List<Geometry> resultGeometries) {
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            Geometry subGeometry = geometry.getGeometryN(i);
            if (subGeometry.getClass().equals(GeometryCollection.class)) {
                resolveGeometryCollection(subGeometry, resultGeometries);
            } else {
                resultGeometries.add(subGeometry);
            }
        }
    }

    /**
     * attributeMap转为TagsList
     */
    private ArrayList<Integer> attributeMap2TagsList(Map<String, ?> attributes) {
        if (null == attributes) {   // 该Feature没有属性
            return null;
        }

        Integer maxSize = attributes.size() * 2;    // tags最大容量
        ArrayList<Integer> tagsList = new ArrayList<>(maxSize);

        for (Map.Entry<String, ?> attribute : attributes.entrySet()) {
            // skip attribute without value
            Object attributeValue = attribute.getValue();
            if (attributeValue == null) {
                continue;
            }
            String attributeName = attribute.getKey();
            tagsList.add(getIntegerKeyOfTheMappingForAttributeName(attributeName));
            tagsList.add(getIntegerKeyOfTheMappingForAttributeValue(attributeValue));
        }
        return tagsList;
    }


    /**
     * 获取属性名映射的整数key，如果不存在，返回当前属性名链表的大小
     */
    private Integer getIntegerKeyOfTheMappingForAttributeName(String attributeName) {
        return attributeNameLinkedHashMap.computeIfAbsent(attributeName, k -> attributeNameLinkedHashMap.size());
    }

    public Set<String> keys() {
        return attributeNameLinkedHashMap.keySet();
    }

    /**
     * 获取属性值映射的整数key，如果不存在，返回当前属性值链表的大小
     */
    private Integer getIntegerKeyOfTheMappingForAttributeValue(Object attributeValue) {
        return attributeValueLinkedHashMap.computeIfAbsent(attributeValue, k -> attributeValueLinkedHashMap.size());
    }

    public Set<Object> values() {
        return attributeValueLinkedHashMap.keySet();
    }


    /**
     * 用瓦片裁剪Feature
     *
     * @param geometry
     * @return
     */
    private Geometry getIntersectionOfTileAndFeature(Geometry geometry) {
        try {
            return mapboxVectorTileBuilder.tileClip.intersection(geometry);
        } catch (TopologyException e) {
            geometry = geometry.buffer(0);
            return mapboxVectorTileBuilder.tileClip.intersection(geometry);
        }
    }

}

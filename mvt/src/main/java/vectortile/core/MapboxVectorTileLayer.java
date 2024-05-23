package vectortile.core;


import org.locationtech.jts.geom.*;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import vectortile.pojo.CustomFeature;
import vectortile.pojo.MapboxVectorTileFeature;

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
        customFeature = simplifyGeometry(customFeature, simplificationDistanceTolerance, curZ, minZ);
        addCipedGeometryAndAttributes(customFeature.getProperties(), clipGeometry(customFeature.getGeometry()));
    }

    /**
     * 简化几何
     *
     * @param customFeature                   自定义要素，包含properties和geometry
     * @param simplificationDistanceTolerance DP简化阈值
     * @param curZ                            当前zoom
     * @param minZ                            最小zoom
     * @return
     */
    private CustomFeature simplifyGeometry(CustomFeature customFeature, Double simplificationDistanceTolerance, byte curZ, byte minZ) {
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
        return customFeature;
    }


    private void addCipedGeometryAndAttributes(Map<String, ?> attributes, Geometry clipedGeometry) {
        if (null == clipedGeometry || clipedGeometry.isEmpty()) {
            return;//裁剪完没有交集则直接return
        }
        // 转换并添加feature
        ArrayList<Integer> tags = attributeMap2TagsList(attributes);
        List<Geometry> resolveGeometries = new LinkedList<>();
        resolveGeometryCollection(clipedGeometry, resolveGeometries);
        for (Geometry resolveGeometry : resolveGeometries) {
            MapboxVectorTileFeature feature = new MapboxVectorTileFeature();
            feature.geometry = resolveGeometry;
            feature.tags = tags;
            mapboxVectorTileFeatureList.add(feature);
        }
    }

    //拆出GeometryCollection中的geometry塞到list中
    private void resolveGeometryCollection(Geometry geometry, List<Geometry> resolveGeometries) {
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            Geometry subGeometry = geometry.getGeometryN(i);
            if (subGeometry.getClass().equals(GeometryCollection.class)) {
                resolveGeometryCollection(subGeometry, resolveGeometries);
            } else {
                resolveGeometries.add(subGeometry);
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


    private Geometry clipGeometry(Geometry geometry) {
        try {
            return mapboxVectorTileBuilder.tileClip.intersection(geometry);
        } catch (TopologyException e) {
            geometry = geometry.buffer(0);
            return mapboxVectorTileBuilder.tileClip.intersection(geometry);
        }
    }

}

/*****************************************************************
 *  Copyright (c) 2022- "giscat by 刘雨 (https://github.com/codingmiao/giscat)"
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package vectortile.core;


import org.locationtech.jts.geom.*;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import vectortile.pojo.Feature;

import java.util.*;

/**
 * mvt layer
 *
 * @author liuyu
 * @date 2022/4/24
 */
public final class MvtLayer {

    public final List<MvtFeature> features = new LinkedList<>();

    private final Map<String, Integer> keys = new LinkedHashMap<>();
    private final Map<Object, Integer> values = new LinkedHashMap<>();

    private final MvtBuilder mvtBuilder;


    /**
     * @param mvtBuilder mvtBuilder
     */
    public MvtLayer(MvtBuilder mvtBuilder) {
        this.mvtBuilder = mvtBuilder;
    }


    private Feature simplifyGeometry(Feature feature, Double simplificationDistanceTolerance, byte curZ, byte minZ) {
        Geometry geometry = feature.getGeometry();
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
                feature.setGeometry(geometry);
            }
        }
        return feature;
    }

    public void addFeature(Feature feature, Double simplificationDistanceTolerance, byte curZ, byte minZ) {
        // 先简化再裁剪比先裁剪再简化效率要高
        feature = simplifyGeometry(feature, simplificationDistanceTolerance, curZ, minZ);
        addCipedGeometryAndAttributes(feature.getProperties(), clipGeometry(feature.getGeometry()));
    }

    public void addCipedGeometryAndAttributes(Map<String, ?> attributes, Geometry clipedGeometry) {
        if (null == clipedGeometry || clipedGeometry.isEmpty()) {
            return;//裁剪完没有交集则直接return
        }
        // 转换并添加feature
        ArrayList<Integer> tags = tags(attributes);
        List<Geometry> resolveGeometries = new LinkedList<>();
        resolveGeometryCollection(clipedGeometry, resolveGeometries);
        for (Geometry resolveGeometry : resolveGeometries) {
            addSampleGeometryFeature(tags, resolveGeometry);
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


    private void addSampleGeometryFeature(ArrayList<Integer> tags, Geometry geometry) {

        MvtFeature feature = new MvtFeature();
        feature.geometry = geometry;

        feature.tags = tags;

        features.add(feature);
    }

    //将attributes转为tags以便加入到feature
    private ArrayList<Integer> tags(Map<String, ?> attributes) {
        if (null == attributes) {
            return null;
        }
        ArrayList<Integer> tags = new ArrayList<>(attributes.size() * 2);
        for (Map.Entry<String, ?> e : attributes.entrySet()) {
            // skip attribute without value
            if (e.getValue() == null) {
                continue;
            }
            tags.add(key(e.getKey()));
            tags.add(value(e.getValue()));
        }
        return tags;
    }

    private Integer key(String key) {
        return keys.computeIfAbsent(key, k -> keys.size());
    }

    public Set<String> keys() {
        return keys.keySet();
    }

    private Integer value(Object value) {
        return values.computeIfAbsent(value, k -> values.size());
    }

    public Set<Object> values() {
        return values.keySet();
    }


    private Geometry clipGeometry(Geometry geometry) {
        try {
            return mvtBuilder.tileClip.intersection(geometry);
        } catch (TopologyException e) {
            geometry = geometry.buffer(0);
            return mvtBuilder.tileClip.intersection(geometry);
        }
    }

}

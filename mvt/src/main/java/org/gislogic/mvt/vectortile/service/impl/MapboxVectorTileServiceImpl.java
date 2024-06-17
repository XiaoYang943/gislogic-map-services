package org.gislogic.mvt.vectortile.service.impl;


import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.gislogic.mvt.vectortile.core.MapboxVectorTileBuilder;
import org.gislogic.mvt.vectortile.core.MapboxVectorTileLayer;
import org.gislogic.mvt.vectortile.pojo.CustomFeature;
import org.gislogic.mvt.vectortile.service.MapboxVectorTileService;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
public class MapboxVectorTileServiceImpl implements MapboxVectorTileService {
    /**
     * @param mapboxVectorTileBuilder
     * @param layer
     * @param featureCollection
     * @param z
     */
    @Override
    public void build(MapboxVectorTileBuilder mapboxVectorTileBuilder, MapboxVectorTileLayer layer, SimpleFeatureCollection featureCollection, byte z) {
        SimpleFeatureIterator iterator = featureCollection.features();
        while (iterator.hasNext()) {    // 遍历源数据的每一个 Feature
            SimpleFeature simpleFeature = iterator.next();
            List<Object> attributes = simpleFeature.getAttributes();
            SimpleFeatureType featureType = simpleFeature.getFeatureType();
            List<AttributeDescriptor> attributeDescriptors = featureType.getAttributeDescriptors();

            HashMap<String, Object> map = new HashMap<>();  // tags
            for (int i = 0; i < attributes.size(); i++) {
                AttributeDescriptor attributeDescriptor = attributeDescriptors.get(i);
                map.put(String.valueOf(attributeDescriptor.getName()), attributes.get(i));
            }
            Geometry geometry = (org.locationtech.jts.geom.Geometry) simpleFeature.getDefaultGeometry();

            // 如果当前 Feature 的 geometry 和当前zxy的瓦片相交
            if (mapboxVectorTileBuilder.getTileBBox().envIntersects(geometry)) {
                layer.addFeature(new CustomFeature(geometry, map), 0.05, z, (byte) 5);   // 给图层添加当前 Feature
            }
        }
        iterator.close();
    }
}

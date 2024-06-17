package org.gislogic.mvt.vectortile.service;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.gislogic.mvt.vectortile.core.MapboxVectorTileBuilder;
import org.gislogic.mvt.vectortile.core.MapboxVectorTileLayer;

public interface MapboxVectorTileService {
    void build(MapboxVectorTileBuilder mapboxVectorTileBuilder, MapboxVectorTileLayer layer, SimpleFeatureCollection featureCollection, byte z);
}

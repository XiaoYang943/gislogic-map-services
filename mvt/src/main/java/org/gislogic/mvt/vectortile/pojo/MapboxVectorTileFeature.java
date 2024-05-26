package org.gislogic.mvt.vectortile.pojo;

import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;

public final class MapboxVectorTileFeature {

    public Geometry geometry;
    public ArrayList<Integer> tags;
}

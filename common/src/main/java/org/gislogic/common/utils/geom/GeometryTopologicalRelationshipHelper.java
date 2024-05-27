package org.gislogic.common.utils.geom;

import org.locationtech.jts.geom.Polygon;

public class GeometryTopologicalRelationshipHelper {
    /**
     * 判断两个 Polygon 是否相交
     *
     * @return boolean
     */
    public static boolean isIntersect(Polygon polygon1, Polygon polygon2) {
        return polygon1.intersects(polygon2) || polygon1.contains(polygon2) || polygon1.within(polygon2);
    }
}

package utils.geom;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

public class GeometryValidator {
    public boolean shouldClosePath(Geometry geometry) {
        return (geometry instanceof Polygon) || (geometry instanceof LinearRing);
    }
}

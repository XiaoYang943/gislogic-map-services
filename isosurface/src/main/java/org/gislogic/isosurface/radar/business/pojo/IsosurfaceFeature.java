package org.gislogic.isosurface.radar.business.pojo;

import lombok.Data;
import org.locationtech.jts.geom.Polygon;

@Data
public class IsosurfaceFeature {
    private Polygon polygon;
    private double value;
}

package org.gislogic.mvt.vectortile.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

/**
 * 自定义要素，包含properties和geometry
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CustomFeature {
    private Geometry geometry;
    private Map<String, Object> properties;
}

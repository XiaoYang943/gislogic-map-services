package vectortile.pojo;

import org.locationtech.jts.geom.Geometry;

import java.util.Map;

/**
 * 自定义要素，包含properties和geometry
 */
public class CustomFeature {
    private Geometry geometry;
    private Map<String, Object> properties;

    public CustomFeature(Geometry geometry, Map<String, Object> properties) {
        this.geometry = geometry;
        this.properties = properties;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}

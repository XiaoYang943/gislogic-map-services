package utils.converter;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SimpleFeatureCollectionConverter {
    /**
     * GeoJSON 转换为 SimpleFeatureCollection
     *
     * @param jsonFilePath GeoJSON文件路径
     * @return SimpleFeatureCollection
     */
    public static SimpleFeatureCollection convertGeoJSON2SimpleFeatureCollection(String jsonFilePath) {
        FeatureJSON featureJSON = new FeatureJSON();
        try {
            FileInputStream fileInputStream = new FileInputStream(jsonFilePath);
            try {
                return (SimpleFeatureCollection) featureJSON.readFeatureCollection(fileInputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}

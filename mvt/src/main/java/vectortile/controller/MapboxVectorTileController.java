package vectortile.controller;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vectortile.core.MvtBuilder;
import vectortile.core.MvtLayer;
import vectortile.pojo.Feature;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import static utils.converter.SimpleFeatureCollectionConverter.convertGeoJSON2SimpleFeatureCollection;


@RestController()
@RequestMapping("/tile")
@CrossOrigin
public class MapboxVectorTileController {
    private static final GeometryFactory geometryFactory = new GeometryFactory();   // 几何工厂

    private static final String vtContentType = "application/octet-stream"; // 二进制数据流的MIME类型

    @RequestMapping("/{z}/{x}/{y}")
    public void getMapboxVectorTile(@PathVariable byte z, @PathVariable int x, @PathVariable int y, HttpServletResponse response) {
        MvtBuilder mvtBuilder = new MvtBuilder(z, x, y, geometryFactory);   // 构造 MvtBuilder
        MvtLayer layer = mvtBuilder.getOrCreateLayer("省区域");    // 创建图层

        SimpleFeatureCollection featureCollection = convertGeoJSON2SimpleFeatureCollection("C:\\Users\\heyiyang\\IdeaProjects\\gislogic-map-services\\mvt\\src\\main\\resources\\china.json");
        SimpleFeatureIterator iterator = featureCollection.features();
        while (iterator.hasNext()) {    // 遍历源数据的每一个 Feature
            SimpleFeature simpleFeature = iterator.next();
            List<Object> attributes = simpleFeature.getAttributes();
            SimpleFeatureType featureType = simpleFeature.getFeatureType();
            List<AttributeDescriptor> attributeDescriptors = featureType.getAttributeDescriptors();
            HashMap<String, Object> map = new HashMap<>();  // 构造当前 Feature 的 properties 对象
            for (int i = 0; i < attributes.size(); i++) {
                AttributeDescriptor attributeDescriptor = attributeDescriptors.get(i);
                map.put(String.valueOf(attributeDescriptor.getName()), attributes.get(i));
            }
            Geometry geometry = (org.locationtech.jts.geom.Geometry) simpleFeature.getDefaultGeometry();

            if (mvtBuilder.getBbox().envIntersects(geometry)) { // 如果当前 Feature 的 geometery 和当前zxy的瓦片相交
                layer.addFeature(new Feature(geometry, map), 0.05, z, (byte) 5);   // 给图层添加当前 Feature
            }
        }
        iterator.close();

        exportByte(mvtBuilder.toBytes(), vtContentType, response);
    }

    /**
     * 将 bytes 写入 HttpServletResponse
     *
     * @param bytes       编码后的mvt
     * @param contentType 响应的内容类型
     * @param response    HTTP 响应
     */
    private void exportByte(byte[] bytes, String contentType, HttpServletResponse response) {
        response.setContentType(contentType);
        try (OutputStream os = response.getOutputStream()) {
            os.write(bytes);
            os.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}

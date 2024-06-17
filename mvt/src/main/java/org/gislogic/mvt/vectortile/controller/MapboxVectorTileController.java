package org.gislogic.mvt.vectortile.controller;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.gislogic.common.utils.converter.DataFormatConverter;
import org.gislogic.mvt.vectortile.core.MapboxVectorTileBuilder;
import org.gislogic.mvt.vectortile.core.MapboxVectorTileLayer;
import org.gislogic.mvt.vectortile.service.MapboxVectorTileService;
import org.gislogic.mvt.vectortile.util.HttpUtil;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;


@RestController()
@RequestMapping("/tile")
@CrossOrigin
public class MapboxVectorTileController {

    private static final String vtContentType = "application/octet-stream"; // 二进制数据流的MIME类型

    @Resource
    MapboxVectorTileService mapboxVectorTileLayerService;

    @RequestMapping("/{z}/{x}/{y}")
    public void getMapboxVectorTile(@PathVariable byte z, @PathVariable int x, @PathVariable int y, HttpServletResponse response) {

        MapboxVectorTileBuilder mapboxVectorTileBuilder = new MapboxVectorTileBuilder(z, x, y);   // 构造 MapboxVectorTileBuilder

        MapboxVectorTileLayer layer = mapboxVectorTileBuilder.getOrCreateLayer("省区域");    // 创建图层

        SimpleFeatureCollection featureCollection = DataFormatConverter.convertGeoJSON2SimpleFeatureCollection("C:\\Users\\heyiyang\\IdeaProjects\\gislogic-map-services\\mvt\\src\\main\\resources\\china.json");

        mapboxVectorTileLayerService.build(mapboxVectorTileBuilder, layer, featureCollection, z);

        HttpUtil.exportByte(mapboxVectorTileBuilder.toBytes(), vtContentType, response);
    }


}

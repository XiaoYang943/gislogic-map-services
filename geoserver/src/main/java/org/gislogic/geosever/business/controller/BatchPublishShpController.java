package org.gislogic.geosever.business.controller;

import org.gislogic.geosever.business.service.BatchPublishAirnetDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/publish")
public class BatchPublishShpController {

    @Autowired
    BatchPublishAirnetDataService batchPublishAirnetDataService;

    /**
     * 批量发布Airnet的XML数据为GeoServer的WMS服务
     */
    @PostMapping("/shp")
    public void publishShp() {
//        String mapXMLFolder = "C:\\Users\\heyiyang\\文档\\数据\\自动化数据\\20231208\\sys\\sys";
//        String outputFolder = "C:\\Users\\heyiyang\\文档\\数据\\自动化数据\\test-java2";
//        batchPublishAirnetDataService.publishAirnetXml(mapXMLFolder, outputFolder, false, "gis");

        String mapXMLFolder = "C:\\Users\\heyiyang\\文档\\数据\\自动化数据\\自动化地图\\区域底图";
        String outputFolder = "C:\\Users\\heyiyang\\文档\\数据\\自动化数据\\test-java3";
        batchPublishAirnetDataService.publishAirnetXml(mapXMLFolder, outputFolder, false, "acc");
    }
}

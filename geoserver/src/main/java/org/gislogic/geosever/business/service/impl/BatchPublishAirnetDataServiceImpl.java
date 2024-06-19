package org.gislogic.geosever.business.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.PathUtil;
import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import org.gislogic.common.utils.converter.DataFormatConverter;
import org.gislogic.common.utils.file.FileUtils;
import org.gislogic.common.utils.services.BatchPublishShpUtil;
import org.gislogic.geosever.business.config.GeoServerConfig;
import org.gislogic.geosever.business.publishshp.CommonEnum;
import org.gislogic.geosever.business.publishshp.CreateSLDByGeoJSON;
import org.gislogic.geosever.business.publishshp.JSON2GeoJSON;
import org.gislogic.geosever.business.service.BatchPublishAirnetDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;

@Service
public class BatchPublishAirnetDataServiceImpl implements BatchPublishAirnetDataService {
    @Autowired
    GeoServerConfig geoServerConfig;

    @Override
    public void publishAirnetXml(String mapXMLFolder, String outputFolder, Boolean delete, String workSpace) {
        String mapJSONFolder = outputFolder + "\\map-json";
        String geojsonFolder = outputFolder + "\\json-geojson";
        String shpFolder = outputFolder + "\\shp";
        String resultFolder = outputFolder + "\\result";


        ArrayList<String> list = new ArrayList<>();
        list.add(mapJSONFolder);
        list.add(geojsonFolder);
        list.add(shpFolder);
        list.add(resultFolder);
        FileUtils.mkdirIfNotExist(list);

        DataFormatConverter.recursiveTraversalFolders(mapXMLFolder, mapJSONFolder, "map");  // 二所ma转json

        PathUtil.mkdir(Paths.get(mapJSONFolder));

//        Set<String> allGeometryTypeSet = new HashSet<>();
//        JSON2GeoJSON.getAllGeometryType(mapJSONFolder, allGeometryTypeSet); // 统计所有数据的几何类型列表(测试用)

        JSON2GeoJSON.executeJson2GeoJSON(mapJSONFolder, geojsonFolder); // json转GeoJSON

        DataFormatConverter.transformGeoJsonToShpByRecursion(geojsonFolder, shpFolder);   // GeoJSON转shp

        DataFormatConverter.zipAllShapefilesInDirectory(shpFolder, resultFolder);   // 压缩目录下的所有 shp
//
        CreateSLDByGeoJSON.recursiveTraversalFolders(geojsonFolder, resultFolder);   // 创建样式

        try {
            GeoServerRESTManager manager = new GeoServerRESTManager(new URL(geoServerConfig.getUrl()), geoServerConfig.getUsername(), geoServerConfig.getPassword());    // geoserver manager 对象
            GeoServerRESTPublisher publisher = manager.getPublisher();
            boolean createWorkSpace = publisher.createWorkspace(workSpace);
            if (createWorkSpace) {
                System.out.println("创建工作区:" + workSpace + ":" + createWorkSpace + ",新增工作区");
            } else {
                System.out.println("创建工作区:" + workSpace + ":" + createWorkSpace + ",在已有的工作区内更新数据");
            }
            BatchPublishShpUtil.executePublish(manager, resultFolder, workSpace, CommonEnum.SRID);  // 执行发布地图服务 //  注意：记得把county的zip(注意'name'字段还是'地名')和xml加入 resultFolder
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        if (delete) {
            for (String s : list) {
                FileUtil.del(s);
            }
        }
    }
}

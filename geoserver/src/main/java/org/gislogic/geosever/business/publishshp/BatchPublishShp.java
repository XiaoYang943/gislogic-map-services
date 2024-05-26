package org.gislogic.geosever.business.publishshp;

import cn.hutool.core.io.file.PathUtil;
import org.gislogic.common.utils.converter.DataFormatConverter;
import org.gislogic.common.utils.services.BatchPublishShpUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BatchPublishShp {
    private static void mkdirIfNotExist(List<String> pathStrList) {
        pathStrList.forEach((pathStr) -> {
            Path path = Paths.get(pathStr);
            try {
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                } else {
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {

        String mapXMLFolder = "C:\\Users\\heyiyang\\文档\\数据\\自动化数据\\20231208\\sys\\sys";
        String mapJSONFolder = "C:\\Users\\heyiyang\\文档\\数据\\自动化数据\\test-java\\map-json";
        String geojsonFolder = "C:\\Users\\heyiyang\\文档\\数据\\自动化数据\\test-java\\json-geojson";
        String shpFolder = "C:\\Users\\heyiyang\\文档\\数据\\自动化数据\\test-java\\shp";
        String resultFolder = "C:\\Users\\heyiyang\\文档\\数据\\自动化数据\\test-java\\result";

        ArrayList<String> list = new ArrayList<>();
        list.add(mapJSONFolder);
        list.add(geojsonFolder);
        list.add(shpFolder);
        list.add(resultFolder);
        mkdirIfNotExist(list);

        DataFormatConverter.recursiveTraversalFolders(mapXMLFolder, mapJSONFolder, "map");  // 二所ma转json

        PathUtil.mkdir(Paths.get(mapJSONFolder));

//        Set<String> allGeometryTypeSet = new HashSet<>();
//        JSON2GeoJSON.getAllGeometryType(mapJSONFolder, allGeometryTypeSet); // 统计所有数据的几何类型列表(测试用)

        JSON2GeoJSON.executeJson2GeoJSON(mapJSONFolder, geojsonFolder); // json转GeoJSON

        DataFormatConverter.transformGeoJsonToShpByRecursion(geojsonFolder, shpFolder);   // GeoJSON转shp

        DataFormatConverter.zipAllShapefilesInDirectory(shpFolder, resultFolder);   // 压缩目录下的所有 shp
//
        CreateSLDByGeoJSON.recursiveTraversalFolders(geojsonFolder, resultFolder);   // 创建样式

        BatchPublishShpUtil.executePublish(resultFolder, CommonEnum.WORKSPACE, CommonEnum.SRID);  // 执行发布地图服务 //  注意：记得把county的zip(注意'name'字段还是'地名')和xml加入 resultFolder
    }


}

package org.gislogic.common.utils.services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


public class BatchPublishShpUtil {
    /**
     * 本机GeoServer
     */
    private static final String GEOSERVER_URL = "http://localhost:8901/geoserver";
    private static final String GEOSERVER_USERNAME = "admin";
    private static final String GEOSERVER_PASSWORD = "geoserver";
    public static final String DEFAULT_ICON_URL = "/styles/markericon/"; // 本机 icon 地址
    /**
     * demo服务器GeoServer
     */
//    private static final String GEOSERVER_URL = "http://demo.dev.bellavitech.com:7801/geoserver";
//    private static final String GEOSERVER_USERNAME = "admin";
//    private static final String GEOSERVER_PASSWORD = "geoserver";
//    public static final String DEFAULT_ICON_URL = "/styles/markericon/"; // demo 服务器 icon 地址

    /**
     * 生产服务器GeoServer
     */
//    private static final String GEOSERVER_URL = "http://iapp.hcso.bellavitech.com:8080/geoserver";
//    private static final String GEOSERVER_USERNAME = "admin";
//    private static final String GEOSERVER_PASSWORD = "fG0vW5uI4xI8pL6yX2";
//    public static final String DEFAULT_ICON_URL = "/styles/markericon/"; // 生产 服务器 icon 地址
    private static void publish(GeoServerRESTManager manager, String shpPath, String shpStylePath, String workSpace, String layerName, String srs) {
        GeoServerRESTPublisher publisher = manager.getPublisher();
        GeoServerRESTReader reader = manager.getReader();
        GSResourceEncoder.ProjectionPolicy projectionPolicy = GSResourceEncoder.ProjectionPolicy.FORCE_DECLARED;    // 设置投影策略

        boolean existsStore = reader.existsDatastore(workSpace, layerName);
        if (existsStore) {
            boolean removeDatastore = publisher.removeDatastore(workSpace, layerName);  // 删除数据源    // client中，删除数据源，图层也会被删除，但是样式不会被删除
            System.out.println("删除数据源:" + workSpace + "_" + layerName + ":" + removeDatastore);
        }

        boolean existsStyle = reader.existsStyle(layerName);    // 样式貌似是公用的，不存在工作区一说
        if (existsStyle) {
            boolean updateStyleInWorkspace = publisher.removeStyle(layerName);
            System.out.println("删除样式:" + workSpace + "_" + layerName + ":" + updateStyleInWorkspace);
        }

        try {
            boolean publishShp = publisher.publishShp(workSpace, layerName, layerName, new File(shpPath), srs, String.valueOf(projectionPolicy));
            System.out.println("发布Shp:" + workSpace + "_" + layerName + ":" + publishShp);

            boolean publishStyle = publisher.publishStyle(new File(shpStylePath), layerName);
            System.out.println("发布样式:" + workSpace + "_" + layerName + ":" + publishStyle);

            GSLayerEncoder layerEncoder = new GSLayerEncoder();
            layerEncoder.setDefaultStyle(workSpace, layerName);
            boolean configureLayer = publisher.configureLayer(workSpace, layerName, layerEncoder);
            System.out.println("应用样式:" + workSpace + "_" + layerName + ":" + configureLayer);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 发布shp为wms服务
     *
     * @param shpAndStyleDirectoryPath shp数据压缩包和sld样式文件所在的文件夹(注意：文件名必须一样，只是后缀不同)
     * @param workspace                要发布服务的工作空间
     * @param crs                      坐标系
     */
    public static void executePublish(String shpAndStyleDirectoryPath, String workspace, String crs) {
        /**
         * 设置日志级别
         */
        ((LoggerContext) LoggerFactory.getILoggerFactory())
                .getLoggerList()
                .forEach(logger -> logger.setLevel(Level.ERROR));

        try {
            GeoServerRESTManager manager = new GeoServerRESTManager(new URL(GEOSERVER_URL), GEOSERVER_USERNAME, GEOSERVER_PASSWORD);    // geoserver manager 对象
            File directory = new File(shpAndStyleDirectoryPath);
            // 创建一个Map来存储文件名和对应的后缀
            Map<String, String> fileMap = new HashMap<>();
            if (directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            String fileName = file.getName();
                            String nameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
                            String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
                            // 如果文件名已经存在于Map中，且后缀不同，则执行某些操作
                            if (fileMap.containsKey(nameWithoutExtension)) {
                                String existingExtension = fileMap.get(nameWithoutExtension);
                                if (!existingExtension.equals(extension)) {
                                    String layerName = fileName.substring(0, fileName.lastIndexOf("."));
                                    String shpPath = file.getAbsolutePath();
                                    String shpStylePath = file.getParent() + "\\" + layerName + ".xml";
//                                System.out.println(shpPath);
//                                System.out.println(shpStylePath);
//                                System.out.println(layerName);
                                    /**
                                     * 发布流程如下：删除数据源、删除样式、发布shp、发布样式、应用样式
                                     * 当读取到同一个图层的xml和zip后，执行发布
                                     * 否则可能会出现以下异常情况
                                     * 1. 应用样式时该图层还没有发布
                                     * 2. 发布样式时该样式还没有被删除(尝试过更新样式，但是更新结果为false)
                                     */
                                    publish(manager, shpPath, shpStylePath, workspace, layerName, crs);
                                }
                            } else {
                                // 如果文件名不在Map中，则将文件名和后缀存入Map
                                fileMap.put(nameWithoutExtension, extension);
                            }
                        }
                    }
                }
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }


    }


}

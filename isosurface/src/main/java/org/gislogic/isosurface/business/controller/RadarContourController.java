package org.gislogic.isosurface.business.controller;


import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.gislogic.common.utils.database.DataStorageInDatabaseUtil;
import org.gislogic.isosurface.business.domain.RadarCrPartitionRelationshipEntity;
import org.gislogic.isosurface.business.domain.RadarCrReq;
import org.gislogic.isosurface.business.domain.RadarTrainDataEntity;
import org.gislogic.isosurface.business.service.RadarCrPartitionRelationshipService;
import org.gislogic.isosurface.configuration.GeoToolsPostgisConfiguration;
import org.gislogic.isosurface.utils.InputDataProcessUtil;
import org.gislogic.isosurface.utils.PostgisDataStoreUtil;
import org.gislogic.isosurface.utils.RadarColorEnum;
import org.gislogic.isosurface.utils.RadarEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;

import static org.gislogic.isosurface.utils.CreateIsosurfaceUtil.equiSurface;


@RestController
@RequestMapping("/radar")
public class RadarContourController {

    @Autowired
    RadarCrPartitionRelationshipService radarCrPartitionRelationshipService;

    /**
     * 2024-2-2
     * Timer 获取训练数据 took 298 ms
     * Timer 生成等值面 took 25684 ms
     * Timer 创建分区表 took 424 ms
     * Timer 给分区表写入数据 took 13063 ms 迭代器写入
     *
     * Timer 获取训练数据 took 232 ms
     * Timer 生成等值面 took 26414 ms
     * Timer 创建分区表 took 434 ms
     * Timer 给分区表写入数据 took 6718 ms  batch批量写入
     *
     */
    /**
     * 请求示例
     * {
     * "dataTime": "20240204061542",
     * "fileTime": "2024020406",
     * "fileStation": "9915",
     * "filePath": "ftp_target_zlxy/2024/02/04/06/Z_RADR_I_Z9915_20240204061542_O_DOR_CB_CAP_FMT.bin.bz2.json"
     * }
     */
    @PostMapping("/add2")
    public boolean calContour2(@RequestBody RadarCrReq req) {
        final TimeInterval timer = new TimeInterval();

        try {
            timer.start("getTrainingDataByJsonFile");
            /**
             * 从PyCWR生成的格网json中提取训练数据
             */
//            String filePath = "D:\\my-store\\resources\\data\\contour\\input\\" + req.getFilePath(); // TODO:集成到系统中后，需要改 Base 地址

            String filePath = req.getFilePath(); // TODO:集成到系统中后，需要改 Base 地址

            if (!new File(filePath).exists()) {
                System.out.println(filePath + "不存在");
                return false;
            }
            RadarTrainDataEntity trainData = radarCrPartitionRelationshipService.getTrainingDataByJsonFile(filePath, "UTF-8", "lon", "lat", "value", "config");
            Console.log("Timer 获取训练数据 took {} ms", timer.intervalMs("getTrainingDataByJsonFile"));


            //        File file = new File(filePath);
//        String fileName = file.getName();
//        String tableName = fileName.substring(0, fileName.lastIndexOf('.'));
//        System.out.println(tableName);

            RadarEntity radarEntity = new RadarEntity(null, null, req.getFileTime(), req.getDataTime(), req.getFileStation());

            timer.start("equiSurface");
            /**
             * 根据训练数据生成等值面
             */
            double[] dataInterval = RadarColorEnum.getValueArray();
            FeatureCollection featureCollection = equiSurface(trainData, dataInterval, radarEntity);
            Console.log("Timer 生成等值面 took {} ms", timer.intervalMs("equiSurface"));


//            timer.start("createPartitionTable");
            /**
             * 创建分区表
             */
            RadarCrPartitionRelationshipEntity radarCrPartitionRelationshipEntity = new RadarCrPartitionRelationshipEntity();
            radarCrPartitionRelationshipEntity.setSchema("gis_radar");
            radarCrPartitionRelationshipEntity.setSubTableName("radar_cr_zlxy3" + "_" + req.getFileTime());
            radarCrPartitionRelationshipEntity.setMainTableName("radar_cr_zlxy3");
            radarCrPartitionRelationshipEntity.setRelationshipPrimaryKey(req.getFileTime());
            radarCrPartitionRelationshipService.createPartitionTable(radarCrPartitionRelationshipEntity);
            Console.log("Timer 创建分区表 took {} ms", timer.intervalMs("createPartitionTable"));


            timer.start("writeSimpleFeatureCollection2pg");
            /**
             * 给分区表写入等值面数据
             */
//            DataStore dataStore = PostgisDataStore.getInstance();
            DataStore dataStore = new PostgisDataStoreUtil().getPostgisDataStore(geoToolsPostgisConfiguration);
            boolean bool = DataStorageInDatabaseUtil.writeSimpleFeatureCollection2pgByBatch((SimpleFeatureCollection) featureCollection, "radar_cr_zlxy3", dataStore, false);
            Console.log("Timer 给分区表写入数据 took {} ms", timer.intervalMs("writeSimpleFeatureCollection2pg"));

            return bool;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 请求示例
     * {
     * "dataTime": "20240204061542",
     * "fileTime": "2024020406",
     * "fileStation": "9915",
     * "filePath": "ftp_target_zlxy/2024/02/04/06/Z_RADR_I_Z9915_20240204061542_O_DOR_CB_CAP_FMT.bin.bz2.json"
     * }
     */
    @Autowired
    private GeoToolsPostgisConfiguration geoToolsPostgisConfiguration;

    @PostMapping("/test1")
    public void test1() {

        File directory = new File("D:\\my-store\\gis\\data\\radar\\output");

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".json")) {
                    String path = file.getPath();
                    System.out.println(path);
//                    D:\radar\output\Z_RADR_I_Z9290_20240204074446_O_DOR_CB_CAP_FMT.bin.bz2.json
                    String s = path.split("Z_RADR_I_Z")[1];
                    String fileStation = s.split("_")[0];
                    String fileTime = s.split("_")[1].split("_")[0].substring(0, 10);
                    String dataTime = s.split("_")[1].split("_")[0].substring(0, 12);
                    System.out.println(fileStation);
                    System.out.println(fileTime);
                    System.out.println(dataTime);

                    RadarEntity radarEntity = new RadarEntity(null, null, fileTime, dataTime, fileStation);

                    RadarTrainDataEntity trainData = InputDataProcessUtil.getTrainingDataByJsonFile(path, "UTF-8", "lon", "lat", "value", "config");
                    double[] dataInterval = RadarColorEnum.getValueArray();
                    FeatureCollection featureCollection = equiSurface(trainData, dataInterval, radarEntity);


                    /**
                     * 每次写入数据之前建表
                     * 也可以写定时任务，每天建后三天的表，删前三天的表
                     //                    **/
                    RadarCrPartitionRelationshipEntity radarCrPartitionRelationshipEntity = new RadarCrPartitionRelationshipEntity();
                    radarCrPartitionRelationshipEntity.setSchema("gis_radar");
                    radarCrPartitionRelationshipEntity.setSubTableName("radar_cr_zlxy3" + "_" + fileTime);
//                    radarCrPartitionRelationshipEntity.setSubTableName(fileTime);
                    radarCrPartitionRelationshipEntity.setMainTableName("radar_cr_zlxy3");
                    radarCrPartitionRelationshipEntity.setRelationshipPrimaryKey(fileTime);
                    radarCrPartitionRelationshipService.createPartitionTable(radarCrPartitionRelationshipEntity);
//                    给pg分区表写入数据时，报错：org.postgresql.util.PSQLException: ERROR: no partition of relation "radar_cr_zlxy3" found for row 详细：Partition key of the failing row contains (file_time) = (202402010602).
                    DataStore postgisDataStore = new PostgisDataStoreUtil().getPostgisDataStore(geoToolsPostgisConfiguration);
                    boolean testRadar = DataStorageInDatabaseUtil.writeSimpleFeatureCollection2pgByBatch((SimpleFeatureCollection) featureCollection, "radar_cr_zlxy3", postgisDataStore, false);
                    System.out.println(testRadar);

                }
            }
        }
    }
}

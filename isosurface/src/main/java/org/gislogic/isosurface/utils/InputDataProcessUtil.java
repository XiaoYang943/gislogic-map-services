package org.gislogic.isosurface.utils;


import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.gislogic.isosurface.radar.business.pojo.GridData;

import java.io.File;
import java.nio.charset.Charset;

/**
 * @description: 生成等值面方法的入参数据处理工具类
 * @author: hyy
 * @create: 2024-02-12
 **/

public class InputDataProcessUtil {
    /**
     * @param gridDataJsonFilePath 格网数据JSON文件路径
     * @param charset              文件字符集
     * @param lonFieldName         经度字段名称
     * @param latFieldName         纬度字段名称
     * @param valueFieldName       值字段名称
     * @param configFieldName      其他字段map名称
     * @return RadarTrainDataEntity 训练数据
     * @description 从格网JSON文件中获取 wContour 所需的训练数据
     * @date 2024-02-12
     * @author hyy
     * 格网数据JSON示例：(请遵循约定提供入参数据)
     * {
     * "lon": [106.79257315831812, 106.81257315831812, 106.83257315831811,...], // 注意：经度是从左到右递增排列(格网间隔：0.02度)
     * "lat": [32.64741393013443, 32.66741393013443, 32.68741393013443,...],   // 注意：纬度是从下到上递增排列(格网间隔：0.02度)
     * "value": [
     * [5.5,7.3,...]
     * [1.1,-9999,...]   // 注意：value 字段是包含无效值的（浮点型），且经纬度中该无效值的经纬度也要占位，不能省略，后续再把无效值的 MultiPolygon 要素删掉即可
     * ]
     * "config": {
     * "invalidValue": -9999,   // 无效值
     * "cellWidth": 0.02   // cell width
     * }
     * }
     **/
    public static GridData getTrainingDataByJsonFile(String gridDataJsonFilePath, String charset, String lonFieldName, String latFieldName, String valueFieldName, String configFieldName) {

        File file = FileUtil.newFile(gridDataJsonFilePath);
        JSONObject jsonObject = JSONUtil.readJSONObject(file, Charset.forName(charset));

        JSONArray lonArray = jsonObject.getJSONArray(lonFieldName);
        JSONArray latArray = jsonObject.getJSONArray(latFieldName);
        JSONArray valueArray = jsonObject.getJSONArray(valueFieldName);
//        JSONObject configObject = jsonObject.getJSONObject(configFieldName);

        double[][] trainData = new double[valueArray.size()][];

        double[] X = new double[lonArray.size()];
        double[] Y = new double[latArray.size()];

        for (int i = 0; i < valueArray.size(); i++) {
            JSONArray jsonArray1 = valueArray.getJSONArray(i);
            double[] row = new double[jsonArray1.size()];
            int j = 0;
            for (Object o : jsonArray1) {
                row[j] = Double.parseDouble(o.toString());
                j++;
            }
            trainData[i] = row;
        }

        for (int i = 0; i < lonArray.size(); i++) {
            Object o = lonArray.get(i);
            X[i] = Double.parseDouble(o.toString());
        }

        for (int i = 0; i < latArray.size(); i++) {
            Object o = latArray.get(i);
            Y[i] = Double.parseDouble(o.toString());
        }

        int[] size = new int[]{X.length, Y.length};

        double[][] trainDataConverted = convertTrainData(trainData);

        GridData gridData = new GridData();
        gridData.setData(trainDataConverted);
        gridData.set_X(X);
        gridData.set_Y(Y);
        gridData.setSize(size);
//        trainDataResult.setConfig(configObject);
        return gridData;
    }

    /**
     * @param trainData 格网值数组
     * @return double 反转后的格网值数组
     * @description 反转格网值数组(训练数据)，否则在调用 Contour.tracingBorders 时报错：java.lang.ArrayIndexOutOfBoundsException
     * @date 2024-02-12
     * @author hyy
     **/
    private static double[][] convertTrainData(double[][] trainData) {
        double[][] trainDataConverted = new double[trainData[0].length][trainData.length];
        for (int i = 0; i < trainData.length; i++) {
            for (int j = 0; j < trainData[0].length; j++) {
                trainDataConverted[j][i] = trainData[i][j];
            }
        }
        return trainDataConverted;
    }
}

package org.gislogic.isosurface.business.domain;

import cn.hutool.json.JSONObject;
import lombok.Data;

@Data
public class RadarTrainDataEntity {
    /**
     * 等值点数据
     * 不做网格运算和插值分析的数组[y][x]
     */
    private double[][] data;

    /**
     * 等值面插值点数量,越多越精细也越慢，[0]=x.length,[1]=y.length
     */
    private int[] size;

    /**
     * X轴集合
     */
    private double[] _X;

    /**
     * Y轴集合
     */
    private double[] _Y;

    private JSONObject config;

}

package org.gislogic.isosurface.radar.business.pojo;

import cn.hutool.json.JSONObject;
import lombok.Data;

/**
 * 网格数据
 */
@Data
public class GridData {
    /**
     * 点
     */
    private double[][] data;

    /**
     * 等值面插值点数量
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

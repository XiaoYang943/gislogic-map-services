package org.gislogic.isosurface.radar.business.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.MultiPolygon;

/**
 * @description: 雷达实体类
 * @author: hyy
 * @create: 2024-03-07
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RadarEntity {
    /**
     * geom字段名称
     */
    public MultiPolygon the_geom;
    /**
     * 等值面value
     * eg：组合反射率值
     */
    public String value;
    /**
     * 气象数据,用于分区 存 年月日小时
     * eg：“2024013104”
     */
    public String file_time;

    /**
     * 气象数据,存完整时间 到分
     * eg：“202401310414”
     */
    public String data_time;

    /**
     * 气象站点
     * eg：“9914”
     */
    public String file_station;
}

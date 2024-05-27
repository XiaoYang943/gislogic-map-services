package org.gislogic.isosurface.radar.business.entity;

import lombok.Data;

/**
 * 雷达等值面，分区表关系
 */
@Data
public class RadarCrPartitionRelationshipEntity {
    /**
     * pg数据库 schema
     */
    String schema;
    /**
     * 分区表，主表表名
     */
    String mainTableName;
    /**
     * 分区表，子表表名
     */
    String subTableName;
    /**
     * 主表和子表的关系，主键的值
     */
    String relationshipPrimaryKey;
}

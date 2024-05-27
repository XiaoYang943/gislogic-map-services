package org.gislogic.isosurface.radar.business.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.gislogic.isosurface.radar.business.entity.RadarCrPartitionRelationshipEntity;

@Mapper
public interface RadarCrPartitionRelationshipMapper {
    Boolean createPartitionTable(RadarCrPartitionRelationshipEntity radarCrPartitionRelationshipEntity);
}

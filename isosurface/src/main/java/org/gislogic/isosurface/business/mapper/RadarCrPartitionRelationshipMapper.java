package org.gislogic.isosurface.business.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.gislogic.isosurface.business.domain.RadarCrPartitionRelationshipEntity;

@Mapper
public interface RadarCrPartitionRelationshipMapper {
    Boolean createPartitionTable(RadarCrPartitionRelationshipEntity radarCrPartitionRelationshipEntity);
}

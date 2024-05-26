package org.gislogic.isosurface.business.mapper;

import cn.gislogic.business.domain.RadarCrPartitionRelationshipEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RadarCrPartitionRelationshipMapper {
    Boolean createPartitionTable(RadarCrPartitionRelationshipEntity radarCrPartitionRelationshipEntity);
}

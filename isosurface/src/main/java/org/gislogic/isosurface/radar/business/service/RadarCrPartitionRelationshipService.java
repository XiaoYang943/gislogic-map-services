package org.gislogic.isosurface.radar.business.service;


import org.gislogic.isosurface.radar.business.entity.RadarCrPartitionRelationshipEntity;
import org.gislogic.isosurface.radar.business.pojo.GridData;

import java.io.IOException;

public interface RadarCrPartitionRelationshipService {
    GridData getTrainingDataByJsonFile(String gridDataJsonFilePath, String charset, String lonFieldName, String latFieldName, String valueFieldName, String configFieldName) throws IOException;

    Boolean createPartitionTable(RadarCrPartitionRelationshipEntity radarCrPartitionRelationshipEntity);
}

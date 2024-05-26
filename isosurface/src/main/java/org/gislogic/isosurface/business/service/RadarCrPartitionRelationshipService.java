package org.gislogic.isosurface.business.service;


import org.gislogic.isosurface.business.domain.RadarCrPartitionRelationshipEntity;
import org.gislogic.isosurface.business.domain.RadarTrainDataEntity;

import java.io.IOException;

public interface RadarCrPartitionRelationshipService {
    RadarTrainDataEntity getTrainingDataByJsonFile(String gridDataJsonFilePath, String charset, String lonFieldName, String latFieldName, String valueFieldName, String configFieldName) throws IOException;

    Boolean createPartitionTable(RadarCrPartitionRelationshipEntity radarCrPartitionRelationshipEntity);
}

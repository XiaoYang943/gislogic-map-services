package org.gislogic.isosurface.business.service.impl;

import org.gislogic.isosurface.business.domain.RadarCrPartitionRelationshipEntity;
import org.gislogic.isosurface.business.domain.RadarTrainDataEntity;
import org.gislogic.isosurface.business.mapper.RadarCrPartitionRelationshipMapper;
import org.gislogic.isosurface.business.service.RadarCrPartitionRelationshipService;
import org.gislogic.isosurface.utils.InputDataProcessUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
public class RadarCrPartitionRelationshipServiceImpl implements RadarCrPartitionRelationshipService {
    @Resource
    RadarCrPartitionRelationshipMapper radarCrPartitionRelationshipMapper;

    @Override
    public RadarTrainDataEntity getTrainingDataByJsonFile(String gridDataJsonFilePath, String charset, String lonFieldName, String latFieldName, String valueFieldName, String configFieldName) throws IOException {
        return InputDataProcessUtil.getTrainingDataByJsonFile(gridDataJsonFilePath, charset, lonFieldName, latFieldName, valueFieldName, configFieldName);
    }

    @Override
    public Boolean createPartitionTable(RadarCrPartitionRelationshipEntity radarCrPartitionRelationshipEntity) {
        radarCrPartitionRelationshipMapper.createPartitionTable(radarCrPartitionRelationshipEntity);
        return true;
    }
}

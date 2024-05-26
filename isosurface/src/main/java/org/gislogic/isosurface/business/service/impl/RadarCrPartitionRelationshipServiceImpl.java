package org.gislogic.isosurface.business.service.impl;

import cn.gislogic.business.domain.RadarCrPartitionRelationshipEntity;
import cn.gislogic.business.domain.RadarTrainDataEntity;
import cn.gislogic.business.mapper.RadarCrPartitionRelationshipMapper;
import cn.gislogic.business.service.RadarCrPartitionRelationshipService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
public class RadarCrPartitionRelationshipServiceImpl implements RadarCrPartitionRelationshipService {
    @Resource
    RadarCrPartitionRelationshipMapper radarCrPartitionRelationshipMapper;

    @Override
    public RadarTrainDataEntity getTrainingDataByJsonFile(String gridDataJsonFilePath, String charset, String lonFieldName, String latFieldName, String valueFieldName, String configFieldName) throws IOException {
        RadarTrainDataEntity trainData = getTrainingDataByJsonFile(gridDataJsonFilePath, charset, lonFieldName, latFieldName, valueFieldName, configFieldName);
        return trainData;
    }

    @Override
    public Boolean createPartitionTable(RadarCrPartitionRelationshipEntity radarCrPartitionRelationshipEntity) {
        radarCrPartitionRelationshipMapper.createPartitionTable(radarCrPartitionRelationshipEntity);
        return true;
    }
}

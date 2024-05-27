package org.gislogic.isosurface.radar.business.service.impl;

import org.gislogic.isosurface.radar.business.entity.RadarCrPartitionRelationshipEntity;
import org.gislogic.isosurface.radar.business.mapper.RadarCrPartitionRelationshipMapper;
import org.gislogic.isosurface.radar.business.pojo.GridData;
import org.gislogic.isosurface.radar.business.service.RadarCrPartitionRelationshipService;
import org.gislogic.isosurface.utils.InputDataProcessUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
public class RadarCrPartitionRelationshipServiceImpl implements RadarCrPartitionRelationshipService {
    @Resource
    RadarCrPartitionRelationshipMapper radarCrPartitionRelationshipMapper;

    @Override
    public GridData getTrainingDataByJsonFile(String gridDataJsonFilePath, String charset, String lonFieldName, String latFieldName, String valueFieldName, String configFieldName) throws IOException {
        return InputDataProcessUtil.getTrainingDataByJsonFile(gridDataJsonFilePath, charset, lonFieldName, latFieldName, valueFieldName, configFieldName);
    }

    @Override
    public Boolean createPartitionTable(RadarCrPartitionRelationshipEntity radarCrPartitionRelationshipEntity) {
        radarCrPartitionRelationshipMapper.createPartitionTable(radarCrPartitionRelationshipEntity);
        return true;
    }
}

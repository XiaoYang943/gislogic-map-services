package org.gislogic.isosurface.radar.business.service.impl;

import org.gislogic.isosurface.radar.business.entity.RadarDataTimeListEntity;
import org.gislogic.isosurface.radar.business.entity.RadarTimeListReq;
import org.gislogic.isosurface.radar.business.mapper.MapBoxVectorTileMapper;
import org.gislogic.isosurface.radar.business.service.MapBoxVectorTileService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class MapBoxVectorTileServiceImpl implements MapBoxVectorTileService {
    @Resource
    MapBoxVectorTileMapper mapper;

    @Override
    public byte[] vectorTitle(String tableName, String fileTime, String fileStation, String dataTime, Integer z, Integer x, Integer y) {
        return mapper.vectorTitle2(tableName, fileTime, fileStation, dataTime, z, x, y).getTile();
    }

    @Override
    public List<RadarDataTimeListEntity> selectDataTimeList(RadarTimeListReq radarTimeListReq) {
        List<RadarDataTimeListEntity> list = mapper.selectDataTimeList(radarTimeListReq);
        return list;
    }
}


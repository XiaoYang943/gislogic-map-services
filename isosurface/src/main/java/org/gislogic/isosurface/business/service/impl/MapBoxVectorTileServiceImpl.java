package org.gislogic.isosurface.business.service.impl;

import cn.gislogic.business.domain.RadarDataTimeListEntity;
import cn.gislogic.business.domain.RadarTimeListReq;
import cn.gislogic.business.mapper.MapBoxVectorTileMapper;
import cn.gislogic.business.service.MapBoxVectorTileService;
import cn.gislogic.isosurface.util.MapBoxVectorTileUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class MapBoxVectorTileServiceImpl implements MapBoxVectorTileService {
    @Resource
    MapBoxVectorTileMapper mapper;

    @Override
    public byte[] vectorTitle(String tableName, String fileTime, String fileStation, String dataTime, Integer z, Integer x, Integer y) {
        Integer dpi = MapBoxVectorTileUtil.zoom2dpi(z);
        /**
         * x:53
         * y:10
         * z:6
         * dpi:2
         */
        System.out.println("x:" + x);
        System.out.println("y:" + y);
        System.out.println("z:" + z);
        System.out.println("dpi:" + dpi);
//        return mapper.vectorTitle2(tableName,fileStation,dataTime,z, x, y, dpi).getTile();
//                return mapper.vectorTitle2(tableName, fileTime, StrUtil.toString(fileStation),dataTime,z, x, y, dpi).getTile();
        return mapper.vectorTitle2(tableName, fileTime, fileStation, dataTime, z, x, y, dpi).getTile();
    }

    @Override
    public List<RadarDataTimeListEntity> selectDataTimeList(RadarTimeListReq radarTimeListReq) {
        List<RadarDataTimeListEntity> list = mapper.selectDataTimeList(radarTimeListReq);
        return list;
    }
}


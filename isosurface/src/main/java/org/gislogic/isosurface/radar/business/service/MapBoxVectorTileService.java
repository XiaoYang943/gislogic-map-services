package org.gislogic.isosurface.radar.business.service;


import org.gislogic.isosurface.radar.business.entity.RadarDataTimeListEntity;
import org.gislogic.isosurface.radar.business.entity.RadarTimeListReq;

import java.util.List;

public interface MapBoxVectorTileService {
    byte[] vectorTitle(String tableName, String fileTime, String fileStation, String dataTime, Integer z, Integer x, Integer y);

    List<RadarDataTimeListEntity> selectDataTimeList(RadarTimeListReq radarTimeListReq);
}

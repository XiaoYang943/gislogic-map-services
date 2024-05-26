package org.gislogic.isosurface.business.service;


import org.gislogic.isosurface.business.domain.RadarDataTimeListEntity;
import org.gislogic.isosurface.business.domain.RadarTimeListReq;

import java.util.List;

public interface MapBoxVectorTileService {
    byte[] vectorTitle(String tableName, String fileTime, String fileStation, String dataTime, Integer z, Integer x, Integer y);

    List<RadarDataTimeListEntity> selectDataTimeList(RadarTimeListReq radarTimeListReq);
}

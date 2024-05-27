package org.gislogic.isosurface.radar.business.mapper;


import com.baomidou.mybatisplus.annotation.SqlParser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.gislogic.isosurface.radar.business.entity.RadarDataTimeListEntity;
import org.gislogic.isosurface.radar.business.entity.RadarTimeListReq;
import org.gislogic.isosurface.radar.business.vo.MapBoxVectorTileVO;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Mapper
public interface MapBoxVectorTileMapper {
    @SqlParser(filter = true)
    MapBoxVectorTileVO vectorTitle2(
            @Param("tableName") String tableName,
            @Param("fileTime") String fileTime,
            @Param("fileStation") String fileStation,
            @Param("dataTime") String dataTime,
            @Param("z") Integer z,
            @Param("x") Integer x,
            @Param("y") Integer y);

    List<RadarDataTimeListEntity> selectDataTimeList(@RequestBody RadarTimeListReq radarTimeListReq);
}


package org.gislogic.isosurface.business.mapper;


import com.baomidou.mybatisplus.annotation.SqlParser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.gislogic.isosurface.business.domain.MapBoxVectorTileEntity;
import org.gislogic.isosurface.business.domain.RadarDataTimeListEntity;
import org.gislogic.isosurface.business.domain.RadarTimeListReq;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Mapper
public interface MapBoxVectorTileMapper {
    @SqlParser(filter = true)
    MapBoxVectorTileEntity vectorTitle2(
            @Param("tableName") String tableName,
            @Param("fileTime") String fileTime,
            @Param("fileStation") String fileStation,
            @Param("dataTime") String dataTime,
            @Param("z") Integer z,
            @Param("x") Integer x,
            @Param("y") Integer y,
            @Param("dpi") Integer dpi);

    List<RadarDataTimeListEntity> selectDataTimeList(@RequestBody RadarTimeListReq radarTimeListReq);
}


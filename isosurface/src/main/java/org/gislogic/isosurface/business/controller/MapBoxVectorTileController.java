package org.gislogic.isosurface.business.controller;


import cn.hutool.core.lang.Console;
import org.gislogic.isosurface.business.domain.RadarDataTimeListEntity;
import org.gislogic.isosurface.business.domain.RadarTimeListReq;
import org.gislogic.isosurface.business.service.MapBoxVectorTileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;

@RestController
@RequestMapping("/radar")
@CrossOrigin
public class MapBoxVectorTileController {

    @Autowired
    MapBoxVectorTileService service;

    @GetMapping("/tile/{tableName}/{fileTime}/{fileStation}/{dataTime}/{z}/{x}/{y}")
    public void vectorTitle2(
            @PathVariable("tableName") String tableName,
            @PathVariable("fileTime") String fileTime,
            @PathVariable("fileStation") String fileStation,
            @PathVariable("dataTime") String dataTime,
            @PathVariable("z") Integer z,
            @PathVariable("x") Integer x,
            @PathVariable("y") Integer y,
            HttpServletResponse response) {
//@GetMapping("/new")
//public void vectorTitle2(@RequestParam("z")String  z, @RequestParam("x") String  x, @RequestParam("y") String  y, HttpServletResponse response){
//            x = x.replace(".pbf", "");
//            y = y.replace(".pbf", "");
//            z = z.replace(".pbf", "");

        response.setContentType("application/x-protobuf;type=mapbox-vector;chartset=UTF-8");
//        byte[] tile = service.vectorTitle(Integer.parseInt(z), Integer.parseInt(x),Integer.parseInt(y));
        byte[] tile = service.vectorTitle(tableName, fileTime, fileStation, dataTime, z, x, y);
        Console.log("-----------------------------------", tile);
        // 输出文件流
        OutputStream os = null;
        InputStream is = null;
        try {
            is = new ByteArrayInputStream(tile);
            os = response.getOutputStream();
            byte[] bytes = new byte[1024];
            int len;
            while ((len = is.read(bytes)) != -1) {
                os.write(bytes, 0, len);
            }
            os.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取data_time字段的列表
     */
    @GetMapping("/list/datatime")
    public List<RadarDataTimeListEntity> selectDataTimeList(@RequestBody RadarTimeListReq radarTimeListReq) {
        List<RadarDataTimeListEntity> list = service.selectDataTimeList(radarTimeListReq);
        return list;
    }
}


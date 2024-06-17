package org.gislogic.common.utils.geom;

import org.gislogic.common.utils.converter.Tile2Wgs84;

public class TileBuilder {
    /**
     * 创建瓦片Bbox
     *
     * @param zoom
     * @param tileX
     * @param tileY
     * @param extent
     * @param clipBuffer
     * @return
     */
    public static Bbox createTileBbox(byte zoom, int tileX, int tileY, int extent, int clipBuffer) {
        //瓦片左上角坐标
        double x0 = Tile2Wgs84.tileX2lon1(tileX, zoom, 0);
        double y0 = Tile2Wgs84.tileY2lat1(tileY, zoom, 0);
        //瓦片右下角坐标
        double x1 = Tile2Wgs84.tileX2lon1(tileX, zoom, extent);
        double y1 = Tile2Wgs84.tileY2lat1(tileY, zoom, extent);
        //clipBuffer后的坐标
        double dx = (x1 - x0) / extent; // 每像素多少经度
        double clipBufferX = dx * clipBuffer;
        x0 = x0 - clipBufferX;
        x1 = x1 + clipBufferX;

        double dy = (y0 - y1) / extent; // 每像素多少纬度
        double clipBufferY = dy * clipBuffer;
        y0 = y0 + clipBufferY;
        y1 = y1 - clipBufferY;
        return new Bbox(x0, y1, x1, y0);
    }
}

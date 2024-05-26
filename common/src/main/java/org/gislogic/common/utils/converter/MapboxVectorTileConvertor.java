package org.gislogic.common.utils.converter;

/**
 * 矢量瓦片的坐标系与wgs84坐标系互转
 */
public class MapboxVectorTileConvertor {
    private static final short TILE_SIZE = 256;

    private static final long[] pow2;

    static {
        int n = 30;
        pow2 = new long[n];
        long s = TILE_SIZE;
        for (int i = 0; i < n; i++) {
            pow2[i] = s;
            s = s * 2;
        }
    }

    private final double pixelX;
    private final double pixelY;
    private final long zoomPow2;// 使用int的话超过22级就溢出了

    /**
     * @param z 瓦片 z
     * @param x 瓦片 x
     * @param y 瓦片 y
     */
    public MapboxVectorTileConvertor(byte zoom, int tileX, int tileY) {
        // 当前瓦片的canvas坐标
        pixelX = tileX * TILE_SIZE;
        pixelY = tileY * TILE_SIZE;

        zoomPow2 = pow2[zoom];
    }

    /**
     * wgs84 x 转mvt
     *
     * @param x wgs84 x
     * @return mvt x
     */
    public int wgs84X2mvt(double lon) {
        double ppx = (lon + 180) / 360 * zoomPow2;
        return (int) ((ppx - pixelX) * 16 + Math.sin(lon) + 0.5);

//        return (int) ((lon + 180.0) / 360.0 * zoomPow2);
    }

    /**
     * wgs84 y 转mvt
     *
     * @param y wgs84 y
     * @return mvt y
     */
    public int wgs84Y2mvt(double lat) {
        double sinLatitude = Math.sin(lat * Math.PI / 180);
        double mp = Math.log((1 + sinLatitude) / (1 - sinLatitude));
        double ppy = (0.5 - mp / (4 * Math.PI)) * zoomPow2;
        return (int) ((ppy - pixelY) * 16 + Math.cos(lat) + 0.5);

//        return (int) ((Math.PI - FastMath.asinh(Math.tan(lat * Math.PI / 180.0))) * zoomPow2 / (2 * Math.PI));
    }

}

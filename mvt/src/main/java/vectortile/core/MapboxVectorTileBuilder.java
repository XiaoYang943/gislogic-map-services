package vectortile.core;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.*;
import utils.converter.MvtAndWGS84Convertor;
import utils.converter.Tile2Wgs84;
import utils.geom.Bbox;
import vector_tile.VectorTile;
import vectortile.pojo.MapboxVectorTileFeature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * mvt瓦片构造器
 *
 * @author liuyu
 * @date 2022/4/24
 */
public class MapboxVectorTileBuilder {
    protected final int extent;

    public final TileClip tileClip;

    private final MvtAndWGS84Convertor mvtCoordinateConvertor;

    private final Map<String, MapboxVectorTileLayer> layers = new LinkedHashMap<>();

    private final Bbox bbox;
    private static final GeometryFactory geometryFactory = new GeometryFactory();   // 几何工厂

    public MapboxVectorTileBuilder(byte zoom, int tileX, int tileY) {
        this(zoom, tileX, tileY, 4096, 8);
    }


    /**
     * Create with the given extent value.
     * <p>
     * The extent value control how detailed the coordinates are encoded in the
     * vector tile. 4096 is a good default, 256 can be used to reduce density.
     * <p>
     * The clip buffer value control how large the clipping area is outside of the
     * tile for geometries. 0 means that the clipping is done at the tile border. 8
     * is a good default.
     *
     * @param extent     瓦片范围(单位：像素、默认：4096)
     * @param clipBuffer 裁剪几何的缓冲区大小(单位：像素、默认：8)
     */
    public MapboxVectorTileBuilder(byte zoom, int tileX, int tileY, int extent, int clipBuffer) {
        this.extent = extent;
        bbox = createTileBbox(zoom, tileX, tileY, extent, clipBuffer);
        tileClip = new TileClip(bbox.xmin, bbox.ymin, bbox.xmax, bbox.ymax, geometryFactory);
        mvtCoordinateConvertor = new MvtAndWGS84Convertor(zoom, tileX, tileY);
    }

    /**
     * 新建一个图层
     */
    private MapboxVectorTileLayer createLayer(String layerName) {
        MapboxVectorTileLayer layer = new MapboxVectorTileLayer(this);
        layers.put(layerName, layer);
        return layer;
    }

    /**
     * 新建或获取一个图层
     */
    public MapboxVectorTileLayer getOrCreateLayer(String layerName) {
        MapboxVectorTileLayer layer = layers.get(layerName);
        if (layer != null) {
            return layer;
        }
        return createLayer(layerName);
    }

    private static Bbox createTileBbox(byte zoom, int tileX, int tileY, int extent, int clipBuffer) {
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


    public Bbox getBbox() {
        return bbox;
    }

    /**
     * 转为 bytes
     */
    public byte[] toBytes() {

        VectorTile.Tile.Builder tile = VectorTile.Tile.newBuilder();
        layers.forEach((layerName, layer) -> {

            VectorTile.Tile.Layer.Builder tileLayer = VectorTile.Tile.Layer.newBuilder();

            tileLayer.setVersion(2);
            tileLayer.setName(layerName);

            tileLayer.addAllKeys(layer.keys());

            for (Object value : layer.values()) {
                VectorTile.Tile.Value.Builder tileValue = VectorTile.Tile.Value.newBuilder();
                if (value instanceof String) {
                    tileValue.setStringValue((String) value);
                } else if (value instanceof Integer) {
                    tileValue.setSintValue((Integer) value);
                } else if (value instanceof Long) {
                    tileValue.setSintValue((Long) value);
                } else if (value instanceof Float) {
                    tileValue.setFloatValue((Float) value);
                } else if (value instanceof Double) {
                    tileValue.setDoubleValue((Double) value);
                } else if (value instanceof Boolean) {
                    tileValue.setBoolValue((Boolean) value);
                } else {
                    tileValue.setStringValue(value.toString());
                }
                tileLayer.addValues(tileValue.build());
            }

            tileLayer.setExtent(extent);
            for (MapboxVectorTileFeature mapboxVectorTileFeature : layer.mapboxVectorTileFeatureList) {

                Geometry geometry = mapboxVectorTileFeature.geometry;

                VectorTile.Tile.Feature.Builder featureBuilder = VectorTile.Tile.Feature.newBuilder();

                if (null != mapboxVectorTileFeature.tags) {
                    featureBuilder.addAllTags(mapboxVectorTileFeature.tags);
                }

                VectorTile.Tile.GeomType geomType = toGeomType(geometry);
                x = 0;
                y = 0;
                List<Integer> commands = commands(geometry);


                featureBuilder.setType(geomType);
                featureBuilder.addAllGeometry(commands);

                tileLayer.addFeatures(featureBuilder.build());
            }

            tile.addLayers(tileLayer.build());
        });


        return tile.build().toByteArray();
    }

    private static VectorTile.Tile.GeomType toGeomType(Geometry geometry) {
        if (geometry instanceof Point) {
            return VectorTile.Tile.GeomType.POINT;
        }
        if (geometry instanceof MultiPoint) {
            return VectorTile.Tile.GeomType.POINT;
        }
        if (geometry instanceof LineString) {
            return VectorTile.Tile.GeomType.LINESTRING;
        }
        if (geometry instanceof MultiLineString) {
            return VectorTile.Tile.GeomType.LINESTRING;
        }
        if (geometry instanceof Polygon) {
            return VectorTile.Tile.GeomType.POLYGON;
        }
        if (geometry instanceof MultiPolygon) {
            return VectorTile.Tile.GeomType.POLYGON;
        }
        return VectorTile.Tile.GeomType.UNKNOWN;
    }

    List<Integer> commands(Geometry geometry) {

        if (geometry instanceof MultiLineString) {
            return commands((MultiLineString) geometry);
        }
        if (geometry instanceof Polygon) {
            return commands((Polygon) geometry);
        }
        if (geometry instanceof MultiPolygon) {
            return commands((MultiPolygon) geometry);
        }

        return commands(geometry.getCoordinates(), shouldClosePath(geometry), geometry instanceof MultiPoint);
    }

    List<Integer> commands(MultiLineString mls) {
        List<Integer> commands = new ArrayList<>();
        for (int i = 0; i < mls.getNumGeometries(); i++) {
            commands.addAll(commands(mls.getGeometryN(i).getCoordinates(), false));
        }
        return commands;
    }

    List<Integer> commands(MultiPolygon mp) {
        List<Integer> commands = new ArrayList<>();
        for (int i = 0; i < mp.getNumGeometries(); i++) {
            Polygon polygon = (Polygon) mp.getGeometryN(i);
            commands.addAll(commands(polygon));
        }
        return commands;
    }

    List<Integer> commands(Polygon polygon) {

        // According to the vector tile specification, the exterior ring of a polygon
        // must be in clockwise order, while the interior ring in counter-clockwise order.
        // In the tile coordinate system, Y axis is positive down.
        //
        // However, in geographic coordinate system, Y axis is positive up.
        // Therefore, we must reverse the coordinates.
        // So, the code below will make sure that exterior ring is in counter-clockwise order
        // and interior ring in clockwise order.
        LineString exteriorRing = polygon.getExteriorRing();
        if (!Orientation.isCCW(exteriorRing.getCoordinates())) {
            exteriorRing = exteriorRing.reverse();
        }
        List<Integer> commands = new ArrayList<>(commands(exteriorRing.getCoordinates(), true));

        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            LineString interiorRing = polygon.getInteriorRingN(i);
            if (Orientation.isCCW(interiorRing.getCoordinates())) {
                interiorRing = interiorRing.reverse();
            }
            commands.addAll(commands(interiorRing.getCoordinates(), true));
        }
        return commands;
    }

    /**
     * // // // Ex.: MoveTo(3, 6), LineTo(8, 12), LineTo(20, 34), ClosePath //
     * Encoded as: [ 9 3 6 18 5 6 12 22 15 ] // == command type 7 (ClosePath),
     * length 1 // ===== relative LineTo(+12, +22) == LineTo(20, 34) // ===
     * relative LineTo(+5, +6) == LineTo(8, 12) // == [00010 010] = command type
     * 2 (LineTo), length 2 // === relative MoveTo(+3, +6) // == [00001 001] =
     * command type 1 (MoveTo), length 1 // Commands are encoded as uint32
     * varints, vertex parameters are // encoded as sint32 varints (zigzag).
     * Vertex parameters are // also encoded as deltas to the previous position.
     * The original // position is (0,0)
     *
     * @param cs cs
     * @return list
     */
    List<Integer> commands(Coordinate[] cs, boolean closePathAtEnd) {
        return commands(cs, closePathAtEnd, false);
    }

    List<Integer> commands(Coordinate[] cs, boolean closePathAtEnd, boolean multiPoint) {
        if (cs.length == 0) {
            throw new IllegalArgumentException("empty geometry");
        }

        List<Integer> r = new ArrayList<>();

        int lineToIndex = 0;
        int lineToLength = 0;


        for (int i = 0; i < cs.length; i++) {
            Coordinate c = cs[i];
            double cx = mvtCoordinateConvertor.wgs84X2mvt(c.x);
            double cy = mvtCoordinateConvertor.wgs84Y2mvt(c.y);
            if (i == 0) {
                r.add(commandAndLength(Command.MoveTo, multiPoint ? cs.length : 1));
            }

            int _x = (int) Math.round(cx);
            int _y = (int) Math.round(cy);

            // prevent point equal to the previous
            if (i > 0 && _x == x && _y == y) {
                lineToLength--;
                continue;
            }

            // prevent double closing
            if (closePathAtEnd && cs.length > 1 && i == (cs.length - 1) && cs[0].equals(c)) {
                lineToLength--;
                continue;
            }

            // delta, then zigzag
            r.add(zigZagEncode(_x - x));
            r.add(zigZagEncode(_y - y));

            x = _x;
            y = _y;

            if (i == 0 && cs.length > 1 && !multiPoint) {
                // can length be too long?
                lineToIndex = r.size();
                lineToLength = cs.length - 1;
                r.add(commandAndLength(Command.LineTo, lineToLength));
            }

        }

        // update LineTo length
        if (lineToIndex > 0) {
            if (lineToLength == 0) {
                // remove empty LineTo
                r.remove(lineToIndex);
            } else {
                // update LineTo with new length
                r.set(lineToIndex, commandAndLength(Command.LineTo, lineToLength));
            }
        }

        if (closePathAtEnd) {
            r.add(commandAndLength(Command.ClosePath, 1));
        }

        return r;
    }

    private int x = 0;
    private int y = 0;

    static int commandAndLength(int command, int repeat) {
        return repeat << 3 | command;
    }

    static int zigZagEncode(int n) {
        // https://developers.google.com/protocol-buffers/docs/encoding#types
        return (n << 1) ^ (n >> 31);
    }

    static boolean shouldClosePath(Geometry geometry) {
        return (geometry instanceof Polygon) || (geometry instanceof LinearRing);
    }
}

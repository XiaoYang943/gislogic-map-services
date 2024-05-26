package org.gislogic.isosurface.utils;

import java.util.HashMap;
import java.util.Map;

public class MapBoxVectorTileUtil {
    private static Map<Integer, Integer> map;

    static {
        map = new HashMap<>();
        map.put(1, 2);
        map.put(2, 2);
        map.put(3, 2);
        map.put(5, 2);
        map.put(6, 2);
        map.put(7, 512);
        map.put(8, 512);
        map.put(9, 1024);
        map.put(10, 1024);
        map.put(11, 1024);
        map.put(12, 1024);
        map.put(13, 2048);
        map.put(14, 2048);
        map.put(15, 4096);
        map.put(16, 4096);
        map.put(17, 4096);
        map.put(18, 4096);
        map.put(19, 4096);
        map.put(20, 4096);
    }

    public static Integer zoom2dpi(Integer z) {
        return map.containsKey(z) ? map.get(z) : 2048;
    }
}


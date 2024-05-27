package org.gislogic.isosurface.utils;


import org.gislogic.common.utils.geom.GeometryTopologicalRelationshipHelper;
import org.gislogic.isosurface.radar.enums.ConstantEnum;
import org.locationtech.jts.geom.Polygon;

import java.util.*;

/**
 * Feature 异常图层处理
 *
 * @author hxd
 */
public class FeatureExceptionHand {

    /**
     * 合并等值面（出现相交，包含等情况的等值面未合并，解决该异常问题）
     *
     * @param geometryMapList
     * @return
     */
    private static List<Map<String, Object>> unionPolygon(List<Map<String, Object>> geometryMapList) {
        // 等值面合并后的集合
        List<Map<String, Object>> unionList = new ArrayList<>();
        // 记录已合并的Polygon
        Set<Integer> isUnionSet = new HashSet<>();

        for (Map<String, Object> map : geometryMapList) {
            if (isUnionSet.contains(map.hashCode())) {
                continue;
            }
            Map<String, Object> unionMap = contactPolygon(map, isUnionSet, geometryMapList);
            isUnionSet.add(map.hashCode());
            unionList.add(unionMap);
        }
        return unionList;
    }

    /**
     * 寻找能合并的等值面
     *
     * @param map
     * @param isUnionSet
     * @param geometryMapList
     * @return
     */
    private static Map<String, Object> contactPolygon(Map<String, Object> map, Set<Integer> isUnionSet, List<Map<String, Object>> geometryMapList) {
        Double value = (double) map.get(ConstantEnum.VALUE);
        Polygon polygon = (Polygon) map.get(ConstantEnum.THE_GEOM);
        if (!polygon.isValid()) {
            // 无效不进行下一步
            return map;
        }
        for (Map<String, Object> unionMap : geometryMapList) {
            if (isUnionSet.contains(unionMap.hashCode())) {
                continue;
            }
            if (Objects.equals(map.hashCode(), unionMap.hashCode())) {
                // 同一个面不合并判断
                continue;
            }
            if (!value.equals(unionMap.get(ConstantEnum.VALUE))) {
                // 不等值
                continue;
            }
            Polygon unionPolygon = (Polygon) unionMap.get(ConstantEnum.THE_GEOM);
            if (!unionPolygon.isValid()) {
                // 无效不进行下一步
                continue;
            }
            if (!GeometryTopologicalRelationshipHelper.isIntersect(polygon, unionPolygon)) {
                continue;
            }
            // 判断是否被不等值面包含（不相交的等值面才需要判断是否被不等值面包含）
            if (!polygon.intersects(unionPolygon) && otherValueWithin(polygon, value, map.hashCode(), geometryMapList)) {
                continue;
            }
            try {
                polygon.union(unionPolygon);
                isUnionSet.add(unionMap.hashCode());
                Map<String, Object> item = new HashMap<>();
                item.put(ConstantEnum.VALUE, value);
                item.put(ConstantEnum.THE_GEOM, polygon);
                // 继续寻找能合并的等值面
                contactPolygon(item, isUnionSet, geometryMapList);
                return item;
            } catch (Exception e) {
                // 异常不处理，说明合并有问题，则不合并
                e.printStackTrace();
            }
        }
        isUnionSet.add(map.hashCode());
        return map;
    }

    /**
     * 该面是否出现不等值的面包含
     *
     * @param polygon
     * @param value
     * @param geometryMapList
     * @return
     */
    private static boolean otherValueWithin(Polygon polygon, Double value, int hashCode, List<Map<String, Object>> geometryMapList) {
        for (Map<String, Object> map : geometryMapList) {
            Polygon p = (Polygon) map.get(ConstantEnum.THE_GEOM);
            if (Objects.equals(hashCode, map.hashCode())) {
                continue;
            }
            try {
                if (!value.equals(map.get(ConstantEnum.VALUE)) && polygon.within(p)) {
                    return true;
                }
            } catch (Exception e) {
                // 不处理
                continue;
            }
        }
        return false;
    }


}

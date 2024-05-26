package org.gislogic.isosurface;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * 气象雷达颜色对应枚举
 */
public enum RadarColorEnum {
    /**
     * V_为前缀,后面是值
     */
    VF_9999(-9999.0, "#FFFFFF"),
    V_0(5.0, "#00ffff"),
    V_1(10.0, "#6698ff"),
    V_2(15.0, "#0000ff"),
    V_3(20.0, "#00ff00"),
    V_4(25.0, "#4cc417"),
    V_5(30.0, "#348017"),
    V_6(35.0, "#ffff00"),
    V_7(40.0, "#fdd017"),
    V_8(45.0, "#ff8040"),
    V_9(50.0, "#ff0000"),
    V_10(55.0, "#e41b17"),
    V_11(60.0, "#800000"),
    V_12(65.0, "#ff00ff"),
    V_13(70.0, "#800080");


    RadarColorEnum(double value, String color) {
        this.value = value;
        this.color = color;
    }

    /**
     * 值
     */
    private double value;

    /**
     * 雷达值对应颜色
     */
    private String color;

    public double getValue() {
        return value;
    }

    public String getColor() {
        return color;
    }

    /**
     * 值数组
     *
     * @return
     */
    public static double[] getValueArray() {
        int length = RadarColorEnum.values().length;
        double[] valueArray = new double[length];
        int i = 0;
        for (RadarColorEnum anEnum : RadarColorEnum.values()) {
            valueArray[i++] = anEnum.value;
        }
        return valueArray;
    }

    public static Map<Double, String> getValueColorMap() {
        Map<Double, String> map = new LinkedHashMap<>();
        for (RadarColorEnum anEnum : RadarColorEnum.values()) {
            map.put(anEnum.getValue(), anEnum.getColor());
        }
        return map;
    }

    public static Double getMinValue() {
        double[] valueArray = getValueArray();
        OptionalDouble minPositiveValue = Arrays.stream(valueArray)
                .filter(value -> value > 0) // 过滤正值
                .sorted() // 排序
                .findFirst(); // 获取最小的元素

        return minPositiveValue.isPresent() ? minPositiveValue.getAsDouble() : null;

    }

    public static Double getMaxValue() {
        double[] valueArray = getValueArray();
        Arrays.sort(valueArray);
        return valueArray[valueArray.length - 1];
    }

}

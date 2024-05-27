package org.gislogic.common.utils.feature;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimpleFeatureHelper {
    /**
     * @param clazz 实体类
     * @return SimpleFeatureBuilder pg数据库表结构构建器
     * @description 根据实体类创建 SimpleFeatureBuilder
     * @date 2024-03-09
     * @author hyy
     * 注意：该表的字段顺序严格遵循传入的实体类的字段顺序
     **/
    public static SimpleFeatureBuilder createSimpleFeatureBuilder(Class<?> clazz) {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("type");  // 必须设置，否则报错：java.lang.NullPointerException: Name is required for PropertyType
        Map<String, Class<?>> fieldsAndTypesOrdered = getFieldsAndTypesOrdered(clazz);
        fieldsAndTypesOrdered.forEach(typeBuilder::add);
        SimpleFeatureType type = typeBuilder.buildFeatureType();
        return new SimpleFeatureBuilder(type);
    }

    /**
     * @param clazz 实体类
     * @return Map<String, Class < ?>> 和实体类中字段顺序相同的Map
     * @description 获取实体类的 字段名-字段类型 的有序map映射
     * @date 2024-03-09
     * @author hyy
     **/
    public static Map<String, Class<?>> getFieldsAndTypesOrdered(Class<?> clazz) {
        List<String> fieldOrder = Arrays.asList(clazz.getDeclaredFields()).stream() // 通过反射获取实体类所有声明的字段并转换为List
                .map(Field::getName)    // 调用每个字段的 getName 方法来提取字段名，并映射到一个新的流中
                .collect(Collectors.toList());  // 将流中的字段名收集到一个列表中，并赋值给 fieldOrder
        Map<String, Class<?>> fieldsAndTypes = new LinkedHashMap<>();   // 不能用普通的 HashMap ，使用 LinkedHashMap 是为了保持插入顺序与实体类中保持一致
        for (String fieldName : fieldOrder) {
            try {
                Field field = clazz.getDeclaredField(fieldName);    // 字段
                Class<?> fieldType = field.getType();   // 字段类型
                fieldsAndTypes.put(fieldName, fieldType);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        return fieldsAndTypes;
    }
}

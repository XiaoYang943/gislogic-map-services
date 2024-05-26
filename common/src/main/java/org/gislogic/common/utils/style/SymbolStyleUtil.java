package org.gislogic.common.utils.style;

import java.awt.*;

/**
 * @description: 符号样式工具类
 * @author: hyy
 * @create: 2024-03-22
 **/
public class SymbolStyleUtil {
    /**
     * 颜色对象转16进制
     */
    public static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}

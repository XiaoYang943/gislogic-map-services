package org.gislogic.flatgeobuf;


import org.gislogic.flatgeobuf.generated.ColumnType;

public class ColumnMeta {
    public String name;
    public byte type;
    public String title;
    public String description;
    public int width;
    public int precision;
    public int scale;
    public boolean nullable;
    public boolean unique;
    public boolean primary_key;
    public String metadata;

    public Class<?> getBinding() {
        switch (type) {
            case ColumnType.Bool:
                return Boolean.class;
            case ColumnType.Byte:
                return Byte.class;
            case ColumnType.Short:
                return Short.class;
            case ColumnType.Int:
                return Integer.class;
            case ColumnType.Long:
                return Long.class;
            case ColumnType.Double:
                return Double.class;
            case ColumnType.DateTime:
                return String.class;
            case ColumnType.String:
                return String.class;
            default:
                throw new RuntimeException("Unknown type");
        }
    }
}
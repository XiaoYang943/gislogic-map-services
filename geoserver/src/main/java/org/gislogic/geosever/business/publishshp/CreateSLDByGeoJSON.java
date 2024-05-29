package org.gislogic.geosever.business.publishshp;


import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.style.Font;
import org.geotools.api.style.Stroke;
import org.geotools.api.style.*;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.xml.styling.SLDTransformer;
import org.gislogic.common.utils.style.SymbolStyleUtil;

import javax.xml.transform.TransformerException;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CreateSLDByGeoJSON {

    private static final StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
    private static final FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

    public static void recursiveTraversalFolders(String inputFolderPath, String outputFolderPath) {
        File file = new File(inputFolderPath);
        File[] files = file.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                String path = f.getAbsolutePath();
                recursiveTraversalFolders(path, outputFolderPath);
            } else {
                String extension = f.getName().substring(f.getName().lastIndexOf('.') + 1);
                if (extension.equals("geojson")) {
                    getStyle(f, outputFolderPath);
                }
            }
        }
    }

    private static void getStyle(File file, String outputFolderPath) {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);  // 读取 JSON 文件的所有行

            String jsonContent = lines.stream().collect(Collectors.joining());  // 将所有行连接成一个字符串
            JSONObject jsonObject = JSONUtil.parseObj(jsonContent);

            Style style = styleFactory.createStyle();
            StyledLayerDescriptor styledLayerDescriptor = styleFactory.createStyledLayerDescriptor();
            styledLayerDescriptor.setTitle("test");

            if (jsonObject.containsKey("features")) {
                JSONArray featuresArr = jsonObject.getJSONArray("features");
                for (int i = 0; i < featuresArr.size(); i++) {
                    JSONObject feature = featuresArr.getJSONObject(i);
                    // 一份geojson collection 中 有多种几何，但是多种几何下面 有多种原始类型。例如 Point 包含 text 和 marker point
                    String featureType = feature.getJSONObject("properties").getStr(CommonEnum.SOURCETYPE);
                    JSONObject properties = feature.getJSONObject("properties");
                    if (Objects.equals(featureType, CommonEnum.DEFAULT_TYPE_POLYLINE) || Objects.equals(featureType, CommonEnum.DEFAULT_TYPE_RUNWAY) || Objects.equals(featureType, CommonEnum.DEFAULT_TYPE_ARC3)) {
                        FeatureTypeStyle polylineStyle = createPolylineStyle(properties);
                        if (!style.featureTypeStyles().contains(polylineStyle)) {
                            style.featureTypeStyles().add(polylineStyle);
                        }
                    } else if (Objects.equals(featureType, CommonEnum.DEFAULT_TYPE_POLYGON) || Objects.equals(featureType, CommonEnum.DEFAULT_TYPE_ELLIPSE) || Objects.equals(featureType, CommonEnum.DEFAULT_TYPE_ARC)) {
                        FeatureTypeStyle polygonStyle = createPolygonStyle(properties, file);
                        if (!style.featureTypeStyles().contains(polygonStyle)) {
                            style.featureTypeStyles().add(polygonStyle);
                        }
                    } else if (Objects.equals(featureType, CommonEnum.DEFAULT_TYPE_MARKER)) {
                        FeatureTypeStyle markerStyle = createMarkerStyle(properties);
                        if (!style.featureTypeStyles().contains(markerStyle)) {
                            style.featureTypeStyles().add(markerStyle);
                        }
                    } else if (Objects.equals(featureType, CommonEnum.DEFAULT_TYPE_TEXT) || Objects.equals(featureType, CommonEnum.DEFAULT_TYPE_POINT)) {
                        FeatureTypeStyle textStyle = createTextStyle(properties);
                        if (!style.featureTypeStyles().contains(textStyle)) {
                            style.featureTypeStyles().add(textStyle);
                        }
                    }
                }

            }
            saveStyleToSLD(file, outputFolderPath, style);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static FeatureTypeStyle createPolylineStyle(JSONObject properties) {
        int pencolor = properties.getInt("pencolor");
        Color color = new Color(pencolor);
        String hex = SymbolStyleUtil.colorToHex(color);
        Stroke stroke = styleFactory.createStroke(
                filterFactory.literal(hex),
                filterFactory.literal(CommonEnum.DEFAULT_LINE_WIDTH)
        );

        String customId = properties.getStr(CommonEnum.CUSTOMID);
        Filter filter = filterFactory.equals(filterFactory.property(CommonEnum.CUSTOMID), filterFactory.literal(customId));

        Rule rule = styleFactory.createRule();
        LineSymbolizer sym = styleFactory.createLineSymbolizer(stroke, null);
        rule.setFilter(filter);
        rule.symbolizers().add(sym);

        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(rule);

        return fts;
    }

    private static FeatureTypeStyle createPolygonStyle(JSONObject properties, File file) {
        Fill fill = null;

        JSONArray willFilledFileName = new JSONArray();
        willFilledFileName.add("S2700");
        willFilledFileName.add("XIN6000");
        willFilledFileName.add("XIN6600");
        willFilledFileName.add("S6000X");
        willFilledFileName.add("S6600X");
        willFilledFileName.add("05L23RCHECK");
        willFilledFileName.add("05R23LCHECK");
        willFilledFileName.add("05L23RCON");
        willFilledFileName.add("05R23LCON");

        int pencolor = properties.getInt("pencolor");
        String filltype = properties.getStr("filltype");
        String hex = SymbolStyleUtil.colorToHex(new Color(pencolor));

        String brushColorString = SymbolStyleUtil.colorToHex(new Color(pencolor));
        String customId = properties.getStr(CommonEnum.CUSTOMID);
        Filter filter = filterFactory.equals(filterFactory.property(CommonEnum.CUSTOMID), filterFactory.literal(customId));


        Stroke stroke = styleFactory.createStroke(
                filterFactory.literal(hex),
                filterFactory.literal(CommonEnum.DEFAULT_LINE_WIDTH)
        );


        if (file.isFile()) {
            String fileName = file.getName();
            String nameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
            if (willFilledFileName.contains(nameWithoutExtension)) {
                if (filltype != "nobrush") {
                    fill = styleFactory.createFill(filterFactory.literal(brushColorString));
                }
            }
        }

        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
//        Fill fill = styleFactory.createFill(filterFactory.literal(Color.CYAN),filterFactory.literal(1.0f));

        Rule rule = styleFactory.createRule();
        PolygonSymbolizer polygonSymbolizer = styleFactory.createPolygonSymbolizer(stroke, fill, null);
        rule.symbolizers().add(polygonSymbolizer);
        rule.setFilter(filter);
        fts.rules().add(rule);
        return fts;
    }


    private static FeatureTypeStyle createMarkerStyle(JSONObject properties) {
        String markerType = properties.getStr("markerType");
        Filter filter = filterFactory.equals(filterFactory.property("markerType"), filterFactory.literal(markerType));
        int strNum = markerType.lastIndexOf('.');   // symbol3.xpm -> 7个字符
        String iconName = "";
        if (strNum != -1) {   // 特殊情况： symbol3xpm，而不是 symbol3.xpm
            Integer beginIndex = markerType.length() - 3;
            iconName = markerType.substring(0, beginIndex - 1) + ".png";
        } else {
            String extension = markerType.substring(markerType.lastIndexOf('.') + 1);
            if (extension == "xpm") {    // 符号库码表-对应格式
                iconName = markerType.substring(0, markerType.lastIndexOf('.')) + ".png";
            }
        }

        ExternalGraphic externalGraphic = styleFactory.createExternalGraphic(CommonEnum.DEFAULT_ICON_URL + iconName, "image/png");

        Graphic gr = styleFactory.createDefaultGraphic();
        gr.graphicalSymbols().clear();  // 先清除默认配置
        gr.graphicalSymbols().add(externalGraphic);
        gr.setSize(filterFactory.literal(CommonEnum.DEFAULT_MARKER_SIZE));

        Rule rule = styleFactory.createRule();
        PointSymbolizer sym = styleFactory.createPointSymbolizer(gr, null);
        rule.symbolizers().add(sym);
        rule.setFilter(filter);

        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(rule);

        return fts;
    }


    private static FeatureTypeStyle createTextStyle(JSONObject properties) {
        int pencolor = properties.getInt("pencolor");
        Fill fill = styleFactory.createFill(filterFactory.literal(pencolor));
        Rule rule = styleFactory.createRule();

        String customId = properties.getStr(CommonEnum.CUSTOMID);
        Filter filter = filterFactory.equals(filterFactory.property(CommonEnum.CUSTOMID), filterFactory.literal(customId));

        Font font = styleFactory.createFont(filterFactory.literal(CommonEnum.DEFAULT_FONT_FAMILY), filterFactory.literal("Regular"), filterFactory.literal("normal"), filterFactory.literal(CommonEnum.DEFAULT_FONT_SIZE));
        TextSymbolizer textSymbolizer = styleFactory.createTextSymbolizer();
        textSymbolizer.setLabel(filterFactory.property("string"));   // 标签的文本关联的字段名
        textSymbolizer.setFill(fill);
        textSymbolizer.setFont(font);
        rule.setFilter(filter);
        rule.symbolizers().add(textSymbolizer);
        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(rule);

        return fts;
    }


    private static void saveStyleToSLD(File file, String outputFolderPath, Style style) {
        SLDTransformer transformer = new SLDTransformer();
        try {
            String sld = transformer.transform(style);
            String nameWithoutExtension = file.getName().substring(0, file.getName().lastIndexOf('.'));
            FileWriter fileWriter = new FileWriter(outputFolderPath + "\\" + nameWithoutExtension + ".xml");
            fileWriter.write(sld);
            fileWriter.close();
            System.out.println("生成样式：" + file.getAbsolutePath() + " 成功！");

        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}

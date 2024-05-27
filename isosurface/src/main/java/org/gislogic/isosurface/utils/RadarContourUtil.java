package org.gislogic.isosurface.utils;


import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Stroke;
import org.geotools.styling.*;
import org.gislogic.isosurface.radar.business.entity.RadarEntity;
import org.gislogic.isosurface.radar.business.pojo.GridData;
import org.gislogic.isosurface.radar.enums.ConstantEnum;
import org.gislogic.isosurface.radar.enums.RadarColorEnum;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;


public class RadarContourUtil {


    public static void main(String[] args) throws SchemaException, IOException {
        // 读取雷达格网json
        GridData trainData = InputDataProcessUtil.getTrainingDataByJsonFile("C:\\Users\\heyiyang\\IdeaProjects\\gislogic-map-services\\isosurface\\src\\main\\resources\\data\\contour\\input\\wContourData_0.02.json", "UTF-8", "lon", "lat", "value", "config");

        // 数据分层级别(数据间隙)
        double[] dataInterval = RadarColorEnum.getValueArray();
        RadarEntity radarEntity = new RadarEntity(null, null, null, null, null);
        SimpleFeatureCollection featureCollection = CreateIsosurfaceUtil.equiSurface(trainData, dataInterval, radarEntity);

        try {
            // 输出图片
            float opacity = 1f;
            Map<Double, String> levelProps = RadarColorEnum.getValueColorMap();
            Layer layer = featureCollection2Layer(featureCollection, levelProps, opacity);
            ReferencedEnvelope bounds = featureCollection.getBounds();
            double[] bbox = {bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY()};
            int[] _xy = {1000, 800};
            String imgPath = "src/main/resources/data/contour/output/result_0.01.png";
            BufferedImage bufferedImage = layer2BufferedImage(layer, bbox, _xy);
            ImageIO.write(bufferedImage, "png", new File(imgPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String result = "src/main/resources/data/contour/output/result_0.01.json";
//        VectorDataUtil.writeFeatureCollection2DiskFile(result, featureCollection);
    }

    /**
     * Layer转png
     * 根据四至坐标、长、宽像素获取地图内容，并生成图片
     *
     * @param layer 图层
     * @param bbox  四至坐标
     * @param _xy   长、宽
     */
    private static BufferedImage layer2BufferedImage(Layer layer, double[] bbox, int[] _xy) {
        // 四至坐标
        double x1 = bbox[0], y1 = bbox[1], x2 = bbox[2], y2 = bbox[3];
        // 输出图片长、宽
        int width = _xy[0], height = _xy[1];
        // 设置输出范围
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        ReferencedEnvelope mapArea = new ReferencedEnvelope(x1, x2, y1, y2, crs);
        // 初始化渲染器
        StreamingRenderer sr = new StreamingRenderer();
        MapContent map = new MapContent();
        map.addLayer(layer);
        sr.setMapContent(map);
        // 初始化输出图像
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.getGraphics();
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Rectangle rect = new Rectangle(0, 0, width, height);
        // 绘制地图
        sr.paint((Graphics2D) g, rect, mapArea);
        map.dispose();
        return bi;
    }

    /**
     * 给featureCollection添加颜色和透明度
     *
     * @param featureCollection 等值面要素几何
     * @param levelProps        色阶,结构如：{0.1:"#a5f38d"}
     * @param opacity           透明度
     */
    private static Layer featureCollection2Layer(FeatureCollection featureCollection, Map<Double, String> levelProps, float opacity) {
        Layer layer = null;
        try {
            // 由坐标顺序引发坐标变换，这三行由于修正数据，不加的话会出现要素漏缺。
            SimpleFeatureType simpleFeatureType = (SimpleFeatureType) featureCollection.getSchema();
            String crs = CRS.lookupIdentifier(simpleFeatureType.getCoordinateReferenceSystem(), true);
            featureCollection = new ForceCoordinateSystemFeatureResults(featureCollection,
                    CRS.decode(crs, true));
            //创建样式
            StyleFactory sf = new StyleFactoryImpl();
            FilterFactory ff = new FilterFactoryImpl();
            FeatureTypeStyle fts = sf.createFeatureTypeStyle();
            for (Map.Entry entry : levelProps.entrySet()) {
                double key = (Double) entry.getKey();
                String value = (String) entry.getValue();
                Fill fill = sf.createFill(ff.literal(value), ff.literal(opacity));
                Stroke stroke = sf.createStroke(ff.literal("#ffffff"), ff.literal(0), ff.literal(0));
                Symbolizer symbolizer = sf.createPolygonSymbolizer(stroke, fill, ConstantEnum.THE_GEOM);
                Rule rule = sf.createRule();
                rule.setName("dzm_" + key);
                rule.symbolizers().add(symbolizer);
                Filter filter = ECQL.toFilter("value=" + key);
                rule.setFilter(filter);
                fts.rules().add(rule);
            }
            Style style = sf.createStyle();
            style.setName("style_dzm");
            style.featureTypeStyles().add(fts);

            layer = new FeatureLayer(featureCollection, style);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return layer;
    }


    /**
     * 网格数据抽稀点数
     * 进行网格IDW插值分析抽稀，不需要转换坐标
     *
     * @param trainData 返回对象 data数组为[y][x]
     * @param rarefy    抽稀倍数
     */
    private static void rarefy(GridData trainData, int rarefy) {
        if (rarefy <= 1) {
            return;
        }
        double[][] data = trainData.getData();
        double[] _X = trainData.get_X();
        double[] _Y = trainData.get_Y();

        int _XSize = trainData.getSize()[0];
        int _YSize = trainData.getSize()[1];
        // 余数
        boolean remainderX = _XSize % rarefy == 0;
        boolean remainderY = _YSize % rarefy == 0;

        // 抽稀最少倍数
        int aliquotX = _XSize / rarefy;
        int aliquotY = _YSize / rarefy;

        // 抽稀后的x和y的长度
        int rarefyX = remainderX ? aliquotX : aliquotX + 1;
        int rarefyY = remainderY ? aliquotY : aliquotY + 1;

        double[] lonArr = new double[rarefyX];
        double[] latArr = new double[rarefyY];
        for (int i = 0; i < _XSize; i++) {
            // 下标
            int index = i / rarefy;
            if (i % rarefy == 0) {
                lonArr[index] = _X[i];
            } else if (aliquotX == index && i == _XSize - 1) {
                // 最后一个不能被整除的点
                lonArr[rarefyX - 1] = _X[i];
            }
        }
        for (int i = 0; i < _YSize; i++) {
            int index = i / rarefy;
            if (i % rarefy == 0) {
                latArr[index] = _Y[i];
            } else if (aliquotY == index && i == _YSize - 1) {
                // 最后一个不能被整除的点
                latArr[rarefyY - 1] = _Y[i];
            }
        }
        trainData.setSize(new int[]{rarefyX, rarefyY});
        trainData.set_X(lonArr);
        trainData.set_Y(latArr);
        // 网格点及网格值
        double[][] matrix = new double[rarefyY][rarefyX];
        for (int i = 0; i < data.length; i++) {
            // 最后一个点不能丢弃
            if (i % rarefy != 0 && i != _YSize - 1) {
                continue;
            }
            for (int j = 0; j < data[0].length; j++) {
                // 最后一个点不能丢弃
                if (j % rarefy == 0) {
                    matrix[i / rarefy][j / rarefy] = data[i][j];
                } else if (j == _XSize - 1) {
                    matrix[rarefyY - 1][rarefyX - 1] = data[i][j];
                }
            }
        }
        trainData.setData(matrix);
    }


}

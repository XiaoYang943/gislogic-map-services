package org.gislogic.common.utils.converter;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @description: 数据转换工具类
 * @author: hyy
 **/

public class DataFormatConverter {
    /**
     * GeoJSON 转换为 SimpleFeatureCollection
     *
     * @param jsonFilePath GeoJSON文件路径
     * @return SimpleFeatureCollection
     */
    public static SimpleFeatureCollection convertGeoJSON2SimpleFeatureCollection(String jsonFilePath) {
        FeatureJSON featureJSON = new FeatureJSON();
        try {
            FileInputStream fileInputStream = new FileInputStream(jsonFilePath);
            try {
                return (SimpleFeatureCollection) featureJSON.readFeatureCollection(fileInputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param featureCollection 要素集合
     * @return String 字符串
     * @description 要素集合转字符串
     * @date 2024-03-06
     * @author hyy
     **/
    public static String convertFeatureCollection2String(SimpleFeatureCollection featureCollection) {
        try {
            FeatureJSON featureJSON = new FeatureJSON();
            StringWriter writer = new StringWriter();
            featureJSON.writeFeatureCollection(featureCollection, writer);
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param jsonFilePath GeoJSON 文件路径
     * @return FeatureCollection 要素集合
     * @description GeoJSON文件转FeatureCollection
     * @date 2024-02-12
     * @author hyy
     **/
    public static SimpleFeatureCollection convertGeoJSON2FeatureCollection(String jsonFilePath) {
        FeatureJSON featureJSON = new FeatureJSON();
        try {
            FileInputStream fileInputStream = new FileInputStream(jsonFilePath);
            try {
                return (SimpleFeatureCollection) featureJSON.readFeatureCollection(fileInputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 保存features为shp格式
     *
     * @param features SimpleFeature List
     * @param type     要素类型
     * @param shpPath  shp保存路径
     */
    public static void convertFeatureList2Shp(List<SimpleFeature> features, SimpleFeatureType type, String shpPath) {
        try {
            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
            File shpFile = new File(shpPath);
            Map<String, Serializable> params = new HashMap<>();
            params.put("url", shpFile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);

            ShapefileDataStore newDataStore =
                    (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.setCharset(StandardCharsets.UTF_8);

            newDataStore.createSchema(type);

            Transaction transaction = new DefaultTransaction("create");
            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
                SimpleFeatureCollection collection = new ListFeatureCollection(type, features);
                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    generateCpgFile(shpPath, StandardCharsets.UTF_8);
                    transaction.commit();   // 提交事务
                } catch (Exception problem) {
                    problem.printStackTrace();
                    transaction.rollback();
                } finally {
                    transaction.close();
                }
            } else {
                System.out.println(typeName + " does not support read/write access");
            }
        } catch (IOException e) {
        }
    }

    /**
     * 生成cpg文件
     *
     * @param filePath 文件完整路径
     * @param charset  文件编码
     */
    private static void generateCpgFile(String filePath, Charset charset) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }
            String tempPath = file.getPath();
            int index = tempPath.lastIndexOf('.');
            String name = tempPath.substring(0, index);
            String cpgFilePath = name + ".cpg";
            File cpgFile = new File(cpgFilePath);
            if (cpgFile.exists()) {
                return;
            }
            boolean newFile = cpgFile.createNewFile();
            if (newFile) {
                Files.write(cpgFile.toPath(), charset.toString().getBytes(charset));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * GeoJson转为Shp
     *
     * @param geojsonPath geojson 文件路径
     * @param shpPath     shp 文件路径
     * @param crs         坐标系
     */
    public static void convertGeoJSON2Shp(String geojsonPath, String shpPath, String crs) {
        try {
            InputStream in = new FileInputStream(geojsonPath);
            GeometryJSON gjson = new GeometryJSON();
            FeatureJSON fjson = new FeatureJSON(gjson);
            /**
             * 报错：java.lang.IllegalArgumentException: No such attribute:name
             * 场景：GeoJSON 的 FeatureCollection 中的 properties 中的所有属性，在转 shp 时必须相同，即属性个数和属性名必须保持一致，值可以为空
             */
            FeatureCollection<SimpleFeatureType, SimpleFeature> features = fjson.readFeatureCollection(in);
            SimpleFeatureType schema = features.getSchema();

            if (schema != null) {
                GeometryDescriptor geom = schema.getGeometryDescriptor();
                List<AttributeDescriptor> attributes = schema.getAttributeDescriptors();    // geojson文件属性
                GeometryType geomType = null;   // geojson文件空间类型（必须在第一个）
                List<AttributeDescriptor> attribs = new ArrayList<>();
                for (AttributeDescriptor attrib : attributes) {
                    AttributeType type = attrib.getType();
                    if (type instanceof GeometryType) {
                        geomType = (GeometryType) type;
                    } else {
                        attribs.add(attrib);
                    }
                }

                NameImpl theGeom = new NameImpl("the_geom");
                Class<?> binding = geomType.getBinding();
                CoordinateReferenceSystem crsResult = null;
                CoordinateReferenceSystem coordinateReferenceSystem = geom.getCoordinateReferenceSystem();
                if (coordinateReferenceSystem == null) {
                    crsResult = CRS.decode(crs);
                } else {
                    crsResult = coordinateReferenceSystem;
                }
                boolean identified = geomType.isIdentified();
                boolean anAbstract = geomType.isAbstract();
                AttributeType aSuper = geomType.getSuper();
                InternationalString description = geomType.getDescription();
                List<Filter> restrictions = geomType.getRestrictions();
                GeometryTypeImpl gt = new GeometryTypeImpl(theGeom, binding, crsResult, identified, anAbstract, restrictions, aSuper, description);

                int minOccurs = geom.getMinOccurs();
                int maxOccurs = geom.getMaxOccurs();
                boolean nillable = geom.isNillable();
                Object defaultValue = geom.getDefaultValue();
                GeometryDescriptor geomDesc = new GeometryDescriptorImpl(gt, theGeom, minOccurs, maxOccurs, nillable, defaultValue);

                attribs.add(0, geomDesc);   // the_geom 属性必须在第一个


                SimpleFeatureType outSchema = new SimpleFeatureTypeImpl(schema.getName(), attribs, geomDesc, schema.isAbstract(),
                        schema.getRestrictions(), schema.getSuper(), schema.getDescription());
                List<SimpleFeature> outFeatures = new ArrayList<>();
                try (FeatureIterator<SimpleFeature> features2 = features.features()) {
                    while (features2.hasNext()) {
                        SimpleFeature f = features2.next();
                        SimpleFeature reType = DataUtilities.reType(outSchema, f, true);
                        reType.setAttribute(outSchema.getGeometryDescriptor().getName(),
                                f.getAttribute(schema.getGeometryDescriptor().getName()));

                        outFeatures.add(reType);
                    }
                }
                convertFeatureList2Shp(outFeatures, outSchema, shpPath);
            }
        } catch (FactoryException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 递归遍历文件夹，批量转换geojson为shp
     *
     * @param inputFolderPath  geojson文件夹
     * @param outputFolderPath shp文件夹
     * @param crs              坐标系
     */
    public static void convertGeoJSON2ShpByBatch(String inputFolderPath, String outputFolderPath, String crs) {
        File file = new File(inputFolderPath);
        File[] files = file.listFiles();
        if (files == null) {
            return;
        }
        if (!Files.exists(Path.of(outputFolderPath))) {
            try {
                Files.createDirectories(Path.of(outputFolderPath));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        for (File f : files) {
            if (f.isDirectory()) {
                String path = f.getAbsolutePath();
                convertGeoJSON2ShpByBatch(path, outputFolderPath, crs);
            } else {
                String fName = f.getName();
                if (Objects.equals(fName.split("\\.")[1], "json")) {
                    String jsonPath = f.getAbsolutePath();
                    String shpPath = outputFolderPath + '\\' + fName.split("\\.")[0] + ".shp";
                    System.out.println("正在处理" + jsonPath + "...");
                    convertGeoJSON2Shp(jsonPath, shpPath, crs);
                }
            }
        }
    }

    /**
     * 递归遍历文件夹，xml转json
     *
     * @param inputFolderPath  xml文件夹路径
     * @param outputFolderPath json文件夹路径
     * @param XMLFileSuffix    xml文件后缀(例如：xml、map、kml...)
     */
    public static void recursiveTraversalFolders(String inputFolderPath, String outputFolderPath, String XMLFileSuffix) {
        File file = new File(inputFolderPath);
        File[] files = file.listFiles();
        if (files == null) {
            return;
        }
        if (!Files.exists(Path.of(outputFolderPath))) {
            try {
                Files.createDirectories(Path.of(outputFolderPath));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        for (File f : files) {
            if (f.isDirectory()) {
                String path = f.getAbsolutePath();
                recursiveTraversalFolders(path, outputFolderPath, XMLFileSuffix);
            } else {
                String extension = f.getName().substring(f.getName().lastIndexOf('.') + 1);
                if (extension.equals(XMLFileSuffix)) {
                    String xmlString = readXMLFileToString(f.getAbsolutePath());
                    JSONObject jsonObject = JSONUtil.parseFromXml(xmlString);
                    String nameWithoutExtension = f.getName().substring(0, f.getName().lastIndexOf('.'));
                    try {
                        FileWriter fileWriter = new FileWriter(outputFolderPath + "\\" + nameWithoutExtension + ".json");
                        fileWriter.write(jsonObject.toString());
                        fileWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * XML转字符串
     *
     * @param filePath xml文件路径
     * @return xml字符串
     */
    private static String readXMLFileToString(String filePath) {
        StringBuilder xmlContent = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;

            while ((line = reader.readLine()) != null) {
                xmlContent.append(line);
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return xmlContent.toString();
    }

    /**
     * 压缩指定文件夹内的所有shp数据，每份数据压缩为一个zip，并保存到指定的文件夹
     *
     * @param inputDirectoryPath  输入shp数据文件夹
     * @param outPutDirectoryPath 输出shp-zip数据文件夹
     */
    public static void zipAllShapefilesInDirectory(String inputDirectoryPath, String outPutDirectoryPath) {
        File directory = new File(inputDirectoryPath);
        File[] files = directory.listFiles();
        if (!Files.exists(Path.of(outPutDirectoryPath))) {
            try {
                Files.createDirectories(Path.of(outPutDirectoryPath));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".shp")) {
                    String shapefilePrefix = file.getName().substring(0, file.getName().lastIndexOf('.'));
                    String zipFileName = shapefilePrefix + ".zip";
                    try {
                        FileOutputStream fos = new FileOutputStream(outPutDirectoryPath + File.separator + zipFileName);
                        ZipOutputStream zipOut = new ZipOutputStream(fos);
                        File[] relatedFiles = directory.listFiles((dir, name) -> name.startsWith(shapefilePrefix));
                        if (relatedFiles != null) {
                            for (File relatedFile : relatedFiles) {
                                if (!relatedFile.getName().endsWith(".zip")) {
                                    addToZip(relatedFile, relatedFile.getName(), zipOut);
                                }
                            }
                        }
                        zipOut.close();
                        fos.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * 压缩文件
     *
     * @param file     文件
     * @param fileName 文件名
     * @param zipOut   输出流
     */
    private static void addToZip(File file, String fileName, ZipOutputStream zipOut) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ZipEntry zipEntry = new ZipEntry(fileName);
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fis.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 保存features为shp格式
     *
     * @param features 要素类
     * @param TYPE     要素类型
     * @param shpPath  shp保存路径
     * @return 是否保存成功
     */
    public static boolean saveFeaturesToShp(List<SimpleFeature> features, SimpleFeatureType TYPE, String shpPath) {
        try {
            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
            File shpFile = new File(shpPath);
            Map<String, Serializable> params = new HashMap<>();
            params.put("url", shpFile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);

            ShapefileDataStore newDataStore =
                    (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.setCharset(StandardCharsets.UTF_8);

            newDataStore.createSchema(TYPE);

            Transaction transaction = new DefaultTransaction("create");
            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
                SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    generateCpgFile(shpPath, StandardCharsets.UTF_8);
                    transaction.commit();   // 提交事务
                } catch (Exception problem) {
                    problem.printStackTrace();
                    transaction.rollback();
                } finally {
                    transaction.close();
                }
            } else {
                System.out.println(typeName + " does not support read/write access");
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean transformGeoJsonToShp(String geojsonPath, String shapefilePath) {
        try (InputStream in = new FileInputStream(geojsonPath)) {
            // open geojson
            GeometryJSON gjson = new GeometryJSON();
            FeatureJSON fjson = new FeatureJSON(gjson);
            FeatureCollection<SimpleFeatureType, SimpleFeature> features = fjson.readFeatureCollection(in);
            // convert schema for shapefile
            SimpleFeatureType schema = features.getSchema();
            GeometryDescriptor geom = schema.getGeometryDescriptor();
            // geojson文件属性
            List<AttributeDescriptor> attributes = schema.getAttributeDescriptors();
            // geojson文件空间类型（必须在第一个）
            GeometryType geomType = null;
            List<AttributeDescriptor> attribs = new ArrayList<>();
            for (AttributeDescriptor attrib : attributes) {
                AttributeType type = attrib.getType();
                if (type instanceof GeometryType) {
                    geomType = (GeometryType) type;
                } else {
                    attribs.add(attrib);
                }
            }
            if (geomType == null)
                return false;

            // 使用geomType创建gt
            GeometryTypeImpl gt = new GeometryTypeImpl(new NameImpl("the_geom"), geomType.getBinding(),
                    geom.getCoordinateReferenceSystem() == null ? DefaultGeographicCRS.WGS84 : geom.getCoordinateReferenceSystem(), // 用户未指定则默认为wgs84
                    geomType.isIdentified(), geomType.isAbstract(), geomType.getRestrictions(),
                    geomType.getSuper(), geomType.getDescription());

            // 创建识别符
            GeometryDescriptor geomDesc = new GeometryDescriptorImpl(gt, new NameImpl("the_geom"), geom.getMinOccurs(),
                    geom.getMaxOccurs(), geom.isNillable(), geom.getDefaultValue());

            // the_geom 属性必须在第一个
            attribs.add(0, geomDesc);

            SimpleFeatureType outSchema = new SimpleFeatureTypeImpl(schema.getName(), attribs, geomDesc, schema.isAbstract(),
                    schema.getRestrictions(), schema.getSuper(), schema.getDescription());
            List<SimpleFeature> outFeatures = new ArrayList<>();
            try (FeatureIterator<SimpleFeature> features2 = features.features()) {
                while (features2.hasNext()) {
                    SimpleFeature f = features2.next();
                    SimpleFeature reType = DataUtilities.reType(outSchema, f, true);

                    reType.setAttribute(outSchema.getGeometryDescriptor().getName(),
                            f.getAttribute(schema.getGeometryDescriptor().getName()));

                    outFeatures.add(reType);
                }
            }
            return saveFeaturesToShp(outFeatures, outSchema, shapefilePath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void transformGeoJsonToShpByRecursion(String inputFolderPath, String outputFolderPath) {
        File file = new File(inputFolderPath);
        File[] files = file.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                String path = f.getAbsolutePath();
//                System.out.println("dir:" + path);
                transformGeoJsonToShpByRecursion(path, outputFolderPath);
            } else {
                String fName = f.getName();
                if (Objects.equals(fName.split("\\.")[1], "geojson")) {
                    String jsonPath = f.getAbsolutePath();
                    String shpPath = outputFolderPath + '\\' + fName.split("\\.")[0] + ".shp";
                    System.out.println("正在处理" + jsonPath + "...");
                    transformGeoJsonToShp(jsonPath, shpPath);
                }
            }
        }
    }

}

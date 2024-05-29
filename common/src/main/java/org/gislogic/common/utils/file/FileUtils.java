package org.gislogic.common.utils.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileUtils {
    /**
     * 新建文件夹
     *
     * @param pathStrList 文件夹路径列表
     */
    public static void mkdirIfNotExist(List<String> pathStrList) {
        pathStrList.forEach((pathStr) -> {
            Path path = Paths.get(pathStr);
            try {
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}

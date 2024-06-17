package org.gislogic.mvt.vectortile.util;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

public class HttpUtil {
    /**
     * 将 bytes 写入 HttpServletResponse
     *
     * @param bytes       编码后的mvt
     * @param contentType 响应的内容类型
     * @param response    HTTP 响应
     */
    public static void exportByte(byte[] bytes, String contentType, HttpServletResponse response) {
        response.setContentType(contentType);
        try (OutputStream os = response.getOutputStream()) {
            os.write(bytes);
            os.flush();
        } catch (org.apache.catalina.connector.ClientAbortException e) {
            //地图移动时客户端主动取消， 产生异常"你的主机中的软件中止了一个已建立的连接"，无需处理
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

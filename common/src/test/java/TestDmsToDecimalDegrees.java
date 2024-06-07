import cn.hutool.core.lang.Console;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import org.gislogic.common.utils.converter.CoordinateConverter;

public class TestDmsToDecimalDegrees {
    public static void main(String[] args) {

        String data = "N39°05′20″,E107°58′16″\n" +
                "N34°53′30″,E106°51′00″\n" +
                "N32°27′00″,E105°49′00″\n" +
                "N31°54′00″,E109°31′00″\n" +
                "N32°59′40″,E110°25′20″\n" +
                "N33°32′27″,E110°50′56″\n" +
                "N35°29′02″,E112°25′16″\n" +
                "N39°02′15″,E111°04′15″\n" +
                "N39°05′20″,E107°58′16″";

        String[] lines = data.split("\\r?\\n"); // 正则匹配每一行

        JSONObject geometry = new JSONObject();
        geometry.set("type", "Polygon");
        JSONArray coordinates = new JSONArray();
        geometry.set("coordinates", coordinates);

        JSONArray feature = new JSONArray();
        // 输出分割后的行
        for (String line : lines) {
            JSONArray coordinate = new JSONArray();
            coordinate.add(CoordinateConverter.dmsToDecimalDegrees(CoordinateConverter.convertDMStoString(line.split(",")[1])));
            coordinate.add(CoordinateConverter.dmsToDecimalDegrees(CoordinateConverter.convertDMStoString(line.split(",")[0])));
            feature.add(coordinate);
        }
        coordinates.add(feature);

        JSONObject polygon = new JSONObject();
        polygon.set("type", "Feature");
        polygon.set("geometry", geometry);
        Console.log(polygon);
    }
}

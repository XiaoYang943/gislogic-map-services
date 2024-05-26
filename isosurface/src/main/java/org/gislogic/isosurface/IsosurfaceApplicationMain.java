package org.gislogic.isosurface;

import org.gislogic.isosurface.configuration.GeoToolsPostgisConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GeoToolsPostgisConfiguration.class})
public class IsosurfaceApplicationMain {
    public static void main(String[] args) {
        SpringApplication.run(IsosurfaceApplicationMain.class, args);
    }
}

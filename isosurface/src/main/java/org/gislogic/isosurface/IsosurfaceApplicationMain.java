package org.gislogic.isosurface;

import org.gislogic.isosurface.radar.configuration.RadarDataPostgisConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({RadarDataPostgisConfig.class})
public class IsosurfaceApplicationMain {
    public static void main(String[] args) {
        SpringApplication.run(IsosurfaceApplicationMain.class, args);
    }
}

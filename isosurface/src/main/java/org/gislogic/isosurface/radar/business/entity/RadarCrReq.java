package org.gislogic.isosurface.radar.business.entity;

import lombok.Data;

@Data
public class RadarCrReq {
    public String filePath;
    public String fileTime;
    public String dataTime;
    public String fileStation;
}

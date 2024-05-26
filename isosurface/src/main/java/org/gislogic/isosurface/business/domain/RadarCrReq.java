package org.gislogic.isosurface.business.domain;

import lombok.Data;

@Data
public class RadarCrReq {
    public String filePath;
    public String fileTime;
    public String dataTime;
    public String fileStation;
}

package org.gislogic.geosever.business.service;

public interface BatchPublishAirnetDataService {
    void publishAirnetXml(String mapXMLFolder, String outputFolder, Boolean delete);
}

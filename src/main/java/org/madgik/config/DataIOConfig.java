package org.madgik.config;

import java.util.Properties;

/**
 * Data source I/O configuration shortcut class
 */
public class DataIOConfig {
    String type;

    public String getType() {
        return type;
    }

    public String getParams() {
        return params;
    }

    String params;

    public DataIOConfig(String name) {
        this.name = name;
    }

    public void readParams(Properties prop){
       this.type = prop.getProperty(name + "_type");
        this.params = prop.getProperty(name + "_params");
    }

    String name;
}

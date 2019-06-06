package org.madgik.io;

import org.apache.log4j.Logger;
import org.madgik.MVTopicModel.SciTopicFlow;
import org.madgik.config.Config;


/**
 * Data source factory object.
 */
public class TMDataSourceFactory {
    public static TMDataSource instantiate(String type, String params){
        Logger logger = Logger.getLogger(SciTopicFlow.LOGGER);
        try {
            if (type.equals(SQLTMDataSource.name)) {
                return new SQLTMDataSource(params);
            }
            else if (type.equals(SerializedFileTMDataSource.name)) {
                return new SerializedFileTMDataSource(params);
            }
            else if (type.equals(JsonTMDataSource.name)) {
                    return new JsonTMDataSource(params);
            } else {
                logger.error("Undefined data source type " + type);
                return null;
            }
        }
        catch (NullPointerException ex){
            logger.error("Error getting source type information: " + type);
            return null;
        }
    }
}

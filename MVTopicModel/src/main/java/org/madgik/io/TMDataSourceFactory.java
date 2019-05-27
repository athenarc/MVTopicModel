package org.madgik.io;

import org.apache.log4j.Logger;
import org.madgik.MVTopicModel.SciTopicFlow;
import org.madgik.config.Config;


/**
 * Data source factory object.
 */
public class TMDataSourceFactory {
    public static TMDataSource instantiate(Config conf){
        Logger logger = Logger.getLogger(SciTopicFlow.LOGGER);
        try {
            if (conf.getDataSourceType().equals(SQLTMDataSource.name)) {
                return new SQLTMDataSource(conf.getDataSourceParams());
            }
            else if (conf.getDataSourceType().equals(SerializedFileTMDataSource.name)) {
                    return new SerializedFileTMDataSource(conf.getDataSourceParams());
            } else {
                logger.error("Undefined data source type " + conf.getDataSourceType());
                return null;
            }
        }
        catch (NullPointerException ex){
            logger.error("Error getting source type information: " + conf.getDataSourceType());
            return null;
        }
    }
}

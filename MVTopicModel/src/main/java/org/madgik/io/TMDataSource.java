package org.madgik.io;

import cc.mallet.types.Instance;
import org.apache.log4j.Logger;
import org.madgik.MVTopicModel.SciTopicFlow;
import org.madgik.config.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for all data sources.
 */
public abstract class TMDataSource {
    Logger logger = Logger.getLogger(SciTopicFlow.LOGGER);

    public static String getName() {
        return name;
    }

    protected static String name;

    public TMDataSource(String properties) {
        initialize(properties);
    }
    abstract void initialize(String properties);

    public abstract ArrayList<ArrayList<Instance>> getInputs(Config config);
    public abstract Map<Integer, String> getTopics(String experimentId);
    public abstract void prepareTopicKeyphraseWrite(Map<Integer, Map<String, List<Integer>>> topicTitles, String experimentId, String keyphraseTagger);
    public abstract void prepareOutput(String experimentId);



}

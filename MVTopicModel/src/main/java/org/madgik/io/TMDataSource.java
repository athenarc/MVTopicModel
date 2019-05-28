package org.madgik.io;

import cc.mallet.types.Instance;
import edu.stanford.nlp.util.Quadruple;
import edu.stanford.nlp.util.Triple;
import org.apache.log4j.Logger;
import org.madgik.MVTopicModel.FastQMVWVTopicModelDiagnostics;
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


    public abstract void saveResults(ArrayList<Quadruple<Integer, Byte, String, Double>> topicData,
                            ArrayList<Triple<Integer, String, Integer>> phraseData,
                            ArrayList<Quadruple<Integer, Byte, Double, Integer>> topicDetails, String batchId, String experimentId,
                            String experimentDescription, String experimentMetadata);

    public abstract void saveDiagnostics(int numModalities, String batchId, String experimentId, double[][] perplexities, int numTopics, ArrayList<FastQMVWVTopicModelDiagnostics.TopicScores> diagnostics);

    // post-analysis
    public abstract void prepareTopicDistroTrendsOutput(String experimentId);




}

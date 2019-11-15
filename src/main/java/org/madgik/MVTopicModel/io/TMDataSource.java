package org.madgik.MVTopicModel.io;

import cc.mallet.types.Instance;
import edu.stanford.nlp.util.Quadruple;
import edu.stanford.nlp.util.Triple;
import org.apache.log4j.Logger;
import org.madgik.MVTopicModel.FastQMVWVTopicInferencer;
import org.madgik.MVTopicModel.FastQMVWVTopicModelDiagnostics;
import org.madgik.MVTopicModel.SciTopicFlow;
import org.madgik.MVTopicModel.config.Config;
import org.madgik.MVTopicModel.model.*;

import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for all data sources.
 */
public abstract class TMDataSource {
    Logger logger = Logger.getLogger(SciTopicFlow.LOGGERNAME);

    public static String getName() {
        return name;
    }

    protected static String name;

    public TMDataSource(String properties) {
        initialize(properties);
    }
    abstract void initialize(String properties);

    public abstract ArrayList<ArrayList<Instance>> getModellingInputs(Config config);
    public abstract ArrayList<ArrayList<Instance>> getInferenceInputs(Config config);
    public abstract Map<Integer, String> getTopics(String experimentId);
    public abstract void prepareTopicKeyphraseWrite(Map<Integer, Map<String, List<Integer>>> topicTitles, String experimentId, String keyphraseTagger);
    public abstract void prepareOutput(String experimentId);


    public abstract void saveResults(List<TopicData> topicData, List<DocumentTopicAssignment> docTopics, String batchId, String experimentId, String experimentDescription, String experimentMetadata);

    public abstract void saveDiagnostics(int numModalities, String batchId, String experimentId, double[][] perplexities,
                                         int numTopics, List<FastQMVWVTopicModelDiagnostics.TopicScores> diagnostics);

    // post-analysis
    public abstract void prepareTopicDistroTrendsOutput(String experimentId);

    public abstract void saveDocumentTopicAssignments(Config config, Map<String, Map<Integer, Double>> docTopicMap, String runType);
    public abstract FastQMVWVTopicInferencer getInferenceModel(Config config);
    public abstract void deleteExistingExperiment(Config config);

    public abstract void saveTopicsAndExperiment(Config config, List<TopicAnalysis> topicAnalysisList, List<TopicDetails> topicDetailsList, byte[] serializedModel, String experimentMetadata);

    public abstract void saveDiagnostics(Config config, List<Score> scores);

}

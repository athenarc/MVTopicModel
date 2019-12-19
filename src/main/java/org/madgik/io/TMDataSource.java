package org.madgik.io;

import cc.mallet.types.Instance;
import org.apache.log4j.Logger;
import org.madgik.MVTopicModel.FastQMVWVTopicInferencer;
import org.madgik.MVTopicModel.FastQMVWVTopicModelDiagnostics;
import org.madgik.MVTopicModel.MalletAdapter;
import org.madgik.MVTopicModel.SciTopicFlow;
import org.madgik.config.Config;
import org.madgik.MVTopicModel.model.*;
import org.madgik.dbpediaspotlightclient.DBpediaAnnotator;
import org.madgik.dbpediaspotlightclient.DBpediaResource;
import org.madgik.io.modality.Modality;
import org.madgik.io.modality.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for all data sources.
 */
public abstract class TMDataSource {
    final protected Logger LOGGER = Logger.getLogger(SciTopicFlow.LOGGERNAME);
    protected String parameters;

    public static String getName() {
        return name;
    }

    protected static String name;
    protected Map<String, List<Modality>> inputs;
    ArrayList<ArrayList<Instance>> inputInstances;

    public boolean isNeedsSerializedModel() {
        return needsSerializedModel;
    }

    protected boolean needsSerializedModel = true;

    public TMDataSource(String properties) {
        inputs = new HashMap<>();
        initialize(properties);
    }
    abstract void initialize(String properties);

    public abstract void getModellingInputs(Config config);
    public abstract void getInferenceInputs(Config config);

    public ArrayList<ArrayList<Instance>> getInputInstances(){
        return inputInstances;
    }

    public void processInputs(Config config){
        MalletAdapter ma = new MalletAdapter(config);
        inputInstances = ma.makeInstances(inputs);
    }

    public List<Modality> getRawInput(String mod) {
        return inputs.get(mod);
    }

    public void clearRawInputData(String mod){
        inputs.get(mod).clear();
    }

    public void setRawInput(List<Modality> data, String modality){
        inputs.put(modality, data);
    }


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

    public abstract void saveTopicsAndExperiment(Config config, List<TopicAnalysis> topicAnalysisList, List<TopicDetails> topicDetailsList, Object serializedModel, String experimentMetadata);

    public abstract void saveDiagnostics(Config config, List<Score> scores);


    public abstract void saveSemanticAugmentationSingleOutput(List<DBpediaResource> entities, String pubId, DBpediaAnnotator.AnnotatorType annotator);
    public abstract void saveSemanticOutputResourceDetails(DBpediaResource resource);

    // inputs for semantic augmentation
    public abstract void loadSemanticAugmentationInputs(int queueSize);
    public abstract Text getNextSemanticAugmentationInput(int queueSize);

    // inputs for semantic resource details extraction
    public abstract void loadSemanticDetailExtractionInputs(int queueSize);
    public abstract String getNextSemanticDetailExtractionInput(int queueSize);

}

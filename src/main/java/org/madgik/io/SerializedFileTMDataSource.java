package org.madgik.io;

import cc.mallet.types.Instance;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Quadruple;
import org.madgik.MVTopicModel.FastQMVWVTopicInferencer;
import org.madgik.MVTopicModel.FastQMVWVTopicModelDiagnostics;
import org.madgik.MVTopicModel.SciTopicFlow;
import org.madgik.config.Config;
import org.madgik.MVTopicModel.model.*;
import org.madgik.dbpediaspotlightclient.DBpediaAnnotator;
import org.madgik.dbpediaspotlightclient.DBpediaResource;
import org.madgik.io.modality.Modality;
import org.madgik.io.modality.Text;
import org.madgik.utils.Utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

public class SerializedFileTMDataSource extends FileTMDataSource {

    public static final Logger LOGGER=Logger.getLogger(SciTopicFlow.LOGGERNAME);
    public static final String name = "serialized";
    public SerializedFileTMDataSource(String properties) {
        super(properties);
        needsSerializedModel = false;
    }

    @Override
    public String getPath(String path) {
        return path + ".ser";
    }

    /**
     * Read java serialized file
     * @param o
     */
    public void writeObject(Object o, String outpath){
        LOGGER.info("Serializing to " + outpath);
        // serialize for testing
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(outpath));
            out.writeObject(o);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object readObject(String inpath){
        LOGGER.info("Reading serialized object from path " + inpath);
        // serialize for testing
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(inpath));
            return in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    public void getModellingInputs(Config config){
        readSerializedInputs(config.getInputId());
    }
    public void readSerializedInputs(String inputId){
        String inputPath = getPath(inputId);
        inputs = (Map<String, List<Modality>>) readObject(inputPath);
    }

    public void getInferenceInputs(Config config){
        readSerializedInputs(config.getInferenceId());
    }

    @Override
    void initialize(String properties) {

    }

    @Override
    public Map<Integer, String> getTopics(String experimentId) {
        return null;
    }

    @Override
    public void prepareTopicKeyphraseWrite(Map<Integer, Map<String, List<Integer>>> topicTitles, String experimentId, String keyphraseTagger) {

    }

    @Override
    public void prepareOutput(String experimentId) {

    }

    @Override
    public void saveResults(List<TopicData> topicData, List<DocumentTopicAssignment> docTopics, String batchId, String experimentId,
                            String experimentDescription, String experimentMetadata){

        writeObject(topicData, getPath("topic_results_" + experimentId));
        writeObject(docTopics, getPath("document_assignments_" + experimentId));
    }

    @Override
    public void saveDiagnostics(int numModalities, String batchId, String experimentId, double[][] perplexities,
                                int numTopics, List<FastQMVWVTopicModelDiagnostics.TopicScores> topicScores) {

        // ensure serializables
        Double [][] perplexityObjArray = Utils.toDouble2DObject(perplexities);

        ArrayList<Quadruple<String, Double[], Double[][], Boolean>> diags = new ArrayList<>();
        for (FastQMVWVTopicModelDiagnostics.TopicScores ts : topicScores) diags.add(ts.toQuadruple());

        // bundle
        Pair<Double[][], ArrayList<Quadruple<String, Double[], Double[][], Boolean>>> data =
                new Pair<> (perplexityObjArray, diags);
        Quadruple<Integer, Integer, String, String> meta = new Quadruple<>(numModalities, numTopics, batchId, experimentId);

        Pair< Pair<Double[][], ArrayList<Quadruple<String, Double[], Double[][], Boolean>>>,
                Quadruple<Integer, Integer, String, String>> diag = new Pair<>(data, meta);

        // serialize
        writeObject(diag, "diagnostics_" + experimentId + ".ser");

    }

    @Override
    public void prepareTopicDistroTrendsOutput(String experimentId) {

    }

    @Override
    public void saveDocumentTopicAssignments(Config config, Map<String, Map<Integer, Double>> docTopicMap, String outpath) {
        writeObject(docTopicMap, getPath(outpath + ".docTopicAssignments"));
    }

    public FastQMVWVTopicInferencer getInferenceModel(Config config){
        String path = config.getInferenceModelDataSourceParams();
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(path)));
            String experimentId = (String) ois.readObject();
            if (! experimentId.equals(config.getExperimentId())){
                LOGGER.error("Attempted to read inferencer for experiment id " + config.getExperimentId() + " but read model has a different experiment id: " + experimentId);
            }
            //return  FastQMVWVTopicInferencer.readFromByteArray(((String) ois.readObject()).getBytes());
            return  (FastQMVWVTopicInferencer) ois.readObject();
//            byte[] model = ((String) ois.readObject()).getBytes();
//            return FastQMVWVTopicInferencer.readFromByteArray(model);
            //return model;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    @Override
    public void deleteExistingExperiment(Config config) {
        String experimentPath = getPath(config.getModellingOutputDataSourceParams() + ".experiment");
        deleteFile(experimentPath);
    }

    @Override
    public void saveTopicsAndExperiment(Config config, List<TopicAnalysis> topicAnalysisList, List<TopicDetails> topicDetailsList, Object serializedModel, String experimentMetadata) {
        String path = getPath(config.getModellingOutputDataSourceParams() + ".topicsAndExperiment");
        String experimentId = config.getExperimentId();
        try{

            LOGGER.info("Serializing experiment and topic information to JSON");

            File f= new File(path);
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
            oos.writeObject(experimentId);
            oos.writeObject(serializedModel);
            oos.writeObject(topicAnalysisList);
            oos.writeObject(topicDetailsList);
            oos.writeObject(experimentMetadata);
            oos.close();
            LOGGER.info("Saved experiment and topic information to " + path);
        } catch (IOException e) {
            LOGGER.error("Problem serializing ParallelTopicModel to file " + path + ": " + e);
        }

    }

    @Override
    public void saveDiagnostics(Config config, List<Score> scores) {

    }

    @Override
    public void saveSemanticAugmentationSingleOutput(List<DBpediaResource> entities, String pubId, DBpediaAnnotator.AnnotatorType annotator) {

    }

    @Override
    public void saveSemanticOutputResourceDetails(DBpediaResource resource) {

    }

    @Override
    public void loadSemanticAugmentationInputs(int queueSize) {

    }

    @Override
    public Text getNextSemanticAugmentationInput(int queueSize) {
        return null;
    }

    @Override
    public void loadSemanticDetailExtractionInputs(int queueSize) {

    }

    @Override
    public String getNextSemanticDetailExtractionInput(int queueSize) {
        return null;
    }

}

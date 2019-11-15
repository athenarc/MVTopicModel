package org.madgik.MVTopicModel.io;

import cc.mallet.types.Instance;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.madgik.MVTopicModel.FastQMVWVTopicInferencer;
import org.madgik.MVTopicModel.FastQMVWVTopicModelDiagnostics;
import org.madgik.MVTopicModel.config.Config;
import org.madgik.MVTopicModel.model.*;
import org.madgik.utils.Utils;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonTMDataSource extends FileTMDataSource{
    public static final String name = "json";
    public JsonTMDataSource(String properties) {
        super(properties);
    }

    @Override
    void initialize(String properties) {

    }


    @Override
    public String getPath(String path) {
        return path + ".json";
    }

    @Override
    public ArrayList<ArrayList<Instance>> getModellingInputs(Config config) {
        return null;
    }

    @Override
    public ArrayList<ArrayList<Instance>> getInferenceInputs(Config config) { return null; }

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
        writeObject(docTopics, getPath("document_assignments" + experimentId));
    }



    @Override
    public void writeObject(Object o, String path) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(path);
            new Gson().toJson(o, fw);
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (fw!=null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    @Override
    public Object readObject(String path) {
        return readObject(path, Object.class);
    }

    public Object readObject(String path, Class cl) {
        FileReader fr = null;
        try {
            fr = new FileReader(path);
            return new Gson().fromJson(fr, cl);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        finally {
                try {
                    fr.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
        }
        return null;
    }



    @Override
    public void saveDiagnostics(int numModalities, String batchId, String experimentId, double[][] perplexities,
                                int numTopics, List<FastQMVWVTopicModelDiagnostics.TopicScores> diagnostics) {

        String outpath = getPath("inference_" + experimentId);
        writeObject(diagnostics, outpath);


        Double[][] serializablePerplexities = Utils.toDouble2DObject(perplexities);
        outpath = getPath("perplexity" + experimentId);
        writeObject(serializablePerplexities, outpath);

    }

    @Override
    public void prepareTopicDistroTrendsOutput(String experimentId) {

    }


    @Override
    public void saveDocumentTopicAssignments(Config config, Map<String, Map<Integer, Double>> docTopicMap, String outpath) {
        String path = getPath(outpath + ".docTopicAssignments");
        logger.info("Saving document topic assignments" + path);
        String experimentId = config.getExperimentId();
        Map<String, String> data = new HashMap<>();
        data.put("assignments", new Gson().toJson(docTopicMap));
        data.put("experimentId", experimentId);
        try{
            FileWriter fr = new FileWriter(path);
            new Gson().toJson(data, fr);
            fr.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public FastQMVWVTopicInferencer getInferenceModel(Config config) {
        String path = config.getInferenceModelDataSourceParams();
        logger.info("Deserializing json: " + path);
        Map<String, String> data = null;
        try {
            FileReader fr = new FileReader(path);
            data = new Gson().fromJson(fr, Map.class);
            return FastQMVWVTopicInferencer.readEntireObject(DatatypeConverter.parseBase64Binary(data.get("serialized_model")));
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    @Override
    public void deleteExistingExperiment(Config config) {
        String experimentPath = getPath(config.getModellingOutputDataSourceParams() + ".experiment");
        deleteFile(experimentPath);
    }

    @Override
    public void saveTopicsAndExperiment(Config config, List<TopicAnalysis> topicAnalysisList, List<TopicDetails> topicDetailsList, byte[] serializedModel, String experimentMetadata) {
        String path = getPath(config.getModellingOutputDataSourceParams() + ".topicsAndExperiment");
        String experimentId = config.getExperimentId();
        Map<String, String> data = new HashMap<>();
        try{

            logger.info("Serializing experiment and topic information to JSON");
            data.put("serialized_model", DatatypeConverter.printBase64Binary(serializedModel));
            data.put("topic_analysis", new Gson().toJson(topicAnalysisList));
            data.put("topic_details", new Gson().toJson(topicDetailsList));
            data.put("experiment_metadata", new Gson().toJson(experimentMetadata));
            data.put("experimentId", experimentId);
            logger.info("Saving experiment and topic information to " + path);
            FileWriter fr = new FileWriter(path);
            new Gson().toJson(data, fr);
            fr.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch(Exception e) {
            logger.error(e.getMessage());
        }
        logger.info("Saved experiment and topic information to " + path);

    }

    @Override
    public void saveDiagnostics(Config config, List<Score> scores) {
        String path = getPath(config.getModellingOutputDataSourceParams() + ".diagnostics");
        Map<String, String> data = new HashMap<>();
        try{

            logger.info("Serializing diagnostics information to JSON:" + path);
            data.put("diagnostics", new Gson().toJson(scores));
            FileWriter fr = new FileWriter(path);
            new Gson().toJson(data, fr);
            fr.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        logger.info("Saved diagnostics information to JSON");
    }

}

package org.madgik.MVTopicModel.io;

import cc.mallet.types.Instance;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.madgik.MVTopicModel.FastQMVWVTopicModelDiagnostics;
import org.madgik.MVTopicModel.config.Config;
import org.madgik.MVTopicModel.model.DocumentTopicAssignment;
import org.madgik.MVTopicModel.model.TopicData;
import org.madgik.utils.Utils;

import java.io.*;
import java.util.ArrayList;
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
}

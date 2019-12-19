package org.madgik.io;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.madgik.MVTopicModel.FastQMVWVTopicInferencer;
import org.madgik.MVTopicModel.FastQMVWVTopicModelDiagnostics;
import org.madgik.config.Config;
import org.madgik.MVTopicModel.model.*;
import org.madgik.dbpediaspotlightclient.DBpediaAnnotator;
import org.madgik.dbpediaspotlightclient.DBpediaResource;
import org.madgik.io.modality.Modality;
import org.madgik.io.modality.Text;
import org.madgik.utils.Utils;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.util.*;

public class JsonTMDataSource extends FileTMDataSource{
    public static final String name = "json";
    private List<Text> semanticAnnotationInputs;
    private List<String> semanticDetailExtractionInputs;

    public JsonTMDataSource(String properties) {
        super(properties);
        this.parameters = properties;
    }

    @Override
    void initialize(String properties) {

    }

    /**
     * Write vocabulary after all preprocessing has taken place.
     * @param instances: The input instances to the TM engine
     * @param modalities: Modalities to process
     * @param path: Output path
     */
    public void writeTokenized(InstanceList[] instances, List<String> modalities, String path){
        Map<String, List<String>> al = new HashMap();
        for (int i=0; i <modalities.size(); i++){
            InstanceList in = instances[i];
            List<String> entries = new ArrayList<>();
            for (Object o: in.get(0).getAlphabet().toArray()) entries.add(o.toString());
            al.put(modalities.get(i), entries);
        }
        writeObject(al, path);
    }

    @Override
    public String getPath(String path) {
        return path + ".json";
    }

    @Override
    public void getModellingInputs(Config config) {
        getJsonInputs(config);
    }

    @Override
    public void getInferenceInputs(Config config) {
        getJsonInputs(config);
    }
    private void getJsonInputs(Config config) {
        List<String> modalities = config.getModalities();
        // Map<String, List<JsonObject>> contents = (Map) readObject(this.parameters);
        JsonObject contents = (JsonObject) readObject(this.parameters, JsonObject.class);

        inputs = new HashMap<>();
        for(String modality : contents.keySet()){
            if (! modalities.contains(modality)){
                LOGGER.error("MVTopicModelModality " + modality + " in data but undefined in config. Ignoring.");
                continue;
            }
            Class cls = Modality.type.modalityClassName(modality);

            List<Modality> modalityContents = new ArrayList<>();
            JsonArray objList = (JsonArray) contents.get(modality);
            for (JsonElement obj: objList){
                modalityContents.add((Modality) new Gson().fromJson(obj, cls));
            }
            if (!modalityContents.isEmpty()) inputs.put(modality, modalityContents);
        }
    }


    public static void main(String[] args) {
        JsonTMDataSource io = new JsonTMDataSource("/home/nik/data.json");
        Config conf = new Config();
        String [] mod = {"text", "mesh"};
        conf.setModalities(Arrays.asList(mod));
        io.getInferenceInputs(conf);
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
                    LOGGER.error(e.getMessage());
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
                    LOGGER.error(e.getMessage());
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
        LOGGER.info("Saving document topic assignments to: " + path);
        String experimentId = config.getExperimentId();
        Map<String, String> data = new HashMap<>();
        data.put("assignments", new Gson().toJson(docTopicMap));
        data.put("experimentId", experimentId);
        try{
            FileWriter fr = new FileWriter(path);
            new Gson().toJson(data, fr);
            fr.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public FastQMVWVTopicInferencer getInferenceModel(Config config) {
        String path = config.getInferenceModelDataSourceParams();
        LOGGER.info("Deserializing json: " + path);
        Map<String, String> data = null;
        try {
            FileReader fr = new FileReader(path);
            data = new Gson().fromJson(fr, Map.class);
            return FastQMVWVTopicInferencer.readFromByteArray(DatatypeConverter.parseBase64Binary(data.get("serialized_model")));
        } catch (FileNotFoundException e) {
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
        Map<String, String> data = new HashMap<>();
        try{

            LOGGER.info("Serializing experiment and topic information to JSON");
            data.put("serialized_model", DatatypeConverter.printBase64Binary((byte[])serializedModel));
            data.put("topic_analysis", new Gson().toJson(topicAnalysisList));
            data.put("topic_details", new Gson().toJson(topicDetailsList));
            data.put("experiment_metadata", new Gson().toJson(experimentMetadata));
            data.put("experimentId", experimentId);
            LOGGER.info("Saving experiment and topic information to " + path);
            FileWriter fr = new FileWriter(path);
            new Gson().toJson(data, fr);
            fr.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } catch(Exception e) {
            LOGGER.error(e.getMessage());
        }
        LOGGER.info("Saved experiment and topic information to " + path);

    }

    @Override
    public void saveDiagnostics(Config config, List<Score> scores) {
        String path = getPath(config.getModellingOutputDataSourceParams() + ".diagnostics");
        Map<String, String> data = new HashMap<>();
        try{

            LOGGER.info("Serializing diagnostics information to JSON:" + path);
            data.put("diagnostics", new Gson().toJson(scores));
            FileWriter fr = new FileWriter(path);
            new Gson().toJson(data, fr);
            fr.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        LOGGER.info("Saved diagnostics information to JSON");
    }

    @Override
    public void saveSemanticAugmentationSingleOutput(List<DBpediaResource> entities, String pubId, DBpediaAnnotator.AnnotatorType annotator) {

    }

    @Override
    public void saveSemanticOutputResourceDetails(DBpediaResource resource) {

    }

    @Override
    public void loadSemanticAugmentationInputs(int queueSize) {
        String path = this.parameters;
        LOGGER.info("Deserializing json: " + path);
        try {
            FileReader fr = new FileReader(path);
            this.semanticAnnotationInputs = new Gson().fromJson(fr, List.class);
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public Text getNextSemanticAugmentationInput(int queueSize) {
        if (this.semanticAnnotationInputs.isEmpty()) return null;
        return this.semanticAnnotationInputs.remove(0);
    }

    @Override
    public void loadSemanticDetailExtractionInputs(int queueSize) {
        String path = this.parameters;
        LOGGER.info("Deserializing json: " + path);
        try {
            FileReader fr = new FileReader(path);
            this.semanticDetailExtractionInputs = new Gson().fromJson(fr, List.class);
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public String getNextSemanticDetailExtractionInput(int queueSize) {
        if (this.semanticDetailExtractionInputs.isEmpty()) return null;
        return this.semanticDetailExtractionInputs.remove(0);
    }

}

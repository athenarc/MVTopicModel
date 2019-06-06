package org.madgik.io;

import cc.mallet.types.Instance;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Quadruple;
import edu.stanford.nlp.util.Triple;
import org.apache.commons.lang.ArrayUtils;
import org.madgik.MVTopicModel.FastQMVWVTopicModelDiagnostics;
import org.madgik.config.Config;
import org.madgik.model.DocumentTopicAssignment;
import org.madgik.model.TopicData;
import org.madgik.utils.Utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SerializedFileTMDataSource extends FileTMDataSource {

    public static final String name = "serialized";
    public SerializedFileTMDataSource(String properties) {
        super(properties);
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
        logger.info("Serializing to " + outpath);
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
        logger.info("Reading serialized object from path " + inpath);
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


    public ArrayList<ArrayList<Instance>> getInputs(Config config){
        String inputId = config.getInputId();
        String inputPath;
        inputPath = getPath("inputs." +inputId);
        return (ArrayList<ArrayList<Instance>>) readObject(inputPath);

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
}

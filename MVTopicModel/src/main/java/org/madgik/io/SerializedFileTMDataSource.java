package org.madgik.io;

import cc.mallet.types.Instance;
import org.madgik.config.Config;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SerializedFileTMDataSource extends FileTMDataSource {

    public static final String name = "serialized";
    public SerializedFileTMDataSource(String properties) {
        super(properties);
    }

    /**
     * Read java serialized file
     * @param o
     */
    void writeObject(Object o, String outpath){
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

    Object readObject(String inpath){
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
        // get modalities
        String inputPath;
        if (config.getLimitDocs() > 0)
            inputPath = "serialized." + config.getLimitDocs() + ".out";
        else
            inputPath = "serialized.out";
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
}

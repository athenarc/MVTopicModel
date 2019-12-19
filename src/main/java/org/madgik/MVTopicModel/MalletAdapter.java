package org.madgik.MVTopicModel;

import cc.mallet.types.Instance;
import org.madgik.config.Config;
import org.madgik.io.modality.Modality;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MalletAdapter {
    Config config;
    Preprocessor preprocessor;
    public MalletAdapter(Config config) {
        this.config = config;
        preprocessor = new Preprocessor(config);
    }

    public Instance makeInstance(String content, String identifier, String modality){
        content = preprocessor.preprocess(content, modality);
        if (content == null) return null;
        return new Instance(content, null, identifier, modality);
    }
    public ArrayList<Instance> makeModalityInstanceCollection(List<Modality> contents, String modality){
        ArrayList<Instance> instanceBuffer = new ArrayList<>();
        for(int i=0; i< contents.size(); i++) {
            // instanceBuffer.get(0).add(new Instance(txt.substring(0, Math.min(txt.length() - 1, numChars)), null, rstxt.getString("docid"), "text"));
            String content = preprocessor.preprocess(contents.get(i).getContent(), modality);
            if (content == null) continue;
            Instance inst = new Instance(content, null,contents.get(i).getId(), modality);
            instanceBuffer.add(inst);
        }
        return instanceBuffer;
    }
    public ArrayList<ArrayList<Instance>> makeInstances(Map<String, List<Modality>> data ){
        ArrayList<ArrayList<Instance>> instanceBuffer = new ArrayList<>();
        for (Modality.type modtype : Modality.type.values()) {
            String modality = modtype.name();
            if (!data.keySet().contains(modality)) continue;
            instanceBuffer.add(makeModalityInstanceCollection(data.get(modality), modality));
        }
        return instanceBuffer;

    }
}

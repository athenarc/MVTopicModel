package org.madgik.MVTopicModel.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to represent multi-modal information pertaining to a single topic
 */
public class TopicData {
    int topicId;

    public int getTopicId() {
        return topicId;
    }

    public List<Modality> getModalities() {
        return modalities;
    }

    public Modality getModality(Modality.types type){
        for(Modality mod : modalities){
            if (Modality.types.values()[mod.getId()] == type) return mod;
        }
        return null;
    }
    public void addModality(Modality mod){
        this.modalities.add(mod);
    }

    List<Modality> modalities;

    public TopicData() {
        modalities = new ArrayList<>();
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

}

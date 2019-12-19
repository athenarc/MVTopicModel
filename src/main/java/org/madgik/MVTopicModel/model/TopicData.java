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

    public List<MVTopicModelModality> getModalities() {
        return modalities;
    }

    public MVTopicModelModality getModality(MVTopicModelModality.types type){
        for(MVTopicModelModality mod : modalities){
            if (MVTopicModelModality.types.values()[mod.getId()] == type) return mod;
        }
        return null;
    }
    public void addModality(MVTopicModelModality mod){
        this.modalities.add(mod);
    }

    List<MVTopicModelModality> modalities;

    public TopicData() {
        modalities = new ArrayList<>();
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

}

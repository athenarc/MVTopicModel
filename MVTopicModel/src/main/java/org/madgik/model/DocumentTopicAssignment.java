package org.madgik.model;

import java.util.HashMap;
import java.util.Map;

public class DocumentTopicAssignment {
    public void setId(String id) {
        this.id = id;
    }

    public DocumentTopicAssignment() {
        topicWeights = new HashMap<>();
    }

    String id;

    public void setTopicWeights(Map<Integer, Double> topicWeights) {
        this.topicWeights = topicWeights;
    }

    public String getId() {
        return id;
    }

    public Map<Integer, Double> getTopicWeights() {
        return topicWeights;
    }

    Map<Integer, Double> topicWeights;

}

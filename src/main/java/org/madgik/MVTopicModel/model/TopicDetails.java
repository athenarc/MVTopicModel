package org.madgik.MVTopicModel.model;

public class TopicDetails {
    public int getTopicId() {
        return TopicId;
    }

    public int getTotalTokens() {
        return TotalTokens;
    }

    public int getModality() {
        return Modality;
    }

    public double getWeight() {
        return Weight;
    }

    public TopicDetails(int topicId, int totalTokens, int modality, double weight) {
        TopicId = topicId;
        TotalTokens = totalTokens;
        Modality = modality;
        Weight = weight;
    }

    int TopicId;
    int TotalTokens;
    int Modality;
    double Weight;

}

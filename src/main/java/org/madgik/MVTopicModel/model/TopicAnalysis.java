package org.madgik.MVTopicModel.model;

public class TopicAnalysis {
    public int getTopicId() {
        return TopicId;
    }

    int TopicId;
    int Modality;
    String Item;

    public int getModality() {
        return Modality;
    }

    public String getItem() {
        return Item;
    }

    public double getWeight() {
        return Weight;
    }

    double Weight;

    public TopicAnalysis(int id, int modality, String item, double count) {
        TopicId = id;
        Modality = modality;
        Item = item;
        Weight = count;
    }
}

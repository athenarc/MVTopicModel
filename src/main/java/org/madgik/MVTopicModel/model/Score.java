package org.madgik.MVTopicModel.model;

public class Score {
    public double getValue() {
        return value;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public type getScoreType() {
        return scoreType;
    }

    public enum type{
        CORPUS, TOPIC;
    }
    double value;
    String id;
    String name;

    public Score(double value, String id, type Type, String name) {
        this.value = value;
        this.id = id;
        this.name = name;
        this.scoreType = Type;
    }
    type scoreType;
}

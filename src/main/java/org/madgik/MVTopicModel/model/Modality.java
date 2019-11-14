package org.madgik.MVTopicModel.model;

import java.util.HashMap;
import java.util.Map;

public class Modality {
    public enum types{
        TEXT, DBPEDIA, MESH, KEYWORDS;
    }
    public void setId(int id) {
        this.id = id;
    }

    public void setWordWeights(Map<String, Double> wordWeights) {
        this.wordWeights = wordWeights;
    }

    public void setPhraseWeights(Map<String, Double> phraseWeights) {
        this.phraseWeights = phraseWeights;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void setNumTokens(int numTokens) {
        this.numTokens = numTokens;
    }

    public Modality() {
        wordWeights = new HashMap<>();
        phraseWeights = new HashMap<>();
    }

    public int getId() {
        return id;
    }

    int id;

    public Map<String, Double> getWordWeights() {
        return wordWeights;
    }

    public Map<String, Double> getPhraseWeights() {
        return phraseWeights;
    }

    Map<String, Double> wordWeights;
    Map<String, Double> phraseWeights;
    double weight;

    public double getWeight() {
        return weight;
    }

    public int getNumTokens() {
        return numTokens;
    }

    int numTokens;

}

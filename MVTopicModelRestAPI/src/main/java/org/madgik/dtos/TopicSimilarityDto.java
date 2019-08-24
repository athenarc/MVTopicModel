package org.madgik.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TopicSimilarityDto extends ParentDto{
    @JsonIgnore
    String experimentId1;
    Integer topicId1;
    @JsonIgnore
    String experimentId2;
    Integer topicId2;

    @Override
    public String toString() {

        return "TopicSimilarityDto{" +
                "experimentId1=" + experimentId1 +
                "experimentId2=" + experimentId2 +
                "topicId1=" + topicId1 +
                "topicId2=" + topicId2 +
                "simlarity=" + similarity +
                '}';

    }

    public TopicSimilarityDto(String experimentid1, Integer topicid1, String experimentid2, Integer topicid2, Double similarity) {
        this.experimentId1 = experimentid1;
        this.topicId1 = topicid1;
        this.experimentId2 = experimentid2;
        this.topicId2 = topicid2;
        this.similarity = similarity;
    }

    public String getExperimentId1() {
        return experimentId1;
    }

    public void setExperimentId1(String experimentId1) {
        this.experimentId1 = experimentId1;
    }

    public Integer getTopicId1() {
        return topicId1;
    }

    public void setTopicId1(Integer topicId1) {
        this.topicId1 = topicId1;
    }

    public String getExperimentId2() {
        return experimentId2;
    }

    public void setExperimentId2(String experimentId2) {
        this.experimentId2 = experimentId2;
    }

    public Integer getTopicId2() {
        return topicId2;
    }

    public void setTopicId2(Integer topicId2) {
        this.topicId2 = topicId2;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    Double similarity;


}

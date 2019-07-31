package org.madgik.rest.requests;

import java.io.Serializable;

public class TopicCurationRequest implements Serializable {

    private Integer topicId;
    private String experimentId;
    private String curatedDescription;

    public TopicCurationRequest() {

    }

    public TopicCurationRequest(Integer topicId, String experimentId, String curatedDescription) {
        this.topicId = topicId;
        this.experimentId = experimentId;
        this.curatedDescription = curatedDescription;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public String getCuratedDescription() {
        return curatedDescription;
    }

    public void setCuratedDescription(String curatedDescription) {
        this.curatedDescription = curatedDescription;
    }

    @Override
    public String toString() {
        return "TopicCurationRequest{" +
                "topicId=" + topicId +
                ", experimentId='" + experimentId + '\'' +
                ", curatedDescription='" + curatedDescription + '\'' +
                '}';
    }
}

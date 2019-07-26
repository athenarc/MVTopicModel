package org.madgik.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Objects;

public class TopicCurationDto implements Serializable {

    private Integer topicId;
    private String experimentId;
    @JsonIgnore
    private TopicDto topic;
    private String curatedDescription;

    public TopicCurationDto() {

    }

    public TopicCurationDto(Integer topicId, String experimentId, TopicDto topic,
                            String curatedDescription) {
        this.topicId = topicId;
        this.experimentId = experimentId;
        this.topic = topic;
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

    public TopicDto getTopic() {
        return topic;
    }

    public void setTopic(TopicDto topic) {
        this.topic = topic;
    }

    public String getCuratedDescription() {
        return curatedDescription;
    }

    public void setCuratedDescription(String curatedDescription) {
        this.curatedDescription = curatedDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicCurationDto that = (TopicCurationDto) o;
        return topicId.equals(that.topicId) &&
                experimentId.equals(that.experimentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicId, experimentId);
    }

    @Override
    public String toString() {
        return "TopicCurationDto{" +
                "topicId=" + topicId +
                ", experimentId='" + experimentId + '\'' +
                ", topic=" + topic +
                ", curatedDescription='" + curatedDescription + '\'' +
                '}';
    }
}

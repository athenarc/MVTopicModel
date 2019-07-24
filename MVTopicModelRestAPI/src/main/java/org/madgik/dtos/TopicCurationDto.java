package org.madgik.dtos;

import java.io.Serializable;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class TopicCurationDto implements Serializable {

    private TopicCurationIdDto topicCurationId;
    @JsonIgnore
    private TopicDto topic;
    private String curatedDescription;

    public TopicCurationDto() {

    }

    public TopicCurationDto(TopicCurationIdDto topicCurationId, TopicDto topic, String curatedDescription) {
        this.topicCurationId = topicCurationId;
        this.topic = topic;
        this.curatedDescription= curatedDescription;
    }

    public TopicCurationIdDto getTopicCurationId() {
        return topicCurationId;
    }

    public void setTopicCurationId(TopicCurationIdDto topicCurationId) {
        this.topicCurationId = topicCurationId;
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
        return topicCurationId.equals(that.topicCurationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicCurationId);
    }

    @Override
    public String toString() {
        return "TopicCurationDto{" +
                "topicCurationId=" + topicCurationId +
                ", topic=" + topic +
                ", curatedDescription=" + curatedDescription +
                '}';
    }
}

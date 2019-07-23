package org.madgik.persistence.entities;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "topic_curation")
public class TopicCuration implements Serializable {

    @EmbeddedId
    private TopicCurationId topicCurationId;

    @OneToOne
    @MapsId("topicId")
    @JoinColumns({
            @JoinColumn(name="topicid", referencedColumnName="id"),
            @JoinColumn(name="experimentid", referencedColumnName="experimentid")
    })
    private Topic topic;
    @Column(name = "curated_description")
    private String curatedDescription;

    public TopicCuration() {

    }

    public TopicCuration(TopicCurationId topicCurationId, Topic topic, String curatedDescription) {
        this.topicCurationId = topicCurationId;
        this.topic = topic;
        this.curatedDescription = curatedDescription;
    }

    public TopicCurationId getTopicCurationId() {
        return topicCurationId;
    }

    public void setTopicCurationId(TopicCurationId topicCurationId) {
        this.topicCurationId = topicCurationId;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
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
        TopicCuration that = (TopicCuration) o;
        return topicCurationId.equals(that.topicCurationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicCurationId);
    }

    @Override
    public String toString() {
        return "TopicCuration{" +
                "topicCurationId=" + topicCurationId +
                ", topic=" + topic +
                ", curatedDescription=" + curatedDescription +
                '}';
    }
}

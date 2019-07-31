package org.madgik.persistence.entities;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TopicCurationId implements Serializable {

    private TopicId topicId;

    public TopicCurationId() {

    }

    public TopicCurationId(TopicId topicId) {
        this.topicId = topicId;
    }

    public TopicId getTopicId() {
        return topicId;
    }

    public void setTopicId(TopicId topicId) {
        this.topicId = topicId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicCurationId that = (TopicCurationId) o;
        return topicId.equals(that.topicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicId);
    }

    @Override
    public String toString() {
        return "TopicCurationId{" +
                "topicId=" + topicId +
                '}';
    }
}

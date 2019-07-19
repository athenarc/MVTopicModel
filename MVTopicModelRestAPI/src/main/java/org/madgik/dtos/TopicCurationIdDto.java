package org.madgik.dtos;

import java.io.Serializable;
import java.util.Objects;

public class TopicCurationIdDto implements Serializable {

    private TopicIdDto topicId;

    public TopicCurationIdDto() {

    }

    public TopicCurationIdDto(TopicIdDto topicId) {
        this.topicId = topicId;
    }

    public TopicIdDto getTopicId() {
        return topicId;
    }

    public void setTopicId(TopicIdDto topicId) {
        this.topicId = topicId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicCurationIdDto that = (TopicCurationIdDto) o;
        return topicId.equals(that.topicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicId);
    }

    @Override
    public String toString() {
        return "TopicCurationIdDto{" +
                "topicId=" + topicId +
                '}';
    }
}

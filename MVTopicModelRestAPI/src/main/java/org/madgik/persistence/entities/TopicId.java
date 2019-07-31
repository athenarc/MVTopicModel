package org.madgik.persistence.entities;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TopicId implements Serializable {

    @Column(nullable = false, name = "id")
    private Integer id;

    @Column(nullable = false, name = "experimentid")
    private String experimentId;

    public TopicId() {

    }

    public TopicId(Integer id, String experimentId) {
        this.id = id;
        this.experimentId = experimentId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicId topicId = (TopicId) o;
        return id.equals(topicId.id) &&
                experimentId.equals(topicId.experimentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, experimentId);
    }

    @Override
    public String toString() {
        return "TopicId{" +
                "id='" + id + '\'' +
                ", experimentId='" + experimentId + '\'' +
                '}';
    }
}

package org.madgik.dtos;

import java.io.Serializable;
import java.util.Objects;

public class TopicIdDto implements Serializable {

    private Integer id;
    private String experimentId;

    public TopicIdDto() {

    }

    public TopicIdDto(Integer id, String experimentId) {
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
        TopicIdDto that = (TopicIdDto) o;
        return id.equals(that.id) &&
                experimentId.equals(that.experimentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, experimentId);
    }

    @Override
    public String toString() {
        return "TopicIdDto{" +
                "id='" + id + '\'' +
                ", experimentId='" + experimentId + '\'' +
                '}';
    }
}

package org.madgik.persistence.compositeIds;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CurationDetailsId implements Serializable {

    @Column(name = "topicid")
    private Integer topicId;
    @Column(name = "experimentid")
    private String experimentId;
    @Column(name = "curator")
    private String curator;

    public CurationDetailsId() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurationDetailsId that = (CurationDetailsId) o;
        return topicId.equals(that.topicId) &&
                experimentId.equals(that.experimentId) &&
                curator.equals(that.curator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicId, experimentId, curator);
    }

    @Override
    public String toString() {
        return "DocTopicId{" +
                "topicId=" + topicId +
                ", experimentId='" + experimentId + '\'' +
                ", curator='" + curator + '\'' +
                '}';
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

    public String getCurator() {
        return curator;
    }

    public void setCurator(String curator) {
        this.curator = curator;
    }
}

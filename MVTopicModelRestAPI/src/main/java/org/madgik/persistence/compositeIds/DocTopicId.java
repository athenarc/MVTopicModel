package org.madgik.persistence.compositeIds;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DocTopicId implements Serializable {

    @Column(name = "topicid")
    private Integer topicId;
    @Column(name = "experimentid")
    private String experimentId;
    @Column(name = "docid")
    private String docId;

    public DocTopicId() {

    }

    public DocTopicId(String docId, Integer topicId, String experimentId) {
        this.topicId = topicId;
        this.docId = docId;
        this.experimentId = experimentId;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocTopicId that = (DocTopicId) o;
        return topicId.equals(that.topicId) &&
                experimentId.equals(that.experimentId) &&
                docId.equals(that.docId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicId, experimentId, docId);
    }

    @Override
    public String toString() {
        return "DocTopicId{" +
                "topicId=" + topicId +
                ", experimentId='" + experimentId + '\'' +
                ", docId='" + docId + '\'' +
                '}';
    }
}

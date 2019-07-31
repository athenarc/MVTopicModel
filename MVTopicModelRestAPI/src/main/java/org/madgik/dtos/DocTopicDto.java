package org.madgik.dtos;

import java.util.Objects;

public class DocTopicDto extends ParentDto {

    private String docId;
    private Integer topicId;
    private String experimentId;
    private Double weight;
    private Boolean inferred;

    public DocTopicDto() {

    }

    public DocTopicDto(String docId, Integer topicId, String experimentId, Double weight,
                       Boolean inferred) {
        this.docId = docId;
        this.topicId = topicId;
        this.experimentId = experimentId;
        this.weight = weight;
        this.inferred = inferred;
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

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Boolean getInferred() {
        return inferred;
    }

    public void setInferred(Boolean inferred) {
        this.inferred = inferred;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocTopicDto that = (DocTopicDto) o;
        return docId.equals(that.docId) &&
                topicId.equals(that.topicId) &&
                experimentId.equals(that.experimentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(docId, topicId, experimentId);
    }

    @Override
    public String toString() {
        return "DocTopicDto{" +
                "docId='" + docId + '\'' +
                ", topicId=" + topicId +
                ", experimentId='" + experimentId + '\'' +
                ", weight=" + weight +
                ", inferred=" + inferred +
                '}';
    }
}

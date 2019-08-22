package org.madgik.dtos;

public class CurationDetailsDto extends AbstractDto{


    public CurationDetailsDto(String experimentId, Integer topicId, String label, String category, String curator) {
        this.experimentId = experimentId;
        this.topicId = topicId;
        this.label = label;
        this.category = category;
        this.curator = curator;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCurator() {
        return curator;
    }

    public void setCurator(String curator) {
        this.curator = curator;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    String experimentId;
    Integer topicId;
    String label;
    String category;
    String curator;

    @Override
    public String toString() {

        return "CurationDetailsDto{" +
                "topicId=" + topicId +
                "experimentId=" + experimentId +
                "curator=" + curator +
                "label=" + label +
                "category=" + category +
                '}';

    }



}
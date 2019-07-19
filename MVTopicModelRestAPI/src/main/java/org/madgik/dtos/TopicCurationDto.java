package org.madgik.dtos;

public class TopicCurationDto {

    private Integer TopicId;
    private String ExperimentId;

    public TopicCurationDto() {

    }

    public TopicCurationDto(Integer topicId, String experimentId) {
        this.TopicId = topicId;
        this.ExperimentId = experimentId;
    }

    public Integer getTopicId() {
        return TopicId;
    }

    public void setTopicId(Integer TopicId) {
        this.TopicId = TopicId;
    }

    public String getExperimentId() {
        return ExperimentId;
    }

    public void setExperimentId(String ExperimentId) {
        this.ExperimentId = ExperimentId;
    }

    @Override
    public String toString() {
        return "TopicCurationDto{" +
                "TopicId=" + TopicId +
                ", ExperimentId=" + ExperimentId +
                '}';
    }
}

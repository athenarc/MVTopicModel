package org.madgik.dtos;

import java.util.Objects;

public class VisualizationTopicDocsPerJournalDto extends ParentDto {

    private Integer topicId;
    private String experimentId;
    private String journalId;
    private Integer count;
    private String journalTitle;

    public VisualizationTopicDocsPerJournalDto() {

    }

    public VisualizationTopicDocsPerJournalDto(Integer topicId, String experimentId, String journalId, Integer count, String journalTitle) {
        this.topicId = topicId;
        this.experimentId = experimentId;
        this.journalId = journalId;
        this.count = count;
        this.journalTitle = journalTitle;
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

    public String getJournalId() {
        return journalId;
    }

    public void setJournalId(String journalId) {
        this.journalId = journalId;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getJournalTitle() {
        return journalTitle;
    }

    public void setJournalTitle(String journalTitle) {
        this.journalTitle = journalTitle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VisualizationTopicDocsPerJournalDto that = (VisualizationTopicDocsPerJournalDto) o;
        return topicId.equals(that.topicId) &&
                experimentId.equals(that.experimentId) &&
                journalId.equals(that.journalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicId, experimentId, journalId);
    }

    @Override
    public String toString() {
        return "VisualizationTopicDocsPerJournalDto{" +
                "topicId=" + topicId +
                ", experimentId='" + experimentId + '\'' +
                ", journalId='" + journalId + '\'' +
                ", count=" + count +
                ", journalTitle='" + journalTitle + '\'' +
                '}';
    }
}

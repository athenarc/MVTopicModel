package org.madgik.dtos;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class VisualizationTopicDocsPerJournalDto extends ParentDto {

    @JsonIgnore
    private Integer topicId;
    @JsonIgnore
    private String experimentId;

    private Integer count;
    private String journalTitle;

    public Integer getDocTopicCount() {
        return docTopicCount;
    }

    public void setDocTopicCount(Integer docTopicCount) {
        this.docTopicCount = docTopicCount;
    }

    private Integer docTopicCount;

    public VisualizationTopicDocsPerJournalDto() {

    }

    public VisualizationTopicDocsPerJournalDto(Integer topicId, String experimentId, Integer count, String journalTitle, Integer docTopicCount) {
        this.topicId = topicId;
        this.experimentId = experimentId;
        this.count = count;
        this.journalTitle = journalTitle;
        this.docTopicCount = docTopicCount;
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
                experimentId.equals(that.experimentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicId, experimentId);
    }

    @Override
    public String toString() {
        return "VisualizationTopicDocsPerJournalDto{" +
                "topicId=" + topicId +
                ", experimentId='" + experimentId + '\'' +
                ", count=" + count +
                ", journalTitle='" + journalTitle + '\'' +
                ", docTopicCount='" + docTopicCount + '\'' +
                '}';
    }
}

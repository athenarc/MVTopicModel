package org.madgik.persistence.compositeIds;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class VisualizationTopicDocsPerJournalId implements Serializable {

    @Column(name = "topicid")
    private Integer topicId;
    @Column(name = "experimentid")
    private String experimentId;
    @Column(name = "journaltitle")
    private String journalTitle;

    public VisualizationTopicDocsPerJournalId() {

    }

    public VisualizationTopicDocsPerJournalId(Integer topicId, String experimentId, String journalTitle) {
        this.topicId = topicId;
        this.experimentId = experimentId;
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

    public String getJournalTitle() {
        return journalTitle;
    }

    public void setJournalTitle(String journalTitle) {
        this.journalTitle = journalTitle;
    }

    @Override
    public String toString() {
        return "VisualizationTopicDocsPerJournalId{" +
                "topicId=" + topicId +
                ", experimentId='" + experimentId + '\'' +
                ", journalTitle='" + journalTitle + '\'' +
                '}';
    }
}

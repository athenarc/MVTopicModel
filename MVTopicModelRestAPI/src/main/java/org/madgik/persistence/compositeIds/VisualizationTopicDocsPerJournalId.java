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
    @Column(name = "journalid")
    private String journalId;

    public VisualizationTopicDocsPerJournalId() {

    }

    public VisualizationTopicDocsPerJournalId(Integer topicId, String experimentId, String journalId) {
        this.topicId = topicId;
        this.experimentId = experimentId;
        this.journalId = journalId;
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

    @Override
    public String toString() {
        return "VisualizationTopicDocsPerJournalId{" +
                "topicId=" + topicId +
                ", experimentId='" + experimentId + '\'' +
                ", journalId='" + journalId + '\'' +
                '}';
    }
}

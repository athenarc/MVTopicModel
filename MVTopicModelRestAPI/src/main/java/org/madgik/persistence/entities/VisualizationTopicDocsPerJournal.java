package org.madgik.persistence.entities;

import org.hibernate.annotations.DynamicUpdate;
import org.madgik.persistence.compositeIds.VisualizationTopicDocsPerJournalId;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "visualization_topic_docs_per_journal")
@DynamicUpdate
public class VisualizationTopicDocsPerJournal implements Serializable {

    @EmbeddedId
    private VisualizationTopicDocsPerJournalId visualizationTopicDocsPerJournalId;
    @Column(name = "count")
    private Integer count;
    @Column(name = "journaltitle")
    private String journalTitle;

    public VisualizationTopicDocsPerJournal() {

    }

    public VisualizationTopicDocsPerJournal(VisualizationTopicDocsPerJournalId visualizationTopicDocsPerJournalId, Integer count, String journalTitle) {
        this.visualizationTopicDocsPerJournalId = visualizationTopicDocsPerJournalId;
        this.count = count;
        this.journalTitle = journalTitle;
    }

    public VisualizationTopicDocsPerJournalId getVisualizationTopicDocsPerJournalId() {
        return visualizationTopicDocsPerJournalId;
    }

    public void setVisualizationTopicDocsPerJournalId(VisualizationTopicDocsPerJournalId visualizationTopicDocsPerJournalId) {
        this.visualizationTopicDocsPerJournalId = visualizationTopicDocsPerJournalId;
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
        VisualizationTopicDocsPerJournal that = (VisualizationTopicDocsPerJournal) o;
        return visualizationTopicDocsPerJournalId.equals(that.visualizationTopicDocsPerJournalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(visualizationTopicDocsPerJournalId);
    }

    @Override
    public String toString() {
        return "VisualizationTopicDocsPerJournal{" +
                "visualizationTopicDocsPerJournalId=" + visualizationTopicDocsPerJournalId +
                ", count=" + count +
                ", journalTitle='" + journalTitle + '\'' +
                '}';
    }
}

package org.madgik.persistence.entities;

import org.madgik.persistence.compositeIds.CurationDetailsId;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "visualization_curation_topic_labels")
public class VisualizationCurationTopicLabels {
    @EmbeddedId
    private CurationDetailsId curationDetailsId;
    @Column(name = "topic_label")
    private String topicLabel;
    @Column(name = "category_label")
    private String categoryLabel;
    @Column(name = "changed")
    private Boolean changed;
    @Column(name = "saved")
    private Boolean saved;

    public VisualizationCurationTopicLabels() {

    }

    public VisualizationCurationTopicLabels(CurationDetailsId curationDetailsId, String topicLabel,
                                            String categoryLabel, Boolean changed, Boolean saved) {
        this.curationDetailsId = curationDetailsId;
        this.topicLabel = topicLabel;
        this.categoryLabel = categoryLabel;
        this.changed = changed;
        this.saved = saved;
    }

    public CurationDetailsId getCurationDetailsId() {
        return curationDetailsId;
    }

    public void setCurationDetailsId(CurationDetailsId curationDetailsId) {
        this.curationDetailsId = curationDetailsId;
    }

    public String getTopicLabel() {
        return topicLabel;
    }

    public void setTopicLabel(String topicLabel) {
        this.topicLabel = topicLabel;
    }

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public void setCategoryLabel(String categoryLabel) {
        this.categoryLabel = categoryLabel;
    }

    public Boolean getChanged() {
        return changed;
    }

    public void setChanged(Boolean changed) {
        this.changed = changed;
    }

    public Boolean getSaved() {
        return saved;
    }

    public void setSaved(Boolean saved) {
        this.saved = saved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VisualizationCurationTopicLabels that = (VisualizationCurationTopicLabels) o;
        return Objects.equals(curationDetailsId, that.curationDetailsId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(curationDetailsId);
    }

    @Override
    public String toString() {
        return "VisualizationCurationTopicLabels{" +
                "curationDetailsId=" + curationDetailsId +
                ", topicLabel='" + topicLabel + '\'' +
                ", categoryLabel='" + categoryLabel + '\'' +
                ", changed=" + changed +
                ", saved=" + saved +
                '}';
    }
}

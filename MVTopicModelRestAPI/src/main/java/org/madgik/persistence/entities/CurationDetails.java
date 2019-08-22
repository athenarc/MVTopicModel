package org.madgik.persistence.entities;

import org.hibernate.annotations.DynamicUpdate;
import org.madgik.persistence.compositeIds.CurationDetailsId;
import org.madgik.persistence.compositeIds.DocTopicId;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "topic_curation_details")
@DynamicUpdate
public class CurationDetails {


    public String getLabel() {
        return label;
    }

    public CurationDetailsId getCurationDetailsId() {
        return curationDetailsId;
    }

    public void setCurationDetailsId(CurationDetailsId curationDetailsId) {
        this.curationDetailsId = curationDetailsId;
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


    String label;
    String category;
    @EmbeddedId
    CurationDetailsId curationDetailsId;

    @Override
    public String toString() {

        return "CurationDetails{" +
                "curationDetailsId=" + curationDetailsId +
                "label=" + label +
                "category=" + category +
                '}';

    }


}

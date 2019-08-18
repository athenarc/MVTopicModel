package org.madgik.persistence.compositeIds;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DocJournalId implements Serializable {

    @Column(name = "docid")
    private String docId;
    @Column(name = "journalid")
    private String journalId;



    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getJournalId() {
        return journalId;
    }

    public void setJournalId(String journalId) {
        this.journalId = journalId;
    }

    public DocJournalId() {

    }

    public DocJournalId(String docId, Integer topicId, String experimentId) {
        this.journalId = journalId;
        this.docId = docId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocJournalId that = (DocJournalId) o;
        return journalId.equals(that.journalId) &&
                docId.equals(that.docId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalId, docId);
    }

    @Override
    public String toString() {
        return "DocJournalId{" +
                "journalId=" + journalId +
                ", docId='" + docId + '\'' +
                '}';
    }
}

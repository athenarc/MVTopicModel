package org.madgik.persistence.entities;

import org.hibernate.annotations.DynamicUpdate;
import org.madgik.persistence.compositeIds.DocJournalId;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@DynamicUpdate
@Table(name = "doc_journal_title_view")
public class DocJournal implements Serializable  {

    @EmbeddedId
    private DocJournalId docJournalId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Column(name="title")
    private String title;


    public DocJournal() {

    }
    public DocJournal(DocJournalId docJournalId) {
        this.docJournalId = docJournalId;
    }

    public DocJournalId getDocJournalId() {
        return docJournalId;
    }

    public void setDocJournalId(DocJournalId docJournalId) {
        this.docJournalId = docJournalId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocJournal other = (DocJournal) o;
        return docJournalId.equals(other.docJournalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(docJournalId);
    }

    @Override
    public String toString() {
        return "DocJournal{" +
                "docJournalId=" + docJournalId +
                '}';
    }


}

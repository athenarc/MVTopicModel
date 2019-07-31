package org.madgik.persistence.entities;

import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "doc_topic")
@DynamicUpdate
public class DocTopic implements Serializable {

    @EmbeddedId
    private DocTopicId docTopicId;

//    @ManyToMany
//    @MapsId("topicId")
//    @JoinColumns({
//            @JoinColumn(name="topicid", referencedColumnName="id"),
//            @JoinColumn(name="experimentid", referencedColumnName="experimentid")
//    })
//    private List<Topic> topic;

//    @ManyToOne
//    @MapsId("docId")
//    @JoinColumn(name = "doc_id")
//    private Document document;
    @Column(name = "weight")
    private Double weight;
    @Column(name = "inferred")
    private Boolean inferred;

    public DocTopic() {

    }

    public DocTopic(DocTopicId docTopicId, Double weight, Boolean inferred) {
        this.docTopicId = docTopicId;
        this.weight = weight;
        this.inferred = inferred;
    }

    public DocTopicId getDocTopicId() {
        return docTopicId;
    }

    public void setDocTopicId(DocTopicId docTopicId) {
        this.docTopicId = docTopicId;
    }

//    public List<Topic> getTopic() {
//        return topic;
//    }

//    public void setTopic(List<Topic> topic) {
//        this.topic = topic;
//    }

//    public Document getDocument() {
//        return document;
//    }

//    public void setDocument(Document document) {
//        this.document = document;
//    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Boolean getInferred() {
        return inferred;
    }

    public void setInferred(Boolean inferred) {
        this.inferred = inferred;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocTopic docTopic = (DocTopic) o;
        return docTopicId.equals(docTopic.docTopicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(docTopicId);
    }

    @Override
    public String toString() {
        return "DocTopic{" +
                "docTopicId=" + docTopicId +
                ", weight=" + weight +
                ", inferred=" + inferred +
                '}';
    }
}

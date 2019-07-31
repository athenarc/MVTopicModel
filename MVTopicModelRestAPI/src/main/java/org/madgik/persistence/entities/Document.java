package org.madgik.persistence.entities;

import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "document")
@DynamicUpdate
public class Document extends AbstractEntity {

    @Column(name = "title")
    private String title;
    @Column(name = "abstract")
    private String abstractText;
    @Column(name = "doctype")
    private String doctype;
    @Column(name = "repository")
    private String repository;
    @Column(name = "pubyear")
    private String pubyear;
    @Column(name = "rights")
    private String rights;
    @Column(name = "collection")
    private String collection;
    @Column(name = "batchId")
    private String batchId;
    @Column(name = "hastext")
    private Boolean hasText;
    @Column(name = "language_pmc")
    private String languagePmc;
    @Column(name = "abstract_pmc")
    private String abstractPmc;
    @Column(name = "other_abstract_pmc")
    private String otherAbstractPmc;
    @Column(name = "publisher")
    @Size(max = 255)
    private String publisher;
    @Column(name = "toptokens")
    @Size(max = 255)
    private String topTokens;
    @Column(name = "url")
    @Size(max = 255)
    private String url;

//    @OneToMany(mappedBy = "document", cascade= CascadeType.ALL)
//    private List<DocTopic> docTopicList = new ArrayList<>();

    public Document() {

    }

    public Document(String id, String title, String abstractText, String doctype, String repository,
                    String pubyear, String rights, String collection, String batchId,
                    Boolean hasText, String languagePmc, String abstractPmc, String otherAbstractPmc,
                    String publisher, String topTokens, String url) {
        this.id = id;
        this.title = title;
        this.abstractText = abstractText;
        this.doctype = doctype;
        this.repository = repository;
        this.pubyear = pubyear;
        this.rights = rights;
        this.collection = collection;
        this.batchId = batchId;
        this.hasText = hasText;
        this.languagePmc = languagePmc;
        this.abstractPmc = abstractPmc;
        this.otherAbstractPmc = otherAbstractPmc;
        this.publisher = publisher;
        this.topTokens = topTokens;
        this.url = url;
    }
    public Document(String title, String abstractText, String doctype, String repository,
                    String pubyear, String rights, String collection, String batchId,
                    Boolean hasText, String languagePmc, String abstractPmc, String otherAbstractPmc,
                    String publisher, String topTokens, String url) {
        this.title = title;
        this.abstractText = abstractText;
        this.doctype = doctype;
        this.repository = repository;
        this.pubyear = pubyear;
        this.rights = rights;
        this.collection = collection;
        this.batchId = batchId;
        this.hasText = hasText;
        this.languagePmc = languagePmc;
        this.abstractPmc = abstractPmc;
        this.otherAbstractPmc = otherAbstractPmc;
        this.publisher = publisher;
        this.topTokens = topTokens;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getDoctype() {
        return doctype;
    }

    public void setDoctype(String doctype) {
        this.doctype = doctype;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getPubyear() {
        return pubyear;
    }

    public void setPubyear(String pubyear) {
        this.pubyear = pubyear;
    }

    public String getRights() {
        return rights;
    }

    public void setRights(String rights) {
        this.rights = rights;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public Boolean getHasText() {
        return hasText;
    }

    public void setHasText(Boolean hasText) {
        this.hasText = hasText;
    }

    public String getLanguagePmc() {
        return languagePmc;
    }

    public void setLanguagePmc(String languagePmc) {
        this.languagePmc = languagePmc;
    }

    public String getAbstractPmc() {
        return abstractPmc;
    }

    public void setAbstractPmc(String abstractPmc) {
        this.abstractPmc = abstractPmc;
    }

    public String getOtherAbstractPmc() {
        return otherAbstractPmc;
    }

    public void setOtherAbstractPmc(String otherAbstractPmc) {
        this.otherAbstractPmc = otherAbstractPmc;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getTopTokens() {
        return topTokens;
    }

    public void setTopTokens(String topTokens) {
        this.topTokens = topTokens;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

//    public List<DocTopic> getDocTopicList() {
//        return docTopicList;
//    }

//    public void setDocTopicList(List<DocTopic> docTopicList) {
//        this.docTopicList = docTopicList;
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Document document = (Document) o;
        return id.equals(document.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Document{" +
                "title='" + title + '\'' +
                ", abstractText='" + abstractText + '\'' +
                ", doctype='" + doctype + '\'' +
                ", repository='" + repository + '\'' +
                ", pubyear='" + pubyear + '\'' +
                ", rights='" + rights + '\'' +
                ", collection='" + collection + '\'' +
                ", batchId='" + batchId + '\'' +
                ", hasText=" + hasText +
                ", languagePmc='" + languagePmc + '\'' +
                ", abstractPmc='" + abstractPmc + '\'' +
                ", otherAbstractPmc='" + otherAbstractPmc + '\'' +
                ", publisher='" + publisher + '\'' +
                ", topTokens='" + topTokens + '\'' +
                ", url='" + url + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}

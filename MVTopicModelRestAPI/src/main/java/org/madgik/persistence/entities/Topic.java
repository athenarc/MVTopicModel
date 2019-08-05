package org.madgik.persistence.entities;

import org.hibernate.annotations.DynamicUpdate;
import org.madgik.persistence.compositeIds.TopicId;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "topic")
@DynamicUpdate
public class Topic implements Serializable {

    @EmbeddedId
    private TopicId topicId;
    @Column(name = "title")
    private String title;
    @Column(name = "category")
    private String category;
    @Column(name = "visibilityindex")
    private Integer visibilityIndex;
    @Column(name = "comments")
    private String comments;
    @Column(name = "icd11")
    private String icd11;
    @Column(name = "concepts")
    @Size(max = 255)
    private String concepts;
    @Column(name = "counts")
    private Integer counts;
    @Column(name = "dbpedia_icd10")
    @Size(max = 255)
    private String dbpediaIcd10;
    @Column(name = "dbpedia_label")
    @Size(max = 255)
    private String dbpediaLabel;
    @Column(name = "dbpedia_mesh")
    @Size(max = 255)
    private String dbpediaMesh;
    @Column(name = "dbpedia_type")
    @Size(max = 255)
    private String dbpediaType;
    @Column(name = "discrweight")
    private Double discrweight;
    @Column(name = "item")
    @Size(max = 255)
    private String item;
    @Column(name = "itemtype")
    private Integer itemType;
    @Column(name = "modality")
    @Size(max = 255)
    private String modality;
    @Column(name = "topicweight")
    private Double topicWeight;
    @Column(name = "totaltokens")
    private Integer totalTokens;
    @Column(name = "weightedcounts")
    private Integer weightedCounts;
    @Column(name = "rk")
    private Integer rk;
//    @ManyToMany(mappedBy = "topic", cascade=CascadeType.ALL)
//    private List<DocTopic> docTopicList = new ArrayList<>();

    public Topic() {

    }

    public Topic(TopicId topicId, String title, String category, Integer visibilityIndex,
                 String comments, String icd11, String concepts, Integer counts,
                 String dbpediaIcd10, String dbpediaLabel, String dbpediaMesh,
                 String dbpediaType, Double discrweight, String item, Integer itemType,
                 String modality, Double topicWeight, Integer totalTokens,
                 Integer weightedCounts, Integer rk) {
        this.topicId = topicId;
        this.title = title;
        this.category = category;
        this.visibilityIndex = visibilityIndex;
        this.comments = comments;
        this.icd11 = icd11;
        this.concepts = concepts;
        this.counts = counts;
        this.dbpediaIcd10 = dbpediaIcd10;
        this.dbpediaLabel = dbpediaLabel;
        this.dbpediaMesh = dbpediaMesh;
        this.dbpediaType = dbpediaType;
        this.discrweight = discrweight;
        this.item = item;
        this.itemType = itemType;
        this.modality = modality;
        this.topicWeight = topicWeight;
        this.totalTokens = totalTokens;
        this.weightedCounts = weightedCounts;
        this.rk = rk;
    }

    public TopicId getTopicId() {
        return topicId;
    }

    public void setTopicId(TopicId topicId) {
        this.topicId = topicId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getVisibilityIndex() {
        return visibilityIndex;
    }

    public void setVisibilityIndex(Integer visibilityIndex) {
        this.visibilityIndex = visibilityIndex;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getIcd11() {
        return icd11;
    }

    public void setIcd11(String icd11) {
        this.icd11 = icd11;
    }

    public String getConcepts() {
        return concepts;
    }

    public void setConcepts(String concepts) {
        this.concepts = concepts;
    }

    public Integer getCounts() {
        return counts;
    }

    public void setCounts(Integer counts) {
        this.counts = counts;
    }

    public String getDbpediaIcd10() {
        return dbpediaIcd10;
    }

    public void setDbpediaIcd10(String dbpediaIcd10) {
        this.dbpediaIcd10 = dbpediaIcd10;
    }

    public String getDbpediaLabel() {
        return dbpediaLabel;
    }

    public void setDbpediaLabel(String dbpediaLabel) {
        this.dbpediaLabel = dbpediaLabel;
    }

    public String getDbpediaMesh() {
        return dbpediaMesh;
    }

    public void setDbpediaMesh(String dbpediaMesh) {
        this.dbpediaMesh = dbpediaMesh;
    }

    public String getDbpediaType() {
        return dbpediaType;
    }

    public void setDbpediaType(String dbpediaType) {
        this.dbpediaType = dbpediaType;
    }

    public Double getDiscrweight() {
        return discrweight;
    }

    public void setDiscrweight(Double discrweight) {
        this.discrweight = discrweight;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public Integer getItemType() {
        return itemType;
    }

    public void setItemType(Integer itemType) {
        this.itemType = itemType;
    }

    public String getModality() {
        return modality;
    }

    public void setModality(String modality) {
        this.modality = modality;
    }

    public Double getTopicWeight() {
        return topicWeight;
    }

    public void setTopicWeight(Double topicWeight) {
        this.topicWeight = topicWeight;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Integer getWeightedCounts() {
        return weightedCounts;
    }

    public void setWeightedCounts(Integer weightedCounts) {
        this.weightedCounts = weightedCounts;
    }

    public Integer getRk() {
        return rk;
    }

    public void setRk(Integer rk) {
        this.rk = rk;
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
        Topic topic = (Topic) o;
        return topicId.equals(topic.topicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicId);
    }

    @Override
    public String toString() {
        return "Topic{" +
                "topicId=" + topicId +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", visibilityIndex=" + visibilityIndex +
                ", comments='" + comments + '\'' +
                ", icd11='" + icd11 + '\'' +
                ", concepts='" + concepts + '\'' +
                ", counts=" + counts +
                ", dbpediaIcd10='" + dbpediaIcd10 + '\'' +
                ", dbpediaLabel='" + dbpediaLabel + '\'' +
                ", dbpediaMesh='" + dbpediaMesh + '\'' +
                ", dbpediaType='" + dbpediaType + '\'' +
                ", discrweight=" + discrweight +
                ", item='" + item + '\'' +
                ", itemType=" + itemType +
                ", modality='" + modality + '\'' +
                ", topicWeight=" + topicWeight +
                ", totalTokens=" + totalTokens +
                ", weightedCounts=" + weightedCounts +
                ", rk=" + rk +
                '}';
    }
}

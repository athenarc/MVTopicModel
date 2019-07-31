package org.madgik.dtos;

import java.util.Objects;

public class TopicDto extends ParentDto {
    private TopicIdDto topicId;
    private String title;
    private String category;
    private Integer visibilityIndex;
    private String comments;
    private String icd11;
    private String concepts;
    private Integer counts;
    private String dbpediaIcd10;
    private String dbpediaLabel;
    private String dbpediaMesh;
    private String dbpediaType;
    private Double discrweight;
    private String item;
    private Integer itemType;
    private String modality;
    private Double topicWeight;
    private Integer totalTokens;
    private Integer weightedCounts;
    private Integer rk;

    public TopicDto() {

    }

    public TopicDto(TopicIdDto topicId, String title, String category,
                    Integer visibilityIndex, String comments, String icd11, String concepts,
                    Integer counts, String dbpediaIcd10, String dbpediaLabel,
                    String dbpediaMesh, String dbpediaType, Double discrweight, String item,
                    Integer itemType, String modality, Double topicWeight, Integer totalTokens,
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

    public TopicIdDto getTopicId() {
        return topicId;
    }

    public void setTopicId(TopicIdDto topicId) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicDto topicDto = (TopicDto) o;
        return topicId.equals(topicDto.topicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicId);
    }

    @Override
    public String toString() {
        return "TopicDto{" +
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

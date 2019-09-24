package org.madgik.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

public class VisualizationCurationTopicLabelsDto extends ParentDto {

    @JsonIgnore
    private CurationDetailsDto curationDetailsId;
    @JsonIgnore
    private String topicLabel;
    @JsonIgnore
    private String categoryLabel;
    @JsonIgnore
    private Boolean changed;
    @JsonIgnore
    private Boolean saved;

    private List<String> categories = new ArrayList<>();
    private Map<Integer,List<Integer>> topicCategoriesIdxMapping = new HashMap<>();
    @JsonIgnore
    private Map<Integer, List<String>> topicCategoriesMapping = new HashMap<>();

    public VisualizationCurationTopicLabelsDto() {

    }

    public VisualizationCurationTopicLabelsDto(CurationDetailsDto curationDetailsId, String topicLabel,
                                               String categoryLabel, Boolean changed, Boolean saved) {
        this.curationDetailsId = curationDetailsId;
        this.topicLabel = topicLabel;
        this.categoryLabel = categoryLabel;
        this.changed = changed;
        this.saved = saved;
    }

    public CurationDetailsDto getCurationDetailsId() {
        return curationDetailsId;
    }

    public void setCurationDetailsId(CurationDetailsDto curationDetailsId) {
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

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public Map<Integer, List<Integer>> getTopicCategoriesIdxMapping() {
        return topicCategoriesIdxMapping;
    }

    public void setTopicCategoriesIdxMapping(Map<Integer, List<Integer>> topicCategoriesIdxMapping) {
        this.topicCategoriesIdxMapping = topicCategoriesIdxMapping;
    }

    public Map<Integer, List<String>> getTopicCategoriesMapping() {
        return topicCategoriesMapping;
    }

    public void setTopicCategoriesMapping(Map<Integer, List<String>> topicCategoriesMapping) {
        this.topicCategoriesMapping = topicCategoriesMapping;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VisualizationCurationTopicLabelsDto that = (VisualizationCurationTopicLabelsDto) o;
        return curationDetailsId.equals(that.curationDetailsId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(curationDetailsId);
    }

    @Override
    public String toString() {
        return "VisualizationCurationTopicLabelsDto{" +
                "curationDetailsId=" + curationDetailsId +
                ", topicLabel='" + topicLabel + '\'' +
                ", categoryLabel='" + categoryLabel + '\'' +
                ", changed=" + changed +
                ", saved=" + saved +
                ", categories=" + categories +
                ", topicCategoriesIdxMapping=" + topicCategoriesIdxMapping +
                ", topicCategoriesMapping=" + topicCategoriesMapping +
                '}';
    }
}

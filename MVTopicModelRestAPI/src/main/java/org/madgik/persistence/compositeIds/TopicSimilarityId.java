package org.madgik.persistence.compositeIds;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TopicSimilarityId implements Serializable {

    @Column(name = "topicid1")
    private Integer topicId1;
    @Column(name = "experimentid1")
    private String experimentId1;
    @Column(name = "topicid2")
    private Integer topicId2;
    @Column(name = "experimentid2")
    private String experimentId2;

    public TopicSimilarityId() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicSimilarityId that = (TopicSimilarityId) o;
        return topicId1.equals(that.topicId1) &&
                topicId2.equals(that.topicId2) &&
                experimentId1.equals(that.experimentId1) &&
                experimentId2.equals(that.experimentId2);
    }

    public Integer getTopicId1() {
        return topicId1;
    }

    public void setTopicId1(Integer topicId1) {
        this.topicId1 = topicId1;
    }

    public String getExperimentId1() {
        return experimentId1;
    }

    public void setExperimentId1(String experimentId1) {
        this.experimentId1 = experimentId1;
    }

    public Integer getTopicId2() {
        return topicId2;
    }

    public void setTopicId2(Integer topicId2) {
        this.topicId2 = topicId2;
    }

    public String getExperimentId2() {
        return experimentId2;
    }

    public void setExperimentId2(String experimentId2) {
        this.experimentId2 = experimentId2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicId1, experimentId1, topicId2, experimentId2);
    }

    @Override
    public String toString() {
        return "DocTopicId{" +
                "topicId1=" + topicId1 +
                ", experimentId1='" + experimentId1 + '\'' +
                "topicId2=" + topicId2 +
                ", experimentId2='" + experimentId2 + '\'' +
                '}';
    }

}

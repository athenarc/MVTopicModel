package org.madgik.persistence.entities;

import org.hibernate.annotations.DynamicUpdate;
import org.madgik.persistence.compositeIds.TopicSimilarityId;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "topicsimilarity")
@DynamicUpdate
public class TopicSimilarity {


    @EmbeddedId
    TopicSimilarityId topicSimilarityId;

    public TopicSimilarityId getTopicSimilarityId() {
        return topicSimilarityId;
    }

    public void setTopicSimilarityId(TopicSimilarityId topicSimilarityId) {
        this.topicSimilarityId = topicSimilarityId;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    @Column(name="similarity")
    Double similarity;

    @Override
    public String toString() {

        return "TopicSimilarityDto{" +
                "TopicSimilarityId=" + topicSimilarityId +
                "simlarity=" + similarity +
                '}';

    }

}

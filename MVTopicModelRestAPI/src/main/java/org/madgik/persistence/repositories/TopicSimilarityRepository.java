package org.madgik.persistence.repositories;

import org.madgik.persistence.compositeIds.DocTopicId;
import org.madgik.persistence.compositeIds.TopicSimilarityId;
import org.madgik.persistence.entities.CurationDetails;
import org.madgik.persistence.entities.TopicSimilarity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TopicSimilarityRepository extends JpaRepository<TopicSimilarity, TopicSimilarityId> {


    @Query("select ts from TopicSimilarity ts where ts.topicSimilarityId.experimentId1 = :experimentId1 and ts.topicSimilarityId.experimentId2 = :experimentId2")
    List<TopicSimilarity> findAllByExperimentIds_forward(@Param("experimentId1") String experimentId1, @Param("experimentId2") String experimentId2);

    @Query("select ts from TopicSimilarity ts where ts.topicSimilarityId.experimentId1 = :experimentId2 and  ts.topicSimilarityId.experimentId2 = :experimentId1 ")
    List<TopicSimilarity> findAllByExperimentIds_backward(@Param("experimentId1") String experimentId1, @Param("experimentId2") String experimentId2);

}

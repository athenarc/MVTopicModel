package org.madgik.persistence.repositories;

import org.madgik.persistence.entities.DocTopic;
import org.madgik.persistence.entities.DocTopicId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocTopicRepository extends JpaRepository<DocTopic, DocTopicId> {
    @Query("select d from DocTopic d where d.docTopicId.topicId = :topicId and d.docTopicId.experimentId = :experimentId")
    List<DocTopic> findAllByTopicIdAndExperimentId(@Param("topicId") Integer topicId, @Param("experimentId") String experimentId);
}

package org.madgik.persistence.repositories;

import org.madgik.persistence.compositeIds.DocTopicId;
import org.madgik.persistence.entities.CurationDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CurationDetailsRepository extends JpaRepository<CurationDetails, DocTopicId> {


    @Query("select cd from CurationDetails cd where cd.curationDetailsId.experimentId = :experimentId")
    List<CurationDetails> findAllByExperimentId(@Param("experimentId") String experimentId);

    @Query("select cd from CurationDetails cd where cd.curationDetailsId.topicId = :topicId and cd.curationDetailsId.experimentId = :experimentId")
    List<CurationDetails> findAllByTopicIdAndExperimentId(@Param("topicId") Integer topicId, @Param("experimentId") String experimentId);
}

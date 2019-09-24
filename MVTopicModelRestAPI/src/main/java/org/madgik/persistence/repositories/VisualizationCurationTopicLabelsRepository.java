package org.madgik.persistence.repositories;

import org.madgik.persistence.compositeIds.CurationDetailsId;
import org.madgik.persistence.entities.VisualizationCurationTopicLabels;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisualizationCurationTopicLabelsRepository extends JpaRepository<VisualizationCurationTopicLabels, CurationDetailsId> {

    @Query("select v from VisualizationCurationTopicLabels v where v.curationDetailsId.experimentId = :experimentId and v.curationDetailsId.curator = :curator")
    List<VisualizationCurationTopicLabels> findAllByExperimentIdAndCurator(@Param("experimentId") String experimentId, @Param("curator") String curator);
}

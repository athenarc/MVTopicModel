package org.madgik.persistence.repositories;

import org.madgik.persistence.compositeIds.VisualizationTopicDocsPerJournalId;
import org.madgik.persistence.entities.VisualizationTopicDocsPerJournal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VisualizationTopicDocsPerJournalRepository extends JpaRepository<VisualizationTopicDocsPerJournal, VisualizationTopicDocsPerJournalId> {
    @Query("select v from VisualizationTopicDocsPerJournal v where v.visualizationTopicDocsPerJournalId.topicId = :topicId and v.visualizationTopicDocsPerJournalId.experimentId = :experimentId")
    Page<VisualizationTopicDocsPerJournal> findAllByTopicIdAndExperimentId(@Param("topicId") Integer topicId, @Param("experimentId") String experimentId, Pageable pageable);
}

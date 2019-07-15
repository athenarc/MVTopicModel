package org.madgik.persistence.repositories;

import org.madgik.persistence.entities.VisualizationDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisualizationDocumentRepository extends JpaRepository<VisualizationDocument, String> {

    @Query("select v from VisualizationDocument v where v.id in :ids")
    List<VisualizationDocument> findAllByIdIn(@Param("ids") List<String> ids, Pageable pageable);
}

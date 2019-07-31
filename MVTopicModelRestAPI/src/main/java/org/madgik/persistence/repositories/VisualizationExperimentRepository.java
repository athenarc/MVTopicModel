package org.madgik.persistence.repositories;

import org.madgik.persistence.entities.VisualizationExperiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisualizationExperimentRepository extends JpaRepository<VisualizationExperiment, String> {

}

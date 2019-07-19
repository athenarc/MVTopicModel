package org.madgik.persistence.repositories;

import org.madgik.persistence.entities.TopicCuration;
import org.madgik.persistence.entities.TopicCurationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicCurationRepository extends JpaRepository<TopicCuration, TopicCurationId> {

}

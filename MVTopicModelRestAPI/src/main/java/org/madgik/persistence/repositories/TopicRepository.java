package org.madgik.persistence.repositories;

import org.madgik.persistence.entities.Topic;
import org.madgik.persistence.compositeIds.TopicId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicRepository extends JpaRepository<Topic, TopicId> {

}

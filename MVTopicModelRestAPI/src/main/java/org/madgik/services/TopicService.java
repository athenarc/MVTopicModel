package org.madgik.services;

import org.madgik.dtos.TopicDto;
import org.madgik.persistence.entities.Topic;
import org.madgik.persistence.compositeIds.TopicId;
import org.madgik.persistence.repositories.TopicRepository;
import org.madgik.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service(Constants.TOPIC_SERVICE)
public class TopicService {

    @Autowired
    private MapperService mapperService;

    @Autowired
    private TopicRepository repo;

    public TopicDto createNewTopic(TopicDto topic) {
        return mapperService.getDto(repo.save(mapperService.getEntity(topic, Topic.class)), TopicDto.class);
    }

    public TopicDto getTopicByCompositeId(Integer topicId, String experimentId) {
        TopicId topicId1 = new TopicId(topicId, experimentId);
        Optional<Topic> topicOptional = repo.findById(topicId1);
        return topicOptional.map(topic -> mapperService.getDto(topic, TopicDto.class)).orElse(null);
    }

}

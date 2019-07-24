package org.madgik.services;

import org.madgik.dtos.TopicCurationDto;
import org.madgik.persistence.entities.TopicCuration;
import org.madgik.persistence.entities.TopicCurationId;
import org.madgik.persistence.entities.TopicId;
import org.madgik.persistence.repositories.TopicCurationRepository;
import org.madgik.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

import java.util.Optional;

@Service(Constants.TOPIC_CURATION_SERVICE)
public class TopicCurationService {

    @Autowired
    private MapperService mapperService;

    @Autowired
    private TopicCurationRepository repo;

    @Autowired
    private TopicService topicService;

    public TopicCurationDto createTopicCuration(TopicCurationDto topicCurationDto) {
        if (topicCurationDto.getTopic() == null) return null;
        topicService.createNewTopic(topicCurationDto.getTopic());
        return mapperService.convertTopicCurationEntityToDto(repo.save(mapperService.convertTopicCurationDtoToEntity(topicCurationDto)));
    }

    public TopicCurationDto getTopicCurationByTopicIdAndExperimentId(Integer topicId, String experimentId) {
        TopicId topicIdObj = new TopicId(topicId, experimentId);
        TopicCurationId topicCurationId = new TopicCurationId(topicIdObj);
        Optional<TopicCuration> topicCurationOptional = repo.findById(topicCurationId);
        if (topicCurationOptional.isPresent()) {
            TopicCuration topicCuration = topicCurationOptional.get();
            return mapperService.convertTopicCurationEntityToDto(topicCuration);
        }
        return null;
    }
    public List<TopicCurationDto> getAllTopicCurations(){
	    return mapperService.convertTopicCurationEntityListToDto(repo.findAll());
    }

}

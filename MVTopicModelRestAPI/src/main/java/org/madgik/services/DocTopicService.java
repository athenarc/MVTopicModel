package org.madgik.services;

import org.madgik.dtos.DocTopicDto;
import org.madgik.persistence.repositories.DocTopicRepository;
import org.madgik.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service(Constants.DOC_TOPIC_SERVICE)
public class DocTopicService {

    @Autowired
    private DocTopicRepository repo;

    @Autowired
    private MapperService mapperService;

    public List<DocTopicDto> getDocTopicsByTopicIdAndExperimentId(Integer topicId, String experimentId) {
        return mapperService.convertDocTopicListEntityToDto(repo.findAllByTopicIdAndExperimentId(topicId, experimentId));
    }
}

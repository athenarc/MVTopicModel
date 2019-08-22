package org.madgik.services;

import org.madgik.dtos.CurationDetailsDto;
import org.madgik.dtos.TopicSimilarityDto;
import org.madgik.persistence.entities.CurationDetails;
import org.madgik.persistence.entities.TopicSimilarity;
import org.madgik.persistence.repositories.CurationDetailsRepository;
import org.madgik.persistence.repositories.TopicSimilarityRepository;
import org.madgik.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service(Constants.TOPIC_SIMILARITY_SERVICE)
public class TopicSimilarityService {


    @Autowired
    private MapperService mapperService;

    @Autowired private TopicSimilarityRepository repo;

    public List<TopicSimilarityDto> findByExperimentIds(String experimentId1, String experimentId2){

        List<TopicSimilarity> ts =repo.findAllByExperimentIds_left(experimentId1,experimentId2);
        ts.addAll(repo.findAllByExperimentIds_right(experimentId1,experimentId2));
        return mapperService.convertTopicSimilarityToDto(ts);

    }
}

package org.madgik.services;

import org.madgik.dtos.TopicSimilarityDto;
import org.madgik.persistence.entities.TopicSimilarity;
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

        List<TopicSimilarity> ts =repo.findAllByExperimentIds_forward(experimentId1,experimentId2);
        List<TopicSimilarity> ts_back =repo.findAllByExperimentIds_backward(experimentId1,experimentId2);
        for(TopicSimilarity tt : ts_back){
            // swap'em
            Integer t = tt.getTopicSimilarityId().getTopicId1();
            tt.getTopicSimilarityId().setTopicId1(tt.getTopicSimilarityId().getTopicId2());
            tt.getTopicSimilarityId().setTopicId2(t);

            String s = tt.getTopicSimilarityId().getExperimentId1();
            tt.getTopicSimilarityId().setExperimentId1(tt.getTopicSimilarityId().getExperimentId2());
            tt.getTopicSimilarityId().setExperimentId2(s);

            if (! ts.contains(tt)) ts.add(tt);
        }

        return mapperService.convertTopicSimilarityToDto(ts);

    }
}

package org.madgik.services;

import org.madgik.dtos.CurationDetailsDto;
import org.madgik.dtos.CurationTopicCategories;
import org.madgik.dtos.CurationTopicsCategories;
import org.madgik.persistence.entities.CurationDetails;
import org.madgik.persistence.repositories.CurationDetailsRepository;
import org.madgik.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service(Constants.CURATION_DETAILS_SERVICE)
public class CurationDetailsService {


    @Autowired
    private MapperService mapperService;

    @Autowired private CurationDetailsRepository repo;

    public List<CurationDetailsDto> findByExperimentId(String experimentId){
        // public Page<CurationDetailsDto> getCurationDetailsByTopicIdAndExperimentId(Integer topicId, String experimentId){
        List<CurationDetails> cd =repo.findAllByExperimentId(experimentId);
        return mapperService.convertCurationDetailsToDto(cd);

    }
    public CurationTopicsCategories mapToCategoriesPerTopic(List<CurationDetailsDto> inp){
        List<CurationTopicCategories> topicCategories = new ArrayList<>();

        // get all major categories
        List<String> categories = new ArrayList<>();
        for(CurationDetailsDto cd : inp)
            if (! categories.contains(cd.getCategory())) categories.add(cd.getCategory());
        Collections.sort(categories);
        // init all topics to zero assignment
        for (int i=0; i<inp.size(); ++i) {
            CurationDetailsDto cd = inp.get(i);
            List<Double> assignments = new ArrayList<>();
            for (String c : categories) assignments.add(new Double(0.0d));
            int index = categories.indexOf(cd.getCategory());
            assignments.set(index, 1.0d);
            CurationTopicCategories ctc = new CurationTopicCategories(cd.getTopicId(), assignments);
            topicCategories.add(ctc);
        }
        return new CurationTopicsCategories(topicCategories, categories);
    }
}

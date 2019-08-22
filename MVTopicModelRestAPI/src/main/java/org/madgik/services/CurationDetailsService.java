package org.madgik.services;

import org.madgik.dtos.CurationDetailsDto;
import org.madgik.persistence.entities.CurationDetails;
import org.madgik.persistence.repositories.CurationDetailsRepository;
import org.madgik.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
}

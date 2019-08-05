package org.madgik.services;

import org.madgik.dtos.VisualizationTopicDocsPerJournalDto;
import org.madgik.persistence.entities.VisualizationTopicDocsPerJournal;
import org.madgik.persistence.repositories.VisualizationTopicDocsPerJournalRepository;
import org.madgik.rest.requests.VisualizationTopicDocsPerJournalRequest;
import org.madgik.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service(Constants.VISUALIZATION_TOPIC_DOCS_PER_JOURNAL_SERVICE)
public class VisualizationTopicDocsPerJournalService {

    @Autowired
    private VisualizationTopicDocsPerJournalRepository repo;

    @Autowired
    private MapperService mapperService;

    public List<VisualizationTopicDocsPerJournalDto> getAllVisualizationTopicDocsPerJournal() {
        return mapperService.convertVisualizationTopicDocsPerJournalEntityListToDto(repo.findAll());
    }

    public Page<VisualizationTopicDocsPerJournalDto> getVisualizationTopicDocsPerJournal(VisualizationTopicDocsPerJournalRequest request) {
        Pageable pageable = PageRequest.of(request.getPageNumber(), request.getPageSize(), Sort.by("count").descending());
        Page<VisualizationTopicDocsPerJournal> entityPage = repo.findAllByTopicIdAndExperimentId(request.getTopicId(), request.getExperimentId(), pageable);
        return entityPage.map(entity -> mapperService.convertVisualizationTopicDocsPerJournalEntityToDto(entity));
    }
}

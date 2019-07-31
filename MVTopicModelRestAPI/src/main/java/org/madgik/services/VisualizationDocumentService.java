package org.madgik.services;

import org.madgik.dtos.VisualizationDocumentDto;
import org.madgik.persistence.entities.VisualizationDocument;
import org.madgik.persistence.repositories.VisualizationDocumentRepository;
import org.madgik.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service(Constants.VISUALIZATION_DOCUMENT_SERVICE)
public class VisualizationDocumentService {

    @Autowired
    private MapperService mapperService;

    @Autowired
    private VisualizationDocumentRepository repo;

    public Page<VisualizationDocumentDto> getVisualizationDocumentsInIds(List<String> ids, int offset, int limit) {
        Pageable pageable = PageRequest.of(offset, limit);
        Page<VisualizationDocument> documentPages = repo.findAllByIdIn(ids, pageable);
        return documentPages.map(entity ->
                mapperService.getDto(entity, VisualizationDocumentDto.class));
    }
}

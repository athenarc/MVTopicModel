package org.madgik.services;

import org.apache.commons.collections4.CollectionUtils;
import org.madgik.dtos.VisualizationDocumentDto;
import org.madgik.persistence.repositories.VisualizationDocumentRepository;
import org.madgik.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
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

    public List<VisualizationDocumentDto> getVisualizationDocumentsInIds(List<String> ids, int offset, int limit) {
        Pageable pageable = PageRequest.of(offset, limit);
        return mapperService.getDtos(repo.findAllByIdIn(ids, pageable),
                VisualizationDocumentDto.class);
    }
}

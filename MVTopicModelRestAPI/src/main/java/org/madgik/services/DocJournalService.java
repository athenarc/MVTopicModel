package org.madgik.services;

import org.madgik.dtos.DocJournalDto;
import org.madgik.persistence.entities.DocJournal;
import org.madgik.persistence.repositories.DocJournalRepository;
import org.madgik.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service(Constants.DOC_JOURNAL_SERVICE)
public class DocJournalService {


    @Autowired
    private DocJournalRepository repo;

    @Autowired
    private MapperService mapperService;

    public List<DocJournalDto> getDocsWithJournal(List<String> docIds, String journalTitle) {
        List<DocJournal> res = repo.filterToJournal(docIds, journalTitle);
        return mapperService.convertDocJournalEntitesToDto(res);
    }
}

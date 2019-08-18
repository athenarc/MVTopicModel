package org.madgik.persistence.repositories;

import org.madgik.persistence.compositeIds.DocJournalId;
import org.madgik.persistence.entities.DocJournal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocJournalRepository extends JpaRepository<DocJournal, DocJournalId>{
    @Query("select dj from DocJournal dj where dj.docJournalId.docId in :docIds and dj.title = :journalTitle")
    List<DocJournal> filterToJournal(@Param("docIds") List<String> docIds, @Param("journalTitle") String journalTitle);
}

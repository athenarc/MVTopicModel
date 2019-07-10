 select document.id as id, pubyear, document.abstract, abstract_pmc, other_abstract_pmc, doctype,
project.acronym as projectAcronym, project.title as project, journal.title as journal
from document
-- join journal and project id mapping tables
left join doc_journal on doc_journal.docid = document.id
left join doc_project on doc_project.docid = document.id
-- join journal and project information tables
left join project on project.id = projectid left join journal on journalid = journal.id
where document.id

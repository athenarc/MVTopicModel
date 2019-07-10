select
TopicId as topicid,
Document.id as documentid,
round(Weight,2) as weight
-- Title  as title,
-- batchid,
--Abstract,
 --journalISSN   ,
   --DOI     ,
  -- repository
  from doc_topic
Inner Join Document on Document.id=Doc_Topic.DocId
where ExperimentId = EXPERIMENT_IDENTIFIER
and weight > FILTERING_WEIGHT
--And topicId=0
Order by TopicId, Weight desc

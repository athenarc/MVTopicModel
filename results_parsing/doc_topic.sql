copy
(

select 
TopicId, 
Document.id, 
round(Weight,2), 
Title  ,
batchid,
--Abstract,
 --journalISSN   ,
   --DOI     ,
  repository 
  from doc_topic
Inner Join Document on Document.id=Doc_Topic.DocId 
where ExperimentId = 'JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay'
and weight > 0.6	
--And topicId=0 
Order by TopicId, Weight desc
)
to '/postgresout/doc_topic.csv' with csv delimiter ',' header;



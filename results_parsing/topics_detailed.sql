copy
(
SELECT TopicDescription.TopicId AS TopicId,
           Round(TopicDetails.Weight,5) AS TopicWeight,
           TopicDetails.TotalTokens,
           TopicDescription.ItemType,
           CASE TopicDescription.Itemtype
           WHEN - 2 THEN 'KeyPhrase'
           WHEN - 1 THEN 'Phrase'
           WHEN 0 THEN 'Word'
           -- WHEN 1 THEN 'Keyword'
           WHEN 1 THEN 'MeshTerm'  
           WHEN 2 THEN 'DBpedia'                  
           --WHEN 4 THEN 'Citation'            
           END AS Modality,          

           CASE TopicDescription.Itemtype
           WHEN - 2 THEN Item
           WHEN - 1 THEN Item
           WHEN 0 THEN Item
           -- WHEN 1 THEN Item
           WHEN 1 THEN Item
           WHEN 2 THEN substring(item,29,length(item))                  
           --WHEN 4 THEN Publication.title            
           END AS concept,  
           Item,
           Counts,
           DiscrWeight,
           WeightedCounts,          
           --document.title AS Citation,
           DBPediaResourceDetails.Label AS DBpedia_Label,          
 DBPediaResourceDetails.icd10 AS DBpedia_icd10,          
     DBPediaResourceDetails.mesh||':'||DBPediaResourceDetails.meshId AS DBpedia_MeSH,          
 DBPediaResourceDetails.Type as DBpedia_type,
           TopicDescription.ExperimentId AS ExperimentId
      FROM (
               SELECT TopicId,
                      TopicAnalysis.Item,
                      TopicAnalysis.ItemType,
                      TopicsCnt,
                      TopicAnalysis.Counts * (CASE TopicAnalysis.itemType WHEN - 1 THEN Experiment.PhraseBoost ELSE 1 END) AS counts,
                      CASE TopicAnalysis.itemType WHEN - 1 THEN Experiment.PhraseBoost ELSE 1 END AS TypeWeight,
                      CAST (PowSum / (TotalSum * TotalSum) AS REAL) AS DiscrWeight,
                      round(CASE TopicAnalysis.itemType WHEN - 1 THEN Experiment.PhraseBoost ELSE 1 END * CAST(PowSum / (TotalSum * TotalSum) * TopicAnalysis.Counts AS REAL)) AS WeightedCounts,
                      TopicAnalysis.ExperimentId
                 FROM TopicAnalysis
                      INNER JOIN
                      Experiment ON TopicAnalysis.ExperimentId = Experiment.ExperimentId AND Experiment.ExperimentId =  'JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay'
                      INNER JOIN
                      (
                          SELECT ExperimentId,
                                 Item,
                                 ItemType,
                                 CAST (count(*) AS REAL)  AS TopicsCnt,
                                 sum(Counts * CAST (Counts AS REAL))  AS PowSum,
                                 CAST (sum(Counts) AS REAL) AS TotalSum
                            FROM TopicAnalysis
                           WHERE counts > 3
                           GROUP BY ExperimentId,
                                    ItemType,
                                    Item
                      )
                      AS FreqItems ON FreqItems.ExperimentId = TopicAnalysis.ExperimentId AND
                                      FreqItems.ItemType = TopicAnalysis.itemType AND
                                      FreqItems.Item = TopicAnalysis.item
                WHERE counts > 3
               
           )
           AS TopicDescription
           LEFT OUTER JOIN
           TopicDetails ON TopicDetails.TopicId = TopicDescription.TopicId AND
                           TopicDetails.ExperimentId = TopicDescription.ExperimentId AND
                           TopicDetails.Itemtype = 0
           
           LEFT OUTER JOIN
          (select DBPediaResource.id, DBPediaResource.label, DBPediaResource.icd10, DBPediaResource.meshId, DBPediaResource.mesh, dbpediaresourcetype.typelabel as type
  from DBPediaResource
      left join dbpediaresourcetype on dbpediaresourcetype.resourceid = id
 ) DBPediaResourceDetails  ON DBPediaResourceDetails.Id = Item AND
                     TopicDescription.ItemType = 2
           
           --LEFT OUTER JOIN
           --document ON document.Id = Item AND
             --          TopicDescription.ItemType = 4
     WHERE Counts > 20  
     -- and TopicDescription.topicid=0  
     Order By TopicDescription.ExperimentId , TopicDescription.TopicId, TopicDescription.ItemType, WeightedCounts DESC
)
to '/postgresout/topics_detailed.csv' with csv delimiter ',' header;

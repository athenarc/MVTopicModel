select  avg(normDiscrWeight) as normDiscrWeight, avg(discrWeight) as discrWeight, avg(coherence) as coherence, min(coherence) as Mincoherence, max(coherence) as Maxcoherence, 
max(coherence) - min(coherence) as VarCoherence, avg(TopicExclusivityOnPubs) as TopicExclusivityOnPubs
from (
select 
TopicDetails.TopicId,
VisibilityIndex, 
round(TopicDetails.Weight,6) as TopicWeight ,
TopicDetails.TotalTokens,
round(discrWeightFlat,5) as DiscrWeightFlat,
round(discrWeight.discrWeight,5) as DiscrWeight,
round(normDiscrWeight,5) as normDiscrWeight, --discrWeightDivLogDiffW
round(coh1.Score,2) as Coherence
,round(topicDiscr.TopicDiscrWeight,6) as TopicExclusivityOnPubs
,topic.Title
,topic.ExperimentId

from TopicDetails
INNER Join  Topic on TopicDetails.TopicId = topic.Id and TopicDetails.ExperimentId=topic.ExperimentId and TopicDetails.ItemType=0
INNER Join 
(select EntityId, Score,ExperimentId from ExpDiagnostics
where entityType=1
and scorename = 'coherence' ) coh1 
on coh1.EntityId='Topic '||TopicDetails.TopicId and TopicDetails.ExperimentId=coh1.ExperimentId 
INNER JOIN 
--(Select a.EntityId, (1 / a.Score) as discrWeightFlat , (1 / a.Score) * b.Score as Coherence2,  b.Score as TotalTokens2, a.ExperimentId
--from 
(select  EntityId, 1/Score as discrWeightFlat,ExperimentId from ExpDiagnostics
where  entityType=1
and scorename in ( 'eff_num_words')) discrWeightFlat 
--INNER JOIN 
--(select EntityId, Score,ExperimentId from ExpDiagnostics
--where  entityType=1
--and scorename in ('tokens')) b on a.EntityId=b.EntityId and a.ExperimentId = b.ExperimentId
--) coh2 
on discrWeightFlat.EntityId=coh1.EntityId and coh1.ExperimentId=discrWeightFlat.ExperimentId 
INNER JOIN 
(select EntityId, Score as discrWeight,ExperimentId from ExpDiagnostics
where entityType=1
and scorename = 'discrWeight') discrWeight
on discrWeight.EntityId=coh1.EntityId and coh1.ExperimentId=discrWeight.ExperimentId 
INNER JOIN 
(select EntityId, Score as normDiscrWeight,ExperimentId from ExpDiagnostics
where entityType=1
and scorename='normDiscrWeight') normDiscrWeight 
on normDiscrWeight.EntityId=coh1.EntityId and coh1.ExperimentId=normDiscrWeight.ExperimentId 
INNER JOIN 
(
select TopicId, PowSum/(TotalSum*TotalSum) AS TopicDiscrWeight , ExperimentId
  from 
  (select TopicId,  sum(NormWeight*NormWeight) as PowSum, sum(NormWeight) as TotalSum , experimentId
  from
  (
SELECT 
           doc_topic.TopicId,
           doc_topic.docId,
           sum(doc_topic.weight) / TopicSum.TopicSumWeight AS NormWeight,
           doc_topic.ExperimentId
      FROM doc_topic
           INNER JOIN (select doc_topic.TopicId, experimentId,  
           sum(weight) AS TopicSumWeight from doc_topic 
           where doc_topic.ExperimentId='JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay' 
           -- where doc_topic.ExperimentId='PubMed_400T_100IT_7000CHRs_4M_Lmt_10000OneWay' 
           Group By ExperimentId, TopicID) as TopicSum 
           On TopicSum.TopicId = doc_topic.TopicId and TopicSum.ExperimentId = doc_topic.ExperimentId
           -- where doc_topic.ExperimentId='ACM_200T_500IT_5000CHRs_100B_5M_cos'
           --where doc_topic.ExperimentId='ACM_400T_750IT_4000CHRs_300_25PRN100B_5M_4TH_cosPPR_' 
           Group BY doc_topic.TopicId,
              TopicSum.TopicSumWeight,
              doc_topic.docId,
              doc_topic.ExperimentId
              ) a
              Group By TopicId, ExperimentID
              ) b 
              
--where ExperimentId='ACM_400T_750IT_4000CHRs_300_25PRN100B_5M_4TH_cosPPR_' 

  ) topicDiscr on topicDiscr.TopicId = TopicDetails.TopicId and TopicDetails.ExperimentId=topicDiscr.ExperimentId 
  
where coh1.ExperimentId='JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay'  
-- where coh1.ExperimentId='PubMed_400T_100IT_7000CHRs_4M_Lmt_10000OneWay'  
--and round(coh1.Score,2)<-150
order by TopicDetails.TopicId
) as c
;

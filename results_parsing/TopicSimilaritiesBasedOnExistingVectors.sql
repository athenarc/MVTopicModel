-- generate topicvector based on wordvectors 
Insert into TopicVector (TopicId,ColumnId,Weight,ExperimentId) 
select TopicVector.topicId, TopicVector.columnId, SUM(TopicVector.topicWordWeight), TopicVector.experimentId from 
(
select topicAnalysis.topicId, item, counts, columnId, weight 
, TotalTopicCnt, Cast(counts as Float)/TotalTopicCnt * weight as topicWordWeight, topicAnalysis.ExperimentId
from topicAnalysis 
Inner join WordVector on word=item and  itemType=0 and modality=0
Inner join (select topicId, sum(Counts) as TotalTopicCnt, experimentId from TopicAnalysis where itemType=0 group by topicId, ExperimentId) totalTopicCnts 
on totalTopicCnts.TopicId = TopicAnalysis.TopicId and totalTopicCnts.ExperimentId=topicAnalysis.ExperimentId
where topicAnalysis.experimentId='PubMed_500T_550IT_7000CHRs_4M_OneWay'
								  
	--'HEALTHTenderPM_500T_600IT_7000CHRs_10.0 3.0E-4_2.0E-4PRN50B_4M_4TH_cosOneWay' 

) TopicVector
group by TopicVector.topicId, columnId, TopicVector.experimentId


--calc cosine similarity based mapping between different experiments 
select vid1, exp1topic.Title as exp1Title, vid2, exp2topic.Title as exp2Title,  cosine, exp1, exp2
from 
(
select v1.topicid as vid1,v2.topicid as vid2, v1.experimentId as exp1, v2.experimentId as exp2,
round(SUM(v1.weight*v2.weight)/(SQRT(SUM(v1.weight*v1.weight))*SQRT(SUM(v2.weight*v2.weight))),2) as cosine
from topicvector v1 
inner join topicvector v2 on v1.columnid=v2.columnid and v1.topicid<>v2.topicid
where v1.topicid<v2.topicid
group by v1.topicid,v2.topicid , v1.experimentId, v2.experimentId
Order by v1.topicid, cosine desc
	) a
	inner join topic exp1topic on a.exp1 = exp1topic.experimentId and exp1topic.Id = vid1
	inner join topic exp2topic on a.exp2 = exp2topic.experimentId and exp2topic.Id = vid2
	where cosine > 0.9 and exp1<> exp2 
	and (exp1='PubMed_500T_550IT_7000CHRs_4M_OneWay' OR exp1='HEALTHTenderPM_500T_600IT_7000CHRs_10.0 3.0E-4_2.0E-4PRN50B_4M_4TH_cosOneWay') 
	and (exp2='PubMed_500T_550IT_7000CHRs_4M_OneWay' OR exp2='HEALTHTenderPM_500T_600IT_7000CHRs_10.0 3.0E-4_2.0E-4PRN50B_4M_4TH_cosOneWay')
	
	
	
endpoint="localhost:8080/mvtm_api"

echo "Hello'ing"
echo "${endpoint}/hello"
curl "${endpoint}/hello"

echo 
# test topic Documents
echo "Testing topic documents fetching"
echo
echo "${endpoint}/topicDocuments?topicId=20&experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&pageNumber=0&pageSize=15&sortOrder=sort&filter=filt&maxNumDocuments=10"
# curl "${endpoint}/topicDocuments?topicId=20&experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&pageNumber=0&pageSize=15&sortOrder=sort&filter=filt&maxNumDocuments=10&journal=" > topicdocs.json && cat topicdocs.json | jq .

# test topic Documents for journal
echo "Testing topic documents fetching in a journal"
echo
echo "${endpoint}/topicDocuments?topicId=20&experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&pageNumber=0&pageSize=55&sortOrder=sort&filter=filt&maxNumDocuments=10&journal=Nature"
# curl "${endpoint}/topicDocuments?topicId=20&experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&pageNumber=0&pageSize=55&sortOrder=sort&filter=filt&maxNumDocuments=10&journal=Nature" > topic_journal_docs.json && cat topic_journal_docs.json | jq .


echo
echo
echo "Testing topic docs per journal fetching"
echo
echo "${endpoint}/topicDocsPerJournal?topicId=20&experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&pageNumber=0&pageSize=15&sortOrder=sort&filter=filt"
curl "${endpoint}/topicDocsPerJournal?topicId=20&experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&pageNumber=0&pageSize=15&sortOrder=sort&filter=filt" | jq .


echo "Testing curation details fetching"
echo "${endpoint}/curationDetails?experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay"
curl "${endpoint}/curationDetails?experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay" | jq .


echo "Testing topic similarity fetching"
echo "${endpoint}/topicSimilarity?experimentId1=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&experimentId2="
curl "${endpoint}/topicSimilarity?experimentId1=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&experimentId2=" | jq .

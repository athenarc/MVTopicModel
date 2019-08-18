endpoint="localhost:8080/mvtm_api"
endpoint="localhost:8080/MVTopicModelRestAPI_war"

# test topic Documents
echo "Testing topic documents fetching"
echo
echo "${endpoint}/topicDocuments?topicId=20&experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&pageNumber=0&pageSize=15&sortOrder=sort&filter=filt&maxNumDocuments=10"
curl "${endpoint}/topicDocuments?topicId=20&experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&pageNumber=0&pageSize=15&sortOrder=sort&filter=filt&maxNumDocuments=10&journal=" > topicdocs.json && cat topicdocs.json | jq .

# test topic Documents for journal
echo "Testing topic documents fetching in a journal"
echo
echo "${endpoint}/topicDocuments?topicId=20&experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&pageNumber=0&pageSize=55&sortOrder=sort&filter=filt&maxNumDocuments=10&journal=Nature"
curl "${endpoint}/topicDocuments?topicId=20&experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&pageNumber=0&pageSize=55&sortOrder=sort&filter=filt&maxNumDocuments=10&journal=Nature" > topic_journal_docs.json && cat topic_journal_docs.json | jq .


echo
echo
echo "Testing topic docs per journal fetching"
echo
echo "${endpoint}/topicDocsPerJournal?topicId=20&experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&pageNumber=0&pageSize=15&sortOrder=sort&filter=filt"
curl "${endpoint}/topicDocsPerJournal?topicId=20&experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&pageNumber=0&pageSize=15&sortOrder=sort&filter=filt" | jq .

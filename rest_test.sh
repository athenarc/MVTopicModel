endpoint="localhost:8080/mvtm_api"
# test topic Documents
echo "Testing topic documents fetching"
echo "${endpoint}/topicDocuments?topicId=20&experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&pageNumber=0&pageSize=15&sortOrder=sort&filter=filt&maxNumDocuments=10"
echo
echo
curl "${endpoint}/topicDocuments?topicId=20&experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&pageNumber=0&pageSize=15&sortOrder=sort&filter=filt&maxNumDocuments=10"

echo
echo
echo "Testing topic docs per journal fetching"
echo "${endpoint}/topicDocsPerJournal?topicId=20&experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&pageNumber=0&pageSize=15&sortOrder=sort&filter=filt"
echo
echo
curl "${endpoint}/topicDocsPerJournal?topicId=20&experimentId=JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay&pageNumber=0&pageSize=15&sortOrder=sort&filter=filt"


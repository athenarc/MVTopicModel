package org.madgik.rest.controllers;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.madgik.dtos.*;
import org.madgik.io.SQLTMDataSource;
import org.madgik.persistence.entities.TopicSimilarity;
import org.madgik.rest.requests.DocumentInfoRequest;
import org.madgik.rest.requests.PageableRequest;
import org.madgik.rest.requests.TopicCurationRequest;
import org.madgik.rest.requests.VisualizationTopicDocsPerJournalRequest;
import org.madgik.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class TopicModelController {

    private static final Logger logger = Logger.getLogger(TopicModelController.class);

    @Autowired
    private VisualizationDocumentService visualizationDocumentService;

    @Autowired
    private TopicCurationService topicCurationService;

    @Autowired
    private CurationDetailsService curationDetailsService;

    @Autowired
    private TopicSimilarityService topicSimilarityService;

    @Autowired
    private TopicCategorySimilaritiesService topicCategorySimilaritiesService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private VisualizationExperimentService visualizationExperimentService;

    @Autowired
    private DocTopicService docTopicService;

    @Autowired
    private DocJournalService docJournalService;

    @Autowired
    private VisualizationTopicDocsPerJournalService visualizationTopicDocsPerJournalService;

//    @Value("${serialization.path}")
//    private String serializationBasePath;
//
//    @Value("${sql.connection.string}")
//    private String sqlConnectionString;
//
//    @Value("${sql.topicinfo.querypath}")
//    private String sqlTopicInfoQueryPath;
//
//    @Value("${sql.documenttopicinfo.querypath}")
//    private String sqlDocumentTopicInfoQueryPath;
//
//    @Value("${sql.documentvisualizationinfo.querypath}")
//    private String sqlDocumentVisualizationInfoQueryPath;

    @RequestMapping(value = "/predict", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> predict() {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping("/hello")
    public String welcome() {
        return "Up and running.";
    }

   private String getSerialized(String path, Boolean refresh){
       if (! refresh) {
           File serfile = new File(path);
           if (serfile.exists() && !serfile.isDirectory()) {
               // get cached results
               try {
                   return new String(Files.readAllBytes(Paths.get(path)));
               } catch (IOException e) {
                   logger.error(e.getMessage());
               }
           }
       }
       return null;
   }

    private void serialize(String path, String obj){
        // get cached results
        try {
            FileWriter fw = new FileWriter(path);
            new Gson().toJson(obj, fw);
            fw.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }


    @RequestMapping(value = "/curationCategoriesPerTopic", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public CurationTopicsCategories getCurationCategoriesPerTopic(@RequestParam("experimentId") String experimentId){
        if (experimentId == null || experimentId.isEmpty())
            experimentId = "JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay";
        return curationDetailsService.mapToCategoriesPerTopic(curationDetailsService.findByExperimentId(experimentId));
    }



    @RequestMapping(value = "/curationDetails", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CurationDetailsDto> getCurationDetails(@RequestParam("experimentId") String experimentId){
        if (experimentId == null || experimentId.isEmpty())
            experimentId = "JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay";
        return curationDetailsService.findByExperimentId(experimentId);
    }

    @RequestMapping(value = "/topicSimilarity", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<TopicSimilarityDto> getTopicSimilarity(@RequestParam("experimentId1") String experimentId1,
                                                       @RequestParam("experimentId2") String experimentId2,
                                                       @RequestParam("topics") String topics){
        List<TopicSimilarityDto> res = null;
        if (experimentId1 == null || experimentId1.isEmpty())
            experimentId1 = "JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay";
        if (experimentId2 == null || experimentId2.isEmpty())
            experimentId2 = "HEALTHTenderPM_500T_600IT_7000CHRs_10.0 3.0E-4_2.0E-4PRN50B_4M_4TH_cosOneWay";
        ArrayList<Integer> topicsList = new ArrayList<>();
        if (topics != null && !  topics.isEmpty()){
            for (String topic : topics.split(",")) {
                try {
                    topicsList.add(Integer.parseInt(topic));
                }catch(NumberFormatException ex){

                    logger.info(String.format("Cannot parse topicid: %s, which came from topic list %s",
                            topic, topics));
                    return null;
                }
            }
            res =  topicSimilarityService.findByExperimentIdsAndTopicIds(experimentId1, experimentId2, topicsList);
            logger.info(String.format("Returning non-all topics, eg %d items.", res.size()));
        }
        else {
            // return all topics
            res = topicSimilarityService.findByExperimentIds(experimentId1, experimentId2);
            logger.info(String.format("Returning all topics, eg %d items.", res.size()));
        }

        return res;
    }


    @RequestMapping(value = "/categorySimilarity", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<List<Integer>> getCategorySimilarity(@RequestParam("experimentId") String experimentId,
                                                     @RequestParam("assignments") List<Integer> assignments,
                                                    @RequestParam("threshold") Double threshold){
        if (experimentId == null || experimentId.isEmpty())
            experimentId = "JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay";

        List<TopicSimilarityDto> res = topicSimilarityService.findByExperimentIds(experimentId, experimentId);
        logger.info(String.format("Returning all topics, eg %d items.", res.size()));
        return topicCategorySimilaritiesService.calculateTopicCategorySimilarity(res, assignments, threshold);
    }






   @RequestMapping(value = "/topicDocuments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
   @ResponseBody
   public Page<VisualizationDocumentDto> getDocumentsPerTopic(@RequestParam("filter") String filter,
                                                 @RequestParam("sortOrder") String sortOrder,
                                                 @RequestParam("journal") String journal,
                                                 @RequestParam("pageNumber") Integer pageNumber,
                                                 @RequestParam("pageSize") Integer pageSize,
                                                 @RequestParam("topicId") Integer topicId,
                                                 @RequestParam("maxNumDocuments") Integer maxNumDocuments,
                                                 @RequestParam("experimentId") String experimentId){

       if (experimentId == null || experimentId.isEmpty()) experimentId = "JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay";
       if (sortOrder == null || sortOrder.isEmpty()) sortOrder = "asc";
       if (filter == null || filter.isEmpty()) filter = "";
       if (pageSize == null) pageSize = 10;
       if (pageNumber == null) pageNumber = 0;

       // limit to topic and experiment id
       List<DocTopicDto> docTopicDtos = docTopicService.getDocTopicsByTopicIdAndExperimentId(topicId, experimentId);

       logger.info(String.format("Got %d documents for topic id %d and experiment id %s.",
               docTopicDtos.size(), topicId, experimentId));

       List<String> docIds = docTopicDtos.stream().map(DocTopicDto::getDocId).collect(Collectors.toList());

       // limit to specified journal constraint, if present
       if (journal != null && ! journal.isEmpty()){

           List<String> jour_docids = new ArrayList<>();
           // batch size for SQL query
           int batchSize = 1000;
           int batchIndex = 0;
           while (batchIndex * batchSize < docIds.size()){
               int startIndex = batchIndex*batchSize;
               List<DocJournalDto> djs = docJournalService.getDocsWithJournal(docIds.subList(startIndex, startIndex + batchSize), journal);

               djs.forEach(dj->{
                   jour_docids.add(dj.getDocid()) ;
               });
               batchIndex ++;
               logger.info(String.format("Got %d journal %s items from %d-sized batch # %d", jour_docids.size(), journal, batchSize, batchIndex));
               if (maxNumDocuments != null && maxNumDocuments <= jour_docids.size()){
                   logger.info(String.format("Stopping batch-queries since max num documents (%d) was satisfied.", maxNumDocuments));
                   break;
               }
           }
           docIds = jour_docids;
           logger.info(String.format("Limited to %d docs, since input num constraint is: %d", docIds.size(), maxNumDocuments));
       }


       if (maxNumDocuments != null){
           docIds = docIds.subList(0, maxNumDocuments);
           logger.info(String.format("Limited to %d by request param.", docIds.size()));
       }



       DocumentInfoRequest request = new DocumentInfoRequest(filter, sortOrder, pageNumber, pageSize, docIds);
       return getDocumentInformation(request);
   }


    @RequestMapping(value="/documentinfo", method=RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Page<VisualizationDocumentDto> getDocumentInformation(@RequestBody DocumentInfoRequest  request) {
        if(request.getNumChars() == null) request.setNumChars(100);
        Page<VisualizationDocumentDto> visualizationDocumentDtos =visualizationDocumentService.getVisualizationDocumentsInIds(
                request.getDocumentIds(), request.getPageNumber(), request.getPageSize());
        return visualizationDocumentDtos;
    }

    @RequestMapping(value = "/topicclustering", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TopicClusteringDto topicClustering(@RequestParam("clusterId") Integer clusterId) {
        TopicClusteringDto dto = new TopicClusteringDto();
        dto.setClusterId(clusterId);
        dto.setClusterMember(1);
        return dto;
    }

    @RequestMapping(value = "/topicCurations", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Page<TopicCurationDto> getTopicsCuration(@RequestBody PageableRequest pageableRequest) {
        return topicCurationService.getAllTopicCurations(pageableRequest);
    }

    @RequestMapping(value = "/topiccuration", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TopicCurationDto getTopicCurationByCompositeId(@RequestParam("topicId") Integer topicId,
                                                          @RequestParam("experimentId") String experimentId) {
        TopicCurationDto res = topicCurationService.getTopicCurationByTopicIdAndExperimentId(topicId, experimentId);
        if (res == null) return new TopicCurationDto();
        return res;
    }

    @RequestMapping(value = "/topiccuration", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TopicCurationDto createTopicCuration(@RequestBody TopicCurationRequest topicCurationRequest) {
        TopicDto topicDto = topicService.getTopicByCompositeId(topicCurationRequest.getTopicId(), topicCurationRequest.getExperimentId());
        if (topicDto != null) {
            TopicCurationDto topicCurationDto = new TopicCurationDto();
            topicCurationDto.setTopic(topicDto);
            topicCurationDto.setTopicId(topicCurationRequest.getTopicId());
            topicCurationDto.setExperimentId(topicCurationRequest.getExperimentId());
            topicCurationDto.setCuratedDescription(topicCurationRequest.getCuratedDescription());
            return topicCurationService.createTopicCuration(topicCurationDto);
        }
        return new TopicCurationDto();
    }

    @RequestMapping(value = "/experiment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Page<VisualizationExperimentDto> getAllVisualizationExperiments(@RequestParam("filter") String filter,
                                                                           @RequestParam("sortOrder") String sortOrder,
                                                                           @RequestParam("pageNumber") Integer pageNumber,
                                                                           @RequestParam("pageSize") Integer pageSize) {
        PageableRequest request = new PageableRequest(filter, sortOrder, pageNumber, pageSize);
        return visualizationExperimentService.getAllVisualizationExperiments(request);
    }

    @RequestMapping(value = "/topicDocsPerJournal", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Page<VisualizationTopicDocsPerJournalDto> getTopicDocsPerJournal(@RequestParam("filter") String filter,
                                                                            @RequestParam("sortOrder") String sortOrder,
                                                                            @RequestParam("pageNumber") Integer pageNumber,
                                                                            @RequestParam("pageSize") Integer pageSize,
                                                                            @RequestParam("topicId") Integer topicId,
                                                                            @RequestParam("experimentId") String experimentId) {
        VisualizationTopicDocsPerJournalRequest request = new VisualizationTopicDocsPerJournalRequest(filter, sortOrder, pageNumber, pageSize, topicId, experimentId);
        return visualizationTopicDocsPerJournalService.getVisualizationTopicDocsPerJournal(request);
    }

    public static void main(String[] args) {

        TopicModelController tmc = new TopicModelController();
        tmc.getDocumentsPerTopic(null, null, "",0,10,0,
                10,"JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay");

    }
}


package org.madgik.rest;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.madgik.dtos.*;
import org.madgik.io.SQLTMDataSource;
import org.madgik.services.TopicCurationService;
import org.madgik.services.TopicService;
import org.madgik.services.VisualizationDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

@RestController
public class TopicModelController {
    private static final Logger logger = Logger.getLogger("REST");

    @Autowired
    private VisualizationDocumentService visualizationDocumentService;

    @Autowired
    private TopicCurationService topicCurationService;

    @Autowired
    private TopicService topicService;

    @Value("${serialization.path}")
    private String serializationBasePath;
    @Value("${sql.connection.string}")
    private String sqlConnectionString;

    @Value("${sql.topicinfo.querypath}")
    private String sqlTopicInfoQueryPath;
    @Value("${sql.documenttopicinfo.querypath}")
    private String sqlDocumentTopicInfoQueryPath;
    @Value("${sql.documentvisualizationinfo.querypath}")
    private String sqlDocumentVisualizationInfoQueryPath;

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
               } catch (FileNotFoundException e) {
                   logger.error(e.getMessage());
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

   @RequestMapping("/topicdocuments")
   public String getDocumentsPerTopic(String expid, Double weight_threshold, Boolean refresh){

       if (expid == null || expid.isEmpty()) expid = "JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay";
       if (weight_threshold == null) weight_threshold = 0.6d;
       if (refresh == null) refresh = false;

       String serialization_path = serializationBasePath + "docs_per_topic_exp_" + expid + "_probthresh_" + weight_threshold;

       String output = getSerialized(serialization_path, refresh);
       if (output != null) return output;

       // resulting container: topic -> document id -> weight
       Map<Integer, Map<String, Double>> res = new HashMap<>();

       try {
           String query = new String(Files.readAllBytes(Paths.get(sqlDocumentTopicInfoQueryPath)));
           query = query.replaceAll("EXPERIMENT_IDENTIFIER", "'" + expid + "'");
           query = query.replaceAll("FILTERING_WEIGHT", Double.toString(weight_threshold));
           SQLTMDataSource ds = new SQLTMDataSource(sqlConnectionString);

           res = ds.getDocumentTopicWeights(query, weight_threshold);
       } catch (IOException e) {
           logger.error(e.getMessage());
       } catch (SQLException e) {
           logger.error(e.getMessage());
       }
       output = new Gson().toJson(res);
       // serialize
       serialize(serialization_path, output);
       return output;
   }


    @RequestMapping(value="/documentinfo", method=RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Page<VisualizationDocumentDto> getDocumentInformation(@RequestBody DocumentInfoRequest request) {
        if(request.getNumChars() == null) request.setNumChars(100);
        Page<VisualizationDocumentDto> visualizationDocumentDtos =visualizationDocumentService.getVisualizationDocumentsInIds(request.getDocumentIds(), request.getOffset(), request.getLimit());
        visualizationDocumentDtos.forEach(doc -> {
            if (StringUtils.isNotBlank(doc.getAbstractField())) {
                doc.setAbstractField(doc.getAbstractField().substring(request.getNumChars()));
            }

            if (StringUtils.isNotBlank(doc.getAbstractPmc())) {
                doc.setAbstractPmc(doc.getAbstractPmc().substring(request.getNumChars()));
            }

            if (StringUtils.isNotBlank(doc.getOtherAbstractPmc())) {
                doc.setOtherAbstractPmc(doc.getAbstractPmc().substring(request.getNumChars()));
            }
        });
        return visualizationDocumentDtos;
    }

    @RequestMapping(value = "/topictokens", method = RequestMethod.GET)
    public String getTopicInformation(String expid, Double prob_threshold, String weight_type, Boolean refresh) {

        if (expid == null || expid.isEmpty()) expid = "JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay";
        if (prob_threshold == null) prob_threshold = 0.05d;
        if (refresh == null) refresh = false;
        if (weight_type == null) weight_type = "across_topics";
        if (!(weight_type.equals("across_topics") || weight_type.equals("within_topic"))){
            logger.error("Undefined weight type: " + weight_type);
            return null;
        }

        String serialization_path = serializationBasePath + "topic_tokens_exp_" + expid + "_probthresh_" + prob_threshold;

        String output = getSerialized(serialization_path, refresh);
        if (output != null) return output;

        // resulting container: topic -> modality -> token -> weight
        Map<Integer, Map<String, Map<String, Double>>> res = new HashMap<>();

        try {
            String query = new String(Files.readAllBytes(Paths.get(sqlTopicInfoQueryPath)));
            query = query.replaceAll("EXPERIMENT_IDENTIFIER", "'" + expid + "'");
            SQLTMDataSource ds = new SQLTMDataSource(sqlConnectionString);
            res = ds.getTopicInformation(query, prob_threshold, weight_type, expid);
        } catch (IOException | SQLException e) {
            logger.error(e.getMessage());
        }
        output = new Gson().toJson(res);
        // serialize
        serialize(serialization_path, output);
        return output;
    }

    @RequestMapping(value = "/topicclustering", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TopicClusteringDto topicClustering(@RequestParam("clusterId") Integer clusterId) {
        TopicClusteringDto dto = new TopicClusteringDto();
        dto.setClusterId(clusterId);
        dto.setClusterMember(1);
        return dto;
    }


    @GetMapping(value = "/topicCurations", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<TopicCurationDto> getTopicsCuration(@RequestParam String experimentId) {

        return topicCurationService.getAllTopicCurations();
    }

    @RequestMapping(value = "/topiccuration", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TopicCurationDto getTopicCurationByCompositeId(@RequestParam("topicId") Integer topicId, @RequestParam("experimentId") String experimentId) {
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

    public static void main(String[] args) {
        List<String> ids = new ArrayList<>();
        ids.add("50|acnbad______::48b13e3bf556d6e858bed820143ef25d");
        ids.add("50|acnbad______::71d981639718b3130ffdf8b29d18792e");
        ids.add("50|base_oa_____::007cdd1252e5f998b25f25c8b6d4453f");
        ids.add("50|base_oa_____::00ecbfbc821a0b359769b1ee2ebb7282");
        ids.add("50|base_oa_____::0190d46e279f9c4e0364be873513a3fe");
        ids.add("50|base_oa_____::02a67ddc101d626df34a5062428dcdf8");
        ids.add("50|base_oa_____::02a87b4d552511958d4a322a713e2816");

        Properties p = new Properties();
        try {
            p.load(new FileReader("/home/nik/athena/code/MVTopicModel/MVTopicModelRestAPI/src/main/resources/application.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        TopicModelController tmc = new TopicModelController();
        tmc.sqlConnectionString = p.getProperty("sql.connection.string");
        tmc.sqlDocumentVisualizationInfoQueryPath = p.getProperty("sql.documentvisualizationinfo.querypath");
        tmc.sqlDocumentTopicInfoQueryPath = p.getProperty("sql.documenttopicinfo.querypath");
        tmc.sqlTopicInfoQueryPath=p.getProperty("sql.topicinfo.querypath");
        tmc.getTopicInformation(null, null, "within_topic", true);
        tmc.getDocumentsPerTopic(null, null,null);
//        tmc.getDocumentInformation(ids,null, 0, 0);
    }
}

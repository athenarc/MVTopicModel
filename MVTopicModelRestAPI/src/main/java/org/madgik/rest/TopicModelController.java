package org.madgik.rest;

import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;
import org.madgik.io.SQLTMDataSource;
import org.madgik.model.LightDocument;
import org.springframework.beans.factory.annotation.Value;
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
    Logger logger = Logger.getLogger("REST");

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

   @RequestMapping("/documentsPerTopic")
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


    @RequestMapping(value="/documentinfo", method=RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public String getDocumentInformation(@RequestBody ArrayList<String> document_ids, Integer numChars) {

        if (document_ids.isEmpty()) return "[]";
        if(numChars == null) numChars=100;

        Gson gson = new Gson();

        //List<String> document_ids = gson.fromJson(new StringReader(docids_str), List.class);
        List<LightDocument> documents = new ArrayList<>();
        SQLTMDataSource ds = new SQLTMDataSource(sqlConnectionString);
        try {

            StringJoiner joiner = new StringJoiner(", ");
            for(String docid: document_ids) joiner.add("'" + docid + "'");
            String inclause = joiner.toString();
            String base_query = new String(Files.readAllBytes(Paths.get(sqlDocumentVisualizationInfoQueryPath)));
            String docQuery = base_query + " in (" + inclause + ")";
            documents = ds.getDocumentVisualizationInformation(docQuery, numChars);

            return gson.toJson(documents);

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "[]";
    }

    @RequestMapping("/topics")
    public String getTopicInformation(String expid, Double prob_threshold, Boolean refresh) {

        if (expid == null || expid.isEmpty()) expid = "JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay";
        if (prob_threshold == null) prob_threshold = 0.05d;
        if (refresh == null) refresh = false;

        String serialization_path = serializationBasePath + "topic_tokens_exp_" + expid + "_probthresh_" + prob_threshold;

        String output = getSerialized(serialization_path, refresh);
        if (output != null) return output;

        // resulting container: topic -> modality -> token -> weight
        Map<Integer, Map<String, Map<String, Double>>> res = new HashMap<>();

        try {
            String query = new String(Files.readAllBytes(Paths.get(sqlTopicInfoQueryPath)));
            query = query.replaceAll("EXPERIMENT_IDENTIFIER", "'" + expid + "'");
            SQLTMDataSource ds = new SQLTMDataSource(sqlConnectionString);
            res = ds.getTopicInformation(query, prob_threshold, expid);
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

    public static void main(String[] args) {
        ArrayList<String> ids = new ArrayList<>();
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
        tmc.getDocumentsPerTopic(null, null,null);
        tmc.getDocumentInformation(ids,null);
    }
}

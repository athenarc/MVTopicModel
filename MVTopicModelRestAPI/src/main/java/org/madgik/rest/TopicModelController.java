package org.madgik.rest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;
import org.madgik.io.SQLTMDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TopicModelController {
    Logger logger = Logger.getLogger("REST");

    private static final String serializationBasePath = "serialized_topic_information";
    @Value("${sql.connection.string}")
    private String sqlConnectionString;

    @Value("${sql.topicinfo.querypath}")
    private String sqlTopicInfoQueryPath;

    @RequestMapping(value = "/predict", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> predict() {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping("/hello")
    public String welcome() {
        return "Up and running.";
    }


    @RequestMapping("/topics")
    public String getTopicInformation(String expid, Double prob_threshold, Boolean refresh) {

        if (expid == null || expid.isEmpty()) expid = "JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay";
        if (prob_threshold == null) prob_threshold = 0.05d;
        if (refresh == null) refresh = false;

        String serialization_path = serializationBasePath + "_exp_" + expid + "_probthresh_" + prob_threshold;

        if (! refresh) {
            File serfile = new File(serialization_path);
            if (serfile.exists() && !serfile.isDirectory()) {
                // get cached results
                try {
                    return new String(Files.readAllBytes(Paths.get(serialization_path)));
                } catch (FileNotFoundException e) {
                    logger.error(e.getMessage());
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }

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
        String jo = new Gson().toJson(res);
        // serialize
        try {
            new Gson().toJson(jo, new FileWriter(serialization_path));
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return jo;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.madgik.dbpediaspotlightclient;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.log4j.Logger;
import org.madgik.config.Config;
import org.madgik.io.TMDataSource;
import org.madgik.io.TMDataSourceFactory;
import org.madgik.io.modality.DBPedia;
import org.madgik.io.modality.Modality;
import org.madgik.io.modality.Text;

/**
 *
 * @author omiros metaxas
 */

public class DBpediaAnnotator {

    public static Logger logger = Logger.getLogger(DBpediaAnnotator.class.getName());
    String spotlightService = "";
    int numOfThreads = 4;
    double confidence = 0.4;
    Config config;

    Map<String, List<DBpediaResource>> allSemanticAnnotations = new HashMap<>();
    Set<DBpediaResource> allResources = new HashSet<>();

    public Map<String, List<DBpediaResource>> getSemanticANnotationMappings() {
        return allSemanticAnnotations;
    }
    public List<Modality> getSemanticAnnotationModalityList() {
        List<Modality> out = new ArrayList<>();
        for(String id : allSemanticAnnotations.keySet()){
            StringBuilder sb = new StringBuilder();
            List<String> resources = new ArrayList<>();
            for(DBpediaResource res : allSemanticAnnotations.get(id)) resources.add(getUsefulResource(res));
            out.add(new DBPedia(id, String.join(":", resources)));
        }
        return out;
    }
    public Set<String> getAllSemanticResourceStrings() {
         Set<String> out = new HashSet<>();
        for(String id : allSemanticAnnotations.keySet()){
            for(DBpediaResource res : allSemanticAnnotations.get(id)) out.add(getUsefulResource(res));
        }
        return out;
    }

    public String getUsefulResource(DBpediaResource e){
        if(config.getSemanticAnnotatorType().equals(DBpediaAnnotator.AnnotatorType.spotlight.name())) return e.getLink().uri;
        if(config.getSemanticAnnotatorType().equals(AnnotatorType.tagMe.name())) return e.getLink().label;
        return null;
    }

    public Set<DBpediaResource> getAllResources() {
        return allResources;
    }


    public DBpediaAnnotator(String conf_path) {
       config = new Config(conf_path);
    }
    public DBpediaAnnotator(Config config) {
        this.config = config;
    }
    public enum AnnotatorType {
        spotlight,
        tagMe
    }

    List<String> uriInputs;
    public void setSemanticDetailExtractionInputs(List<String> uris){
        uriInputs = new ArrayList<>();
        uriInputs.addAll(uris);
    }
    public void loadSemanticDetailExtractionInputs(BlockingQueue<String> newURIsQueue) throws InterruptedException {
        TMDataSource semanticInputsIo = TMDataSourceFactory.instantiate(config.getSemanticAugmentationInput());
        while(true){
            String uri = semanticInputsIo.getNextSemanticDetailExtractionInput(queueSize);
            if (uri == null) break;
            newURIsQueue.put(uri);
        }
    }

    public void updateResourceDetails() {
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        // Passing it to the HttpClient.
        HttpClient httpClient = new HttpClient(connectionManager);
        ExecutorService executor = Executors.newFixedThreadPool(numOfThreads);
        int queueSize = 10000;

        BlockingQueue<String> newURIsQueue = new ArrayBlockingQueue<>(queueSize);

        TMDataSource semanticOutputsIo = TMDataSourceFactory.instantiate(config.getSemanticAugmentationOutput());

        logger.info(String.format("Getting extra fields from dbpedia.org using %d threads", numOfThreads));

        try {

            for (int thread = 0; thread < numOfThreads; thread++) {
                executor.submit(new DBpediaAnnotatorRunnable(
                        semanticOutputsIo, null, null, thread, httpClient, newURIsQueue,
                        spotlightService.replace("x", Integer.toString(thread)), confidence
                ));
            }

            if (newURIsQueue == null)
                loadSemanticDetailExtractionInputs(newURIsQueue);
            else{
                for (String uri : uriInputs) newURIsQueue.put(uri);
            }

        }catch (InterruptedException e) {
            logger.error("thread was interrupted, shutting down obtaining new resources phase", e);
            for (int i = 0; i < numOfThreads; i++) {
                try {
                    newURIsQueue.put(DBpediaAnnotatorRunnable.RESOURCE_POISON);
                } catch (InterruptedException e1) {
                    logger.error("got interrupted while sending poison to worker threads", e1);
                }
            }
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("execution was interrupted while awaiting submitted runnables finish", e);
        }
    }


    List<Text> pubTextInputs;
    int queueSize = 10000;
    public void setSemanticAugmentationInputs(List<Text> texts){
         pubTextInputs = new ArrayList<>();
         pubTextInputs.addAll(texts);
    }
    public void loadSemanticAugmentationInputs(BlockingQueue pubsQueue) throws InterruptedException {
        final int logBatchSize = 100000;
        int counter = 0;
        TMDataSource semanticInputsIo = TMDataSourceFactory.instantiate(config.getSemanticAugmentationInput());
        while (true){
            Text element = semanticInputsIo.getNextSemanticAugmentationInput(queueSize);
            if (element == null) break;
            pubsQueue.put(element);
            counter++;
            if (counter % logBatchSize == 0) logger.info(String.format("Read total %s publications", counter));
        }
    }

    public void annotatePubs() {
        AnnotatorType annotator = AnnotatorType.valueOf(config.getSemanticAnnotatorType());
        // Creating MultiThreadedHttpConnectionManager
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        // Passing it to the HttpClient.
        HttpClient httpClient = new HttpClient(connectionManager);
        connectionManager.getParams().setDefaultMaxConnectionsPerHost(20);
        connectionManager.getParams().setMaxTotalConnections(200);
        BlockingQueue<Text> pubsQueue = new ArrayBlockingQueue<>(queueSize);

        TMDataSource semanticOutputsIo = TMDataSourceFactory.instantiate(config.getSemanticAugmentationOutput());
        ExecutorService executor = Executors.newFixedThreadPool(numOfThreads);

        List<DBpediaAnnotatorRunnable> runnables = new ArrayList<>();
        try {
            logger.info(String.format("Start annotation using %d threads, @ %s with %.2f confidence", numOfThreads, spotlightService, confidence));
            for (int thread = 0; thread < numOfThreads; thread++) {
                DBpediaAnnotatorRunnable t = new DBpediaAnnotatorRunnable(semanticOutputsIo, annotator, pubsQueue, thread, httpClient,
                        null, spotlightService.replace("x", Integer.toString(thread + 1)), confidence);
                runnables.add(t);
                executor.submit(t);
            }

            if (pubsQueue == null)
                loadSemanticAugmentationInputs(pubsQueue);
            else{
                for (Text pt: pubTextInputs) pubsQueue.put(pt);
            }

            for (int i = 0; i < numOfThreads; i++)
                pubsQueue.put(new PubTextPoison());

        } catch (InterruptedException e) {
            logger.error("thread was interrupted, shutting down annotation phase", e);
            for (int i = 0; i < numOfThreads; i++) {
                try {
                    pubsQueue.put(new PubTextPoison());
                } catch (InterruptedException e1) {
                    logger.error("got interrupted while sending poison to worker threads", e1);
                }
            }
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("execution was interrupted while awaiting submitted runnables finish", e);
        }
        // get results
        for(DBpediaAnnotatorRunnable t: runnables) {
            allSemanticAnnotations.putAll(t.allEntities);
            allResources.addAll(t.allResources);
        }
    }

    public static void main(String[] args){
        //Class.forName("org.sqlite.JDBC");
        //Class.forName("org.postgresql.Driver");
        DBpediaAnnotator c = new DBpediaAnnotator(new Config("config.properties"));
        logger.info("DBPedia annotation: Annotate new publications");
        c.annotatePubs();
        logger.info("DBPedia annotation: Get extra fields from DBPedia");
        c.updateResourceDetails();

    }
}

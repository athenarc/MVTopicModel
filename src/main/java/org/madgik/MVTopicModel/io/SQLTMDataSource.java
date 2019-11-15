package org.madgik.MVTopicModel.io;

import cc.mallet.types.Instance;
import org.madgik.MVTopicModel.FastQMVWVTopicInferencer;
import org.madgik.MVTopicModel.FastQMVWVTopicModelDiagnostics;
import org.madgik.MVTopicModel.SciTopicFlow;
import org.madgik.MVTopicModel.model.*;
import org.madgik.utils.Utils;
import org.madgik.MVTopicModel.config.*;
import org.madgik.MVTopicModel.config.Config.ExperimentType;

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * Class for SQL-based data sources
 */
public class SQLTMDataSource extends TMDataSource {

    Connection connection;
    String jdbcString;
    public static final String name = "sql";
    Logger LOGGER = Logger.getLogger(SciTopicFlow.LOGGERNAME);

    public SQLTMDataSource(String properties) {
        super(properties);
    }

    @Override
    void initialize(String properties) {
        // properties is a jdbc string
        connection = null;
        String [] parts = properties.split(",");
        this.jdbcString =  parts[0];
    }

    private void closeConnection() {
        if(this.connection != null) {
            try {
                this.connection.close();
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }
    }
    /**
     * Persistent database connection fetcher
     * @return
     */
    private Connection getConnection(){
        try {
            if (this.connection != null && (!this.connection.isClosed()))
                return this.connection;
        } catch (SQLException e) {
            ;
        }
        try {
            this.connection = DriverManager.getConnection(this.jdbcString);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            return null;
        }
        return this.connection;
    }

    private ResultSet query(String sqlQuery){
        Statement statement = null;
        try {
            statement = getConnection().createStatement();
            return statement.executeQuery(sqlQuery);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public Map<Integer, String> getTopics(String experimentId){

        double topicWeight = 0.55;
        String sql = "select doc_topic.TopicId, document.title, document.abstract from \n"
                + "doc_topic\n"
                + "inner join publication on doc_topic.pubId= document.doc_id and doc_topic.Weight>" + topicWeight + "\n"
                + "where experimentId='" + experimentId + "' \n"
                + "order by doc_topic.topicid, weight desc";


        Map<Integer, String> res = new HashMap<>();
        ResultSet rs = query(sql);

        try {
            while (rs.next()) {
                res.put(rs.getInt("TopicId"), rs.getString("title"));
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        return res;
    }

    /**
     * Method to insert extracted keyphrases per topic to the sql backend
     * @param topicTitles
     * @param experimentId
     * @param keyphraseTagger
     */
    public void prepareTopicKeyphraseWrite(Map<Integer, Map<String, List<Integer>>> topicTitles, String experimentId, String keyphraseTagger){

        try {
            Statement statement = this.connection.createStatement();

            String createKeyphraseTable = "create table if not exists TopicKeyPhrase ( TopicId Integer, Tagger TEXT, Phrase Text, Count Integer, WordsNum Integer, Weight double, ExperimentId TEXT) ";
            statement.executeUpdate(createKeyphraseTable);

            String delete = String.format("Delete from TopicKeyPhrase WHERE ExperimentId='" + experimentId + "' AND Tagger ='" + keyphraseTagger + "'");
            statement.executeUpdate(delete);

            PreparedStatement bulkInsert = null;
            String insertSQL = "insert into TopicKeyPhrase values(?,?,?,?,?,?,?);";

            logger.info("Saving key phrases.");
            try {

                connection.setAutoCommit(false);
                bulkInsert = connection.prepareStatement(insertSQL);

                for (Integer tmpTopicId : topicTitles.keySet()) {
                    //boolean startComparison = false;fuyhgjlkfdytrdfuikol
                    Map<String, List<Integer>> extractedPhrases = topicTitles.get(tmpTopicId);
                    for (String phrase : extractedPhrases.keySet()) {

                        bulkInsert.setInt(1, tmpTopicId);
                        bulkInsert.setString(2, keyphraseTagger);
                        bulkInsert.setString(3, phrase);
                        bulkInsert.setInt(4, extractedPhrases.get(phrase).get(0));
                        bulkInsert.setInt(5, extractedPhrases.get(phrase).get(1));
                        bulkInsert.setDouble(6, 0);
                        bulkInsert.setString(7, experimentId);

                        bulkInsert.executeUpdate();
                    }

                }
                connection.commit();

            } catch (SQLException e) {

                logger.error("Error in insert topicPhrases: " + e);
                if (connection != null) {
                    try {
                        logger.error("Transaction is being rolled back");
                        connection.rollback();
                    } catch (SQLException excep) {
                        logger.error("Error in insert topicPhrases: " + excep);
                    }
                }
            } finally {

                if (bulkInsert != null) {
                    bulkInsert.close();
                }
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            logger.error(e.getMessage());
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                logger.error(e);
            }
        }
        logger.info("Finding Key phrases finished");
    }

    @Override
    public void prepareOutput(String experimentId) {
        Statement statement = null;
        try {
            statement = this.getConnection().createStatement();
            // create a database connection
            String [] delSources = {"doc_topic", "TopicDetails", "Topic", "TopicAnalysis", "ExpDiagnostics"};
            for (String delTable: delSources)
                statement.executeUpdate(String.format("Delete from %s where ExperimentId ='%s'", delTable, experimentId));
        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
        } finally {
            this.closeConnection();
        }
    }

    @Override
    public void saveResults(List<TopicData> topicData, List<DocumentTopicAssignment> docTopics, String batchId, String experimentId,
                            String experimentDescription, String experimentMetadata){


        PreparedStatement bulkInsert = null;
        try {

            // Save Topic Analysis
            String sql = "insert into TopicAnalysis values(?,?,?,?,?,?);";
            getConnection().setAutoCommit(false);
            bulkInsert = getConnection().prepareStatement(sql);

            for(TopicData topic : topicData){
                for (Modality mod : topic.getModalities()) {
                    for (String word : mod.getWordWeights().keySet()) {

                        bulkInsert.setInt(1, topic.getTopicId());
                        bulkInsert.setInt(2, mod.getId());
                        bulkInsert.setString(3, word);
                        bulkInsert.setDouble(4, mod.getWordWeights().get(word));
                        bulkInsert.setString(5, batchId);
                        bulkInsert.setString(6, experimentId);
                        bulkInsert.executeUpdate();
                    }
                }
            }


            // Save Topic Analysis
            for(TopicData topic : topicData) {
                Modality textModality = topic.getModality(Modality.types.TEXT);
                if (textModality == null) {
                    logger.error("No text modality found to save prhase data!");
                    return;
                }
                for (String phrase : textModality.getPhraseWeights().keySet()) {
                    bulkInsert.setInt(1, topic.getTopicId());
                    bulkInsert.setInt(2, -1);
                    bulkInsert.setString(3, phrase);
                    bulkInsert.setDouble(4, textModality.getPhraseWeights().get(phrase));
                    bulkInsert.setString(5, batchId);
                    bulkInsert.setString(6, experimentId);
                    //bulkInsert.setDouble(6, 1);
                    bulkInsert.executeUpdate();
                }
                connection.commit();
            }

        } catch (SQLException e) {
            logger.error("Exception in save Topics: " + e.getMessage());
            //System.err.print(e.getMessage());
            if (connection != null) {
                try {
                    logger.error("Transaction is being rolled back");
                    connection.rollback();
                } catch (SQLException excep) {
                    logger.error("Error in insert topicAnalysis");
                }
            }
        } finally {

            if (bulkInsert != null) {
                try {
                    bulkInsert.close();
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                    logger.error(ex.getMessage());
                }
            }
        }


        try{
            String boostSelect = String.format("select  \n"
                    + " a.experimentid, PhraseCnts, textcnts, textcnts/phrasecnts as boost\n"
                    + "from \n"
                    + "(select experimentid, itemtype, avg(counts) as PhraseCnts from topicanalysis\n"
                    + "where itemtype=-1\n"
                    + "group by experimentid, itemtype) a inner join\n"
                    + "(select experimentid, itemtype, avg(counts) as textcnts from topicanalysis\n"
                    + "where itemtype=0  and ExperimentId = '%s' \n"
                    + "group by experimentid, itemtype) b on a.experimentId=b.experimentId\n"
                    + "order by a.experimentId;", experimentId);
            float boost = 70;
            ResultSet rs = query(boostSelect);
            while (rs.next()) {
                boost = rs.getFloat("boost");
            }
            bulkInsert = null;
            String sql = "insert into Experiment (ExperimentId  ,    Description,    Metadata  ,    InitialSimilarity,    PhraseBoost, ended) values(?,?,?, ?, ?,? );";
            connection.setAutoCommit(false);
            bulkInsert = connection.prepareStatement(sql);
            bulkInsert.setString(1, experimentId);
            bulkInsert.setString(2, experimentDescription);
            bulkInsert.setString(3, experimentMetadata);
            bulkInsert.setDouble(4, 0.6);
            bulkInsert.setInt(5, Math.round(boost));
            bulkInsert.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            bulkInsert.executeUpdate();
            connection.commit();

        } catch (SQLException e) {
            logger.error("Exception in save Topics: " + e.getMessage());
            if (connection != null) {
                try {
                    logger.error("Transaction is being rolled back");
                    connection.rollback();
                } catch (SQLException excep) {
                    logger.error("Error in insert experiment details");
                }
            }
        } finally {

            try {
                connection.setAutoCommit(true);
                if (bulkInsert != null) bulkInsert.close();
            }
            catch (SQLException ex) {
                    logger.error(ex.getMessage());
           }
        }

            try {

                String insertTopicDescriptionSql = "INSERT into Topic (Title, Category, id , VisibilityIndex, ExperimentId )\n"
                        + "select substr(string_agg(Item,','),1,100), '' , topicId , 1, '" + experimentId + "' \n"
                        + "from  Topic_View\n"
                        + " where experimentID = '" + experimentId + "' \n"
                        + " GROUP BY TopicID";
                Statement statement = getConnection().createStatement();
                statement.executeUpdate(insertTopicDescriptionSql);
            } catch (SQLException e) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                logger.error("Exception in save Topics: " + e.getMessage());
            } finally {
                /*try {
                    if (connection != null) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    // connection close failed.
                    LOGGER.error(e);
                }
                 */
            }

            String topicDetailInsertsql = "insert into TopicDetails values(?,?,?,?,?,? );";
            PreparedStatement bulkTopicDetailInsert = null;
            bulkTopicDetailInsert = null;
            try {
                bulkTopicDetailInsert = connection.prepareStatement(topicDetailInsertsql);
                connection.setAutoCommit(false);


                for(TopicData topic : topicData) {
                    for (Modality mod : topic.getModalities()) {
                        bulkTopicDetailInsert.setInt(1, topic.getTopicId());
                        bulkTopicDetailInsert.setInt(2, mod.getId());
                        bulkTopicDetailInsert.setDouble(3, mod.getWeight());
                        bulkTopicDetailInsert.setInt(4, mod.getNumTokens());
                        bulkTopicDetailInsert.setString(5, batchId);
                        bulkTopicDetailInsert.setString(6, experimentId);

                        bulkTopicDetailInsert.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                logger.error("Exception in save Topics: " + e.getMessage());

                if (connection != null) {
                    try {
                        logger.error("Transaction is being rolled back");
                        connection.rollback();
                    } catch (SQLException excep) {
                        logger.error("Error in insert topic details");
                    }
                }
            } finally {

                try {
                    if (bulkTopicDetailInsert != null) {
                        bulkTopicDetailInsert.close();
                    }
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                    logger.error(ex.getMessage());
                }
            }
            logger.info("Done saving results.");


        // save document assignments
        String docSql = "insert into doc_topic values(?,?,?,?,?);";
        try {
            bulkInsert = connection.prepareStatement(docSql);
            connection.setAutoCommit(false);
            for(DocumentTopicAssignment dta : docTopics) {
                for (int topic : dta.getTopicWeights().keySet()) {

                    bulkInsert.setString(1, dta.getId());
                    bulkInsert.setInt(2, topic);
                    bulkInsert.setDouble(3, dta.getTopicWeights().get(topic));
                    bulkInsert.setString(4, experimentId);
                    bulkInsert.setBoolean(5, true);
                    bulkInsert.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            logger.error(ex.getMessage());
        }
        finally {
            try {
                connection.setAutoCommit(true);
                bulkInsert.close();
            } catch (SQLException ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    @Override
    public void saveDiagnostics(int numModalities, String batchId, String experimentId, double[][] perplexities,
                                int numTopics, List<FastQMVWVTopicModelDiagnostics.TopicScores> diagnostics) {
        Connection connection = null;
        double perplexity = 0;
        String current_score = "";
        try {
            Statement statement = getConnection().createStatement();

            PreparedStatement bulkInsert = null;
            String sql = "insert into expDiagnostics values(?,?,?,?,?,?);";

            connection.setAutoCommit(false);
            bulkInsert = connection.prepareStatement(sql);

            for (byte m = 0; m < numModalities; m++) {
                current_score =  String.format("perplexity %d" , m);

                bulkInsert.setString(1, experimentId);
                bulkInsert.setString(2, batchId);
                bulkInsert.setString(3, "TestCorpus");
                bulkInsert.setInt(4, 0); //corpus
                bulkInsert.setString(5, "perplexity");
                bulkInsert.setDouble(6, perplexity);
                bulkInsert.executeUpdate();

                int p = 1;
                while (p < perplexities[m].length && perplexities[m][p] != 0) //for (int p = 0; p < model.perplexities[m].length; p++)
                {
                    current_score =  String.format("LogLikehood %d %d", m,p);
                    bulkInsert.setString(1, experimentId);
                    bulkInsert.setString(2, batchId);
                    bulkInsert.setString(3, String.format("%d", 10 * p));
                    bulkInsert.setInt(4, 0); //corpus
                    bulkInsert.setString(5, "LogLikehood");
                    bulkInsert.setDouble(6, perplexities[m][p]);
                    bulkInsert.executeUpdate();
                    p++;
                }
            }

            for (int topic = 0; topic < numTopics; topic++) {

                for (FastQMVWVTopicModelDiagnostics.TopicScores scores : diagnostics) {
                    current_score = scores.name+String.format(" Topic %d", topic);

                    bulkInsert.setString(1, experimentId);
                    bulkInsert.setString(2, batchId);
                    bulkInsert.setString(3, String.format("Topic %d", topic));
                    bulkInsert.setInt(4, 1); //Topic
                    bulkInsert.setString(5, scores.name);
                    bulkInsert.setDouble(6, scores.scores[topic]);
                    bulkInsert.executeUpdate();
                }

//                for (int position = 0; position < topicTopWords[topic].length; position++) {
//                    if (topicTopWords[topic][position] == null) {
//                        break;
//                    }
//
//                    formatter.format("  %s", topicTopWords[topic][position]);
//                    for (TopicScores scores : diagnostics) {
//                        if (scores.wordScoresDefined) {
//                            formatter.format("\t%s=%.4f", scores.name, scores.topicWordScores[topic][position]);
//                            bulkInsert.setString(1, experimentId);
//                    bulkInsert.setString(2, String.format("Topic %d", topic));
//                    bulkInsert.setInt(3, 1); //Word
//                    bulkInsert.setString(4, scores.name);
//                    bulkInsert.setDouble(5, scores.scores[topic]);
//                    bulkInsert.executeUpdate();
//                        }
//                    }
//
//                }
            }

            connection.commit();
            if (bulkInsert != null) {
                bulkInsert.close();
            }
            connection.setAutoCommit(true);

        } catch (SQLException e) {

            logger.error("Exception in save diagnostics score ["+ current_score +"] : "+ e.getMessage());

            if (connection != null) {
                try {
                    logger.error("Transaction is being rolled back \n");
                    connection.rollback();
                } catch (SQLException excep) {
                    logger.error("Error in insert expDiagnostics \n");
                }
            }
        } finally {
        }
    }

    @Override
    public void prepareTopicDistroTrendsOutput(String experimentId) {

        try {
            Statement statement = getConnection().createStatement();

            logger.info("Calc topic Entity Topic Distributions and Trends started");
            String deleteSQL = String.format("Delete from EntityTopicDistribution where ExperimentId= '%s'", experimentId);
            statement.executeUpdate(deleteSQL);
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }

    }

    @Override
    public void saveDocumentTopicAssignments(Config config, Map<String, Map<Integer, Double>> docTopicMap, String runType) {
        String experimentId = config.getExperimentId();
        String sql = "insert into doc_topic values(?,?,?,?,?);";
        PreparedStatement bulkInsert = null;
        try {
            getConnection().setAutoCommit(false);
            bulkInsert = getConnection().prepareStatement(sql);
            for (String docId : docTopicMap.keySet()) {
                // delete existing
                PreparedStatement deletePrevious = getConnection().prepareStatement(String.format("Delete from doc_topic where  ExperimentId = '%s' and docid='%s' ", experimentId, docId));
                deletePrevious.executeUpdate();

                for (Integer topicId : docTopicMap.get(docId).keySet()) {
                    Double weight = docTopicMap.get(docId).get(topicId);
                    bulkInsert.setString(1, docId);
                    bulkInsert.setInt(2, topicId);
                    bulkInsert.setDouble(3, weight);
                    bulkInsert.setString(4, experimentId);
                    bulkInsert.setBoolean(5, true);
                    bulkInsert.executeUpdate();
                }
            }

            getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public FastQMVWVTopicInferencer getInferenceModel(Config config) {
        FastQMVWVTopicInferencer topicModel = null;
        String experimentId = config.getExperimentId();
        Statement statement;
        try {
            // create a database connection
                statement = getConnection().createStatement();
                String modelSelect = String.format("select model from inferencemodel "
                        + "where experimentid = '%s' \n",
                        experimentId);

                boolean loaded = false;
                ResultSet rs = statement.executeQuery(modelSelect);
                while (rs.next()) {
                    if (loaded){
                       logger.error("Found more than one model for experiment " + experimentId);
                       return null;
                    }
                    try {
                        byte b[] = (byte[]) rs.getObject("model");
                        ByteArrayInputStream bi = new ByteArrayInputStream(b);
                        ObjectInputStream si = new ObjectInputStream(bi);
                        topicModel = (FastQMVWVTopicInferencer) si.readObject();
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                    }
                    loaded = true;
                }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                logger.error(e.getMessage());
            }
        }
        return topicModel;
    }

    @Override
    public void deleteExistingExperiment(Config config) {
        String experimentId = config.getExperimentId();
        logger.info("Deleting previous experiment" + experimentId);
        try {
            // create a database connection
            Statement statement = getConnection().createStatement();
            statement.executeUpdate(String.format("Delete from doc_topic where  ExperimentId = '%s'", experimentId));

            String deleteSQL = String.format("Delete from Experiment where  ExperimentId = '%s'", experimentId);
            statement.executeUpdate(deleteSQL);

            deleteSQL = String.format("Delete from TopicDetails where  ExperimentId = '%s'", experimentId);
            statement.executeUpdate(deleteSQL);

            deleteSQL = String.format("Delete from Topic where  ExperimentId = '%s'", experimentId);
            statement.executeUpdate(deleteSQL);

            deleteSQL = String.format("Delete from TopicAnalysis where  ExperimentId = '%s'", experimentId);
            statement.executeUpdate(deleteSQL);

            deleteSQL = String.format("Delete from ExpDiagnostics where  ExperimentId = '%s'", experimentId);
            statement.executeUpdate(deleteSQL);
        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e);
            }

        }
    }


    @Override
    public void saveTopicsAndExperiment(Config config, List<TopicAnalysis> topicAnalysisList, List<TopicDetails> topicDetailsList, byte[] serializedModel, String experimentMetadata) {

        Statement statement = null;
        try {
            PreparedStatement bulkInsert = null;
            String sql = "insert into TopicAnalysis values(?,?,?,?,?,?);";

            try {
                getConnection().setAutoCommit(false);
                // store topic analysis
                bulkInsert = getConnection().prepareStatement(sql);
                for (TopicAnalysis ta : topicAnalysisList){
                    bulkInsert.setInt(1, ta.getTopicId());
                    bulkInsert.setInt(2, ta.getModality());
                    bulkInsert.setString(3, ta.getItem());
                    bulkInsert.setDouble(4, ta.getWeight());
                    bulkInsert.setString(5, "");
                    bulkInsert.setString(6, config.getExperimentId());
                    //bulkInsert.setDouble(6, 1);
                    bulkInsert.executeUpdate();

                }
                connection.commit();
                } catch (SQLException e) {

                    logger.error("Exception in save Topics: " + e.getMessage());
                    //System.err.print(e.getMessage());
                    if (connection != null) {
                        try {
                            logger.error("Transaction is being rolled back");
                            connection.rollback();
                        } catch (SQLException excep) {
                            logger.error("Error in insert topicAnalysis");
                        }
                    }
                } finally {

                    if (bulkInsert != null) {
                        bulkInsert.close();
                    }
                    connection.setAutoCommit(true);
                }


            // boost
            statement = connection.createStatement();
            String boostSelect = String.format("select  \n"
                    + " a.experimentid, PhraseCnts, textcnts, textcnts/phrasecnts as boost\n"
                    + "from \n"
                    + "(select experimentid, itemtype, avg(counts) as PhraseCnts from topicanalysis\n"
                    + "where itemtype=-1\n"
                    + "group by experimentid, itemtype) a inner join\n"
                    + "(select experimentid, itemtype, avg(counts) as textcnts from topicanalysis\n"
                    + "where itemtype=0  and ExperimentId = '%s' \n"
                    + "group by experimentid, itemtype) b on a.experimentId=b.experimentId\n"
                    + "order by a.experimentId;", config.getExperimentId());
            float boost = 70;
            ResultSet rs = statement.executeQuery(boostSelect);
            while (rs.next()) {
                boost = rs.getFloat("boost");
            }

            sql = "insert into Experiment (ExperimentId  ,    Description,    Metadata  ,    InitialSimilarity,    PhraseBoost, ended) values(?,?,?, ?, ?,?);";

            try {
                connection.setAutoCommit(false);

                bulkInsert = connection.prepareStatement(sql);

                LocalDateTime now = LocalDateTime.now();
                Timestamp timestamp = Timestamp.valueOf(now);

                bulkInsert.setString(1, config.getExperimentId());
                bulkInsert.setString(2, config.getExperimentDetails());
                bulkInsert.setString(3, experimentMetadata);
                bulkInsert.setDouble(4, 0.6);
                bulkInsert.setInt(5, Math.round(boost));
                bulkInsert.setTimestamp(6, timestamp);
                bulkInsert.executeUpdate();
                connection.commit();

                sql = "insert into inferencemodel (ExperimentId, model) values(?,?);";
                bulkInsert = connection.prepareStatement(sql);
                bulkInsert.setString(1, config.getExperimentId());
                ByteArrayInputStream bais = new ByteArrayInputStream(serializedModel);
                bulkInsert.setBinaryStream(2, bais);
                bulkInsert.executeUpdate();

                connection.commit();

            } catch (SQLException e) {

                logger.error("Exception in save Topics: " + e.getMessage());
                if (connection != null) {
                    try {

                        logger.error("Transaction is being rolled back");
                        connection.rollback();
                    } catch (SQLException excep) {
                        logger.error("Error in insert experiment details");
                    }
                }
            } finally {

                if (bulkInsert != null) {
                    bulkInsert.close();
                }
                connection.setAutoCommit(true);
            }

            try {

                String insertTopicDescriptionSql = "INSERT into Topic (Title, Category, id , VisibilityIndex, ExperimentId )\n"
                        + "select substr(string_agg(Item,','),1,100), '' , topicId , 1, '" + config.getExperimentId() + "' \n"
                        + "from  Topic_View\n"
                        + " where experimentID = '" + config.getExperimentId() + "' \n"
                        + " GROUP BY TopicID";

                statement = getConnection().createStatement();
                statement.executeUpdate(insertTopicDescriptionSql);
                //ResultSet rs = statement.executeQuery(sql);

            } catch (SQLException e) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                logger.error("Exception in save Topics: " + e.getMessage());
            } finally {
                /*try {
                    if (connection != null) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    // connection close failed.
                    LOGGER.error(e);
                }
                 */
            }

            // topic details
            String topicDetailInsertsql = "insert into TopicDetails values(?,?,?,?,?,? );";
            PreparedStatement bulkTopicDetailInsert = null;
            try {
                bulkTopicDetailInsert = connection.prepareStatement(topicDetailInsertsql);
                connection.setAutoCommit(false);
                for (TopicDetails td: topicDetailsList){
                        bulkTopicDetailInsert.setInt(1, td.getTopicId());
                        bulkTopicDetailInsert.setInt(2, td.getModality());
                        bulkTopicDetailInsert.setDouble(3, td.getWeight());
                        bulkTopicDetailInsert.setInt(4, td.getTotalTokens());
                        bulkTopicDetailInsert.setString(5, "");
                        bulkTopicDetailInsert.setString(6, config.getExperimentId());
                        bulkTopicDetailInsert.executeUpdate();
                }
                connection.commit();

            } catch (SQLException e) {
                logger.error("Exception in save Topic details: " + e.getMessage());

                if (connection != null) {
                    try {
                        logger.error("Transaction is being rolled back");
                        connection.rollback();
                    } catch (SQLException excep) {
                        logger.error("Error in insert topic details");
                    }
                }
            } finally {

                if (bulkTopicDetailInsert != null) {
                    bulkTopicDetailInsert.close();
                }
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }
    }

    public ArrayList<ArrayList<Instance>> getInferenceInputs(Config config) {
        return null;
    }

    public ArrayList<ArrayList<Instance>> getModellingInputs(Config config) {
//        instanceBuffer = ReadDataFromDB(SQLConnectionString, experimentType, numModalities, limitDocs, filter);
        int numModalities = config.getNumModalities();
        ArrayList<ArrayList<Instance>> instanceBuffer = new ArrayList<>(numModalities);
        String SQLConnection = jdbcString;
        int limitDocs = config.getLimitDocs();
        ExperimentType experimentType = config.getExperimentType();
        String filter = " where batchid > '2018'";
        int numChars = config.getNumChars();

        for (byte m = 0; m < numModalities; m++) instanceBuffer.add(new ArrayList<>());

        Connection connection = null;
        try {

            connection = DriverManager.getConnection(SQLConnection);
            connection.setAutoCommit(false);

            String sql = "";
            // String txtsql = "select doctxt_view.docId, text, fulltext from doctxt_view " + filter + " Order by doctxt_view.docId " + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");
            String txtsql = "select distinct ON (document.id)  document.id as docid, " +
                    "substr((((COALESCE(pmc_titles_temp.title, ''::text) || ' '::text) || substr(COALESCE(document.abstract_pmc, ''::text), 0, 7000)) || ' '::text), 0, 10000) AS text,"
                    + "batchid from document \n"
                    + " LEFT JOIN doc_project on doc_project.docid = document.id  \n" +
                    "left join pmc_titles_temp on pmc_titles_temp.docid = document.id \n"
                    + "where document.doctype='publication' and batchid > '2017' and (language_pmc is null or language_pmc = 'eng') and document.abstract_pmc is not null\n"
                    + "and (repository = 'PubMed Central' OR  doc_project.projectid IN \n"
                    + "(select projectid from projects_atleast5docs))"
                    + "Order by document.id \n"
                    + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");//+ " LIMIT 10000";

            if (experimentType == ExperimentType.ACM) {

                sql = " select  docid,  citations, categories, keywords, venue, DBPediaResources from docsideinfo_view " + filter + " Order by docsideinfo_view.docId " + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");

                /*
                if (PPRenabled == Net2BoWType.PPR) {
                    sql = " select  docid,   citations, categories, period, keywords, venue, DBPediaResources from docsideinfo_view  Order by docsideinfo_view.docId  " + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");
                } else if (PPRenabled == Net2BoWType.OneWay) {

                    sql = " select  docid,  citations, categories, keywords, venue, DBPediaResources from docsideinfo_view  Order by docsideinfo_view.docId " + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");
                } else if (PPRenabled == Net2BoWType.TwoWay) {
                    sql = " select  docid, authors, citations, categories, keywords, venue, DBPediaResources from docsideinfo_view  Order by docsideinfo_view.docId  " + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");

                }
                 */
            } else if (experimentType == ExperimentType.PubMed) {
                // sql = " select  docid, keywords, meshterms, dbpediaresources  from docsideinfo_view  " + filter + " Order by docsideinfo_view.docId " + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");

                sql = "select distinct ON (docsideinfo_norescount_view.docid)  docsideinfo_norescount_view.docid, keywords, meshterms, dbpediaresources  \n"
                        + "from docsideinfo_norescount_view  \n"
                        + "LEFT JOIN doc_project on doc_project.docid = docsideinfo_norescount_view.docId\n"
                        + "LEFT JOIN document on document.id = docsideinfo_norescount_view.docId\n"

                        + "where document.doctype='publication' and document.batchid > '2017' and (language_pmc is null or language_pmc = 'eng') and document.abstract_pmc is not null\n"
                        + "and (document.repository = 'PubMed Central' OR  doc_project.projectid IN \n"
                        + "(select projectid from projects_atleast5docs))"
                        + "Order by docsideinfo_norescount_view.docId \n"
                        + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");


                LOGGER.info("Text SQL:\n" + txtsql);
                /* if (D4I) {
                    sql = "select distinct ON (docsideinfo_view.docid)  docsideinfo_view.docid, keywords, meshterms, dbpediaresources  \n"
                            + "from docsideinfo_view  \n"
                            + "LEFT JOIN doc_project on doc_project.docid = docsideinfo_view.docId\n"
                            + "where batchid > '2004' and (doctype='publication' OR doctype='project_report') \n"
                            + "and (repository = 'PubMed Central' OR  doc_project.projectid IN \n"
                            + "(select projectid from doc_project\n"
                            + "join document on doc_project.docid = document.id and repository = 'PubMed Central'\n"
                            + "group by projectid\n"
                            + "having count(*) > 5) )\n"
                            + "Order by docsideinfo_view.docId \n"
                            + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");
                }*/

            }

            LOGGER.info(" Getting text from the database");
            // get txt data
            Statement txtstatement = connection.createStatement();
            txtstatement.setFetchSize(10000);
            ResultSet rstxt = txtstatement.executeQuery(txtsql);

            while (rstxt.next()) {

                String txt = "";

                switch (experimentType) {

                    case ACM:
                    case PubMed:
                        txt = rstxt.getString("text");
                        instanceBuffer.get(0).add(new Instance(txt.substring(0, Math.min(txt.length() - 1, numChars)), null, rstxt.getString("docid"), "text"));

                        break;

                    default:
                }
            }

            if (numModalities > 1) {
                LOGGER.info(" Getting side info from the database");
                Statement statement = connection.createStatement();
                statement.setFetchSize(10000);
                ResultSet rs = statement.executeQuery(sql);

                while (rs.next()) {
                    // read the result set

                    switch (experimentType) {

                        case ACM:
//                        instanceBuffer.get(0).add(new Instance(rs.getString("Text"), null, rs.getString("pubId"), "text"));
                            //String txt = rs.getString("text");
                            //instanceBuffer.get(0).add(new Instance(txt.substring(0, Math.min(txt.length() - 1, numChars)), null, rs.getString("pubId"), "text"));

                            if (numModalities > 1) {
                                String tmpJournalStr = rs.getString("Keywords");//.replace("\t", ",");
                                if (tmpJournalStr != null && !tmpJournalStr.equals("")) {
                                    instanceBuffer.get(1).add(new Instance(tmpJournalStr.replace('-', ' ').toLowerCase(), null, rs.getString("docid"), "Keywords"));
                                }
                            }

                            if (numModalities > 2) {
                                String tmpStr = rs.getString("DBPediaResources");//.replace("\t", ",");
                                String DBPediaResourceStr = "";
                                if (tmpStr != null && !tmpStr.equals("")) {
                                    String[] DBPediaResources = tmpStr.trim().split(",");
                                    for (int j = 0; j < DBPediaResources.length; j++) {
                                        String[] pairs = DBPediaResources[j].trim().split(";");
                                        if (pairs.length == 2) {
                                            for (int i = 0; i < Integer.parseInt(pairs[1]); i++) {
                                                DBPediaResourceStr += pairs[0] + ",";
                                            }
                                        } else {
                                            DBPediaResourceStr += DBPediaResources[j] + ",";

                                        }
                                    }
                                    DBPediaResourceStr = DBPediaResourceStr.substring(0, DBPediaResourceStr.length() - 1);
                                    instanceBuffer.get(2).add(new Instance(DBPediaResourceStr, null, rs.getString("docid"), "DBPediaResource"));
                                }
                            }

                            if (numModalities > 3) {
                                String tmpStr = rs.getString("Categories");//.replace("\t", ",");
                                if (tmpStr != null && !tmpStr.equals("")) {

                                    instanceBuffer.get(3).add(new Instance(tmpStr, null, rs.getString("docid"), "category"));
                                }
                            }

                            if (numModalities > 4) {
                                String tmpStr = rs.getString("Citations");//.replace("\t", ",");
                                String citationStr = "";
                                if (tmpStr != null && !tmpStr.equals("")) {
                                    String[] citations = tmpStr.trim().split(",");
                                    for (int j = 0; j < citations.length; j++) {
                                        String[] pairs = citations[j].trim().split(":");
                                        if (pairs.length == 2) {
                                            for (int i = 0; i < Integer.parseInt(pairs[1]); i++) {
                                                citationStr += pairs[0] + ",";
                                            }
                                        } else {
                                            citationStr += citations[j] + ",";

                                        }
                                    }
                                    citationStr = citationStr.substring(0, citationStr.length() - 1);
                                    instanceBuffer.get(4).add(new Instance(citationStr, null, rs.getString("docid"), "citation"));
                                }
                            }

                            if (numModalities > 5) {
                                String tmpAuthorsStr = rs.getString("Venue");//.replace("\t", ",");
                                if (tmpAuthorsStr != null && !tmpAuthorsStr.equals("")) {

                                    instanceBuffer.get(5).add(new Instance(tmpAuthorsStr, null, rs.getString("docid"), "Venue"));
                                }
                            }

//DBPediaResources
                            if (numModalities > 6) {
                                String tmpAuthorsStr = rs.getString("Authors");//.replace("\t", ",");
                                if (tmpAuthorsStr != null && !tmpAuthorsStr.equals("")) {

                                    instanceBuffer.get(6).add(new Instance(tmpAuthorsStr, null, rs.getString("docid"), "author"));
                                }
                            }

                            if (numModalities > 7) {
                                String tmpPeriod = rs.getString("Period");//.replace("\t", ",");
                                if (tmpPeriod != null && !tmpPeriod.equals("")) {

                                    instanceBuffer.get(7).add(new Instance(tmpPeriod, null, rs.getString("docid"), "period"));
                                }
                            }

                            break;
                        case PubMed:
                            if (numModalities > 1) {
                                String tmpJournalStr = rs.getString("Keywords");//.replace("\t", ",");
                                if (tmpJournalStr != null && !tmpJournalStr.equals("")) {
                                    instanceBuffer.get(1).add(new Instance(tmpJournalStr.replace('-', ' ').toLowerCase(), null, rs.getString("docid"), "Keywords"));
                                }
                            }

                            if (numModalities > 2) {
                                String tmpMeshTermsStr = rs.getString("meshterms");//.replace("\t", ",");
                                if (tmpMeshTermsStr != null && !tmpMeshTermsStr.equals("")) {
                                    instanceBuffer.get(2).add(new Instance(tmpMeshTermsStr.replace('-', ' ').toLowerCase(), null, rs.getString("docid"), "MeshTerms"));
                                }
                            }

                            if (numModalities > 3) {
                                String tmpStr = rs.getString("DBPediaResources");//.replace("\t", ",");
                                //http://dbpedia.org/resource/Aerosol:3;http://dbpedia.org/resource/Growth_factor:4;http://dbpedia.org/resource/Hygroscopy:4;http://dbpedia.org/resource/Planetary_boundary_layer:3
                                String DBPediaResourceStr = "";
                                if (tmpStr != null && !tmpStr.equals("")) {
                                    String[] DBPediaResources = tmpStr.trim().split(";");
                                    for (int j = 0; j < DBPediaResources.length; j++) {
                                        String[] pairs = DBPediaResources[j].trim().split("#");
                                        if (pairs.length == 2) {
                                            for (int i = 0; i < Integer.parseInt(pairs[1]); i++) {
                                                DBPediaResourceStr += pairs[0] + ";";
                                            }
                                        } else {
                                            DBPediaResourceStr += DBPediaResources[j] + ";";

                                        }
                                    }
                                    DBPediaResourceStr = DBPediaResourceStr.substring(0, DBPediaResourceStr.length() - 1);
                                    instanceBuffer.get(3).add(new Instance(DBPediaResourceStr, null, rs.getString("docid"), "DBPediaResources"));
                                }
                            }

                            break;

                        default:
                    }

                }
            }

        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            LOGGER.error(e.getMessage());

        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                LOGGER.error(e.getMessage());

            }
        }

        for (byte m = (byte) 0; m < numModalities; m++) {

            LOGGER.info("Read " + instanceBuffer.get(m).size() + " instances modality: " + (instanceBuffer.get(m).size() > 0 ? instanceBuffer.get(m).get(0).getSource().toString() : m));

        }
        return instanceBuffer;

    }












    private Double getPhraseBoost(String expid) throws SQLException {
        ResultSet rs = query("select phraseboost from experiment where experimentid = '" + expid + "'");
        while(rs.next()) return rs.getDouble("phraseboost");
        return null;
    }

    Map<Integer, Map<String, Integer>> getTokenCountAcrossTopics(String expid) throws SQLException {
        String query = "select item, itemtype, sum(counts) as count from topicanalysis where experimentid = '" + expid + "' group by (item, itemtype);";
        ResultSet rs = query(query);
        Map<Integer, Map<String, Integer>>  res = new HashMap<>();
        while (rs.next()){
            int modalityType = rs.getInt("itemtype");
            String token = rs.getString("item");
            int count = rs.getInt("count");
            if (! res.containsKey(modalityType)) res.put(modalityType, new HashMap<>());
            res.get(modalityType).put(token, count);
        }
        return res;
    }

    Map<Integer, Map<Integer, Integer>> getTopicTokenCounts(String expid) throws SQLException {
        String query = "select topicid, itemtype, sum(counts) as count from topicanalysis where experimentid = '" + expid + "' group by (topicid, itemtype);";
        ResultSet rs = query(query);
        Map<Integer, Map<Integer, Integer>>  res = new HashMap<>();
        while (rs.next()){
            int topicid = rs.getInt("topicid");
            int modalityid = rs.getInt("itemtype");
            int count = rs.getInt("count");
            if (! res.containsKey(topicid)) res.put(topicid, new HashMap<>());
            res.get(topicid).put(modalityid, count);
        }
        return res;
    }

    public Map<Integer, Map<String, Map<String, Double>>> getTopicInformation(String query, double prob_threshold, String weight_type, String expid) throws SQLException {


        Map<Integer, Map<String, Map<String, Double>>> res = new HashMap<>();
        // from topic details: topic index, topic total tokens,
        double phraseboost = getPhraseBoost(expid);
        Map<Integer, Map<String, Integer>> tokenCountsAcrossTopics = getTokenCountAcrossTopics(expid);
        Map<Integer, Map<Integer, Integer>> topicTokenCounts = getTopicTokenCounts(expid);

        ResultSet rs = query(query);

        String [] modalityNames = {"Text", "MESH", "DBPedia"};
        while (rs.next()){
            int topic_id = rs.getInt("topicid");
            int modality_id = rs.getInt("modalityid");
            String modality_name = (modality_id >=0) ? modalityNames[modality_id] : "Phrase";
            String token_name = rs.getString("item");
            int token_count = rs.getInt("counts");

            if (!res.containsKey(topic_id)) res.put(topic_id, new HashMap<>());
            if (!res.get(topic_id).containsKey(modality_name)) res.get(topic_id).put(modality_name, new HashMap<>());
            if (res.get(topic_id).get(modality_name).containsKey(token_name)){
                logger.error(String.format("Duplicate token %s for modality %s and topic %d !", token_name, modality_name, topic_id));
            }
            int total_tokens_in_topic = topicTokenCounts.get(topic_id).get(modality_id);
            int total_tokens_across_topics = tokenCountsAcrossTopics.get(modality_id).get(token_name);

            // if (modality_name.equals("Phrase")){
            //     token_count *= phraseboost;
            // }

            // calc token importance
            // this below is the regular topic modelling probability
            double token_importance_within_topic = ((double)token_count) / total_tokens_in_topic;
            // this below is the token weight in this topic, compared to other topics
            double token_importance_across_topics = ((double)token_count) / total_tokens_across_topics;

            if (token_importance_across_topics > 1 || token_importance_within_topic > 1 ||
                token_importance_across_topics < 0 || token_importance_within_topic < 0) {
                logger.error(String.format("Problematic weights for topic %d modality %s token %s : within-top: %f accross-top: %f", topic_id, modality_name, token_name, token_importance_within_topic, token_importance_across_topics));
                continue;
            }
            double token_importance = -1;
            if (weight_type.equals("within_topic"))
                token_importance = token_importance_within_topic;
            else if (weight_type.equals("across_topics"))
                token_importance = token_importance_across_topics;
            else{
                logger.error("Undefined weight type: " + weight_type);
                return null;
            }

            if (token_importance < prob_threshold) continue;
            res.get(topic_id).get(modality_name).put(token_name, token_importance);

            //Map<String, Map<String, Map<String, Double>>> res = new HashMap<>();
        }
        return res;
    }


    /** Builds a light weight view of a document object, suitable for visualization
     *
     * @param query
     * @param numChars
     * @return
     * @throws SQLException
     */
    public List<LightDocument> getDocumentVisualizationInformation(String query, int numChars) throws SQLException {
        ResultSet rs = query(query);
        String text = "";
        List<LightDocument> res = new ArrayList<>();

        while (rs.next()) {
            String docid = rs.getString("id");
            String type = rs.getString("doctype");
            String pubyear = rs.getString("pubyear");
            String project = rs.getString("projectAcronym");
            if (project == null || project.isEmpty()) project = rs.getString("project");
            String journal = rs.getString("journal");
            if (type.equals("project_report")) text = rs.getString("abstract");
            else if (type.equals("publication")) {
                text = rs.getString("abstract_pmc");
                if (text == null || text.isEmpty()) text = rs.getString("other_abstract_pmc");
                if (text == null) text = "";
            }
            text = (numChars > text.length()) ? text : text.substring(0, numChars) + "...";
            if (project == null) project = "";
            if (journal == null) journal = "";
            res.add(new LightDocument(docid, type, text, pubyear, journal, project));
        }
        return res;
    }

    /** Rroduces a topic: modality: token:weight mapping
     *
     * @param query
     * @param weight_threshold
     * @return
     * @throws SQLException
     */
    public Map<Integer, Map<String, Double>> getDocumentTopicWeights(String query, double weight_threshold) throws SQLException {
        ResultSet rs = query(query);
        Map<Integer, Map<String, Double>>  res = new HashMap<>();
        while(rs.next()){
            int topic_id = rs.getInt("topicid");
            String document_id = rs.getString("documentid");
            double weight = rs.getDouble("weight");
            if (weight < weight_threshold) continue;
            if (!res.containsKey(topic_id)) res.put(topic_id, new HashMap<>());
            res.get(topic_id).put(document_id, weight);
        }
        return res;
    }

    public void saveDiagnostics(Config config, List<Score> scores){
        Connection connection = null;
        String current_score = "";
        try {

            PreparedStatement bulkInsert = null;
            String sql = "insert into expDiagnostics values(?,?,?,?,?,?);";

            connection.setAutoCommit(false);
            bulkInsert = connection.prepareStatement(sql);
            for(Score sc : scores){
                bulkInsert.setString(1, config.getExperimentId());
                bulkInsert.setString(2, "01");
                bulkInsert.setString(3, sc.getId());
                bulkInsert.setInt(4, sc.getScoreType().ordinal()); //corpus
                bulkInsert.setString(5, sc.getName());
                bulkInsert.setDouble(6, sc.getValue());
                bulkInsert.executeUpdate();

            }
            connection.commit();
            if (bulkInsert != null) {
                bulkInsert.close();
            }
            connection.setAutoCommit(true);

        } catch (SQLException e) {

            logger.error("Exception in save diagnostics score ["+ current_score +"] : "+ e.getMessage());

            if (connection != null) {
                try {
                    logger.error("Transaction is being rolled back \n");
                    connection.rollback();
                } catch (SQLException excep) {
                    logger.error("Error in insert expDiagnostics \n");
                }
            }
        } finally {
        }

    }


    }

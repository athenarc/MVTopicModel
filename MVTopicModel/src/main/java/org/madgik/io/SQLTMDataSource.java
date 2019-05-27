package org.madgik.io;

import cc.mallet.types.Instance;
import org.madgik.config.Config;
import org.madgik.utils.Utils;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for SQL-based data sources
 */
public class SQLTMDataSource extends TMDataSource {

    Connection connection;
    String jdbcString;
    public static final String name = "SQL";

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
            statement = connection.createStatement();
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


    public ArrayList<ArrayList<Instance>> getInputs(Config config){
        boolean D4I = true;
        Config.Net2BoWType PPRenabled = config.getPPRenabled();
        String experimentId = config.getExperimentId();
        String SQLConnection = config.getDataSourceParams();
        Config.ExperimentType experimentType = config.getExperimentType();
        int limitDocs = config.getLimitDocs();
        int numChars = config.getNumChars();
        int numModalities = config.getNumModalities();
        // check for existing serialization
        ArrayList<ArrayList<Instance>> instanceBuffer = null;
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream("serialized.out"));
            instanceBuffer = (ArrayList<ArrayList<Instance>> ) in.readObject();
            logger.info(String.format("Loaded serialized data for experiment %s", experimentId));

            return instanceBuffer;
        }
        catch (FileNotFoundException e){
            logger.debug("No serialized file.");
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                System.exit(-1);
            }
        }


        instanceBuffer = new ArrayList<>(numModalities);
        for (byte m = 0; m < numModalities; m++) instanceBuffer.add(new ArrayList<>());

        Connection connection = null;
        try {

            connection = DriverManager.getConnection(SQLConnection);
            connection.setAutoCommit(false);

            String sql = "";
            String txtsql = "select doctxt_view.docId, text, fulltext from doctxt_view " + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");

            if (experimentType == Config.ExperimentType.PubMed) {
                txtsql = "select doctxt_view.docId, text, fulltext, batchid from doctxt_view where repository = 'PubMed Central' "
                        + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");//+ " LIMIT 10000";
                if (D4I) {
                    txtsql = "select distinct ON (doctxt_view.docId)  doctxt_view.docId, text, fulltext, batchid from doctxt_view \n"
                            + " LEFT JOIN doc_project on doc_project.docid = doctxt_view.docId\n"
                            + "where batchid > '2004' and (doctype='publication' OR doctype='project_report') \n"
                            + "and (repository = 'PubMed Central' OR  doc_project.projectid IN \n"
                            + "(select projectid from doc_project\n"
                            + "join document on doc_project.docid = document.id and repository = 'PubMed Central'\n"
                            + "group by projectid\n"
                            + "having count(*) > 5) )\n"
                            + "Order by doctxt_view.docId \n"
                            + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");//+ " LIMIT 10000";
                }
            }

            if (experimentType == Config.ExperimentType.ACM) {

                if (PPRenabled == Config.Net2BoWType.PPR) {
                    sql = " select  docid,   citations, categories, period, keywords, venue, DBPediaResources from docsideinfo_view  " + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");
                } else if (PPRenabled == Config.Net2BoWType.OneWay) {

                    sql = " select  docid,  citations, categories, keywords, venue, DBPediaResources from docsideinfo_view " + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");
                } else if (PPRenabled == Config.Net2BoWType.TwoWay) {
                    sql = " select  docid, authors, citations, categories, keywords, venue, DBPediaResources from docsideinfo_view " + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");

                }

            } else if (experimentType == Config.ExperimentType.PubMed) {
                sql = " select  docid, keywords, meshterms, dbpediaresources  from docsideinfo_view  where repository = 'PubMed Central' " + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");
                if (D4I) {
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
                }
            }

            logger.info(" Getting text from the database");
            Utils.tic();
            // get txt data
            Statement txtstatement = connection.createStatement();
            txtstatement.setFetchSize(10000);
            ResultSet rstxt = txtstatement.executeQuery(txtsql);
            Utils.toc("Getting text from the database");

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
                logger.info(" Getting side info from the database");
                Utils.tic();
                Statement statement = connection.createStatement();
                statement.setFetchSize(10000);
                ResultSet rs = statement.executeQuery(sql);
                Utils.toc("Getting non-text views data");

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
                                String tmpStr = rs.getString("Categories");//.replace("\t", ",");
                                if (tmpStr != null && !tmpStr.equals("")) {

                                    instanceBuffer.get(2).add(new Instance(tmpStr, null, rs.getString("docid"), "category"));
                                }
                            }

                            if (numModalities > 3) {
                                String tmpAuthorsStr = rs.getString("Venue");//.replace("\t", ",");
                                if (tmpAuthorsStr != null && !tmpAuthorsStr.equals("")) {

                                    instanceBuffer.get(3).add(new Instance(tmpAuthorsStr, null, rs.getString("docid"), "Venue"));
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

//DBPediaResources
                            if (numModalities > 5) {
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
                                    instanceBuffer.get(5).add(new Instance(DBPediaResourceStr, null, rs.getString("docid"), "DBPediaResource"));
                                }
                            }

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
            logger.error(e.getMessage());
            System.exit(1);

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

        // serialize for testing
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream("serialized." + limitDocs + ".out"));
            out.writeObject(instanceBuffer);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return instanceBuffer;

    }


    }

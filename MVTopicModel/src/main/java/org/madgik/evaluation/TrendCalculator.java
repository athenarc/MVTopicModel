package org.madgik.evaluation;

import cc.mallet.types.NormalizedDotProductMetric;
import cc.mallet.types.SparseVector;
import cc.mallet.util.Maths;
import org.apache.commons.lang.StringUtils;
import org.madgik.MVTopicModel.SciTopicFlow;
import org.madgik.config.Config;
import org.madgik.model.TopicVector;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.madgik.MVTopicModel.SciTopicFlow.logger;
import static org.madgik.utils.Utils.cosineSimilarity;

public class TrendCalculator {
    public void setConfig(Config config) {
        this.config = config;
    }

    Config config;
    public void CalcEntityTopicDistributionsAndTrends(String experimentId){

        if (! config.isCalcTopicDistributionsAndTrends()) return;

        // have to seperate input - output
        String SQLConnectionString = config.getInputDataSourceParams();
        Config.ExperimentType experimentType = config.getExperimentType();

        Connection connection = null;
        try {

            connection = DriverManager.getConnection(SQLConnectionString);
            Statement statement = connection.createStatement();

            logger.info("Calc topic Entity Topic Distributions and Trends started");

            String deleteSQL = String.format("Delete from EntityTopicDistribution where ExperimentId= '%s'", experimentId);
            statement.executeUpdate(deleteSQL);

            logger.info("Insert Full Topic Distribution ");

            /**
             * From doc_topic:
             * experiment == current
             * weight: > 0.1
             */

            String SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                    + "select '',  doc_topic.TopicId, '', 'Corpus', round(sum(weight)/SumTopicWeightView.SumWeight, 5) as NormWeight, doc_topic.ExperimentId\n"
                    + "from doc_topic\n"
                    + "INNER JOIN (SELECT  sum(weight) AS SumWeight, ExperimentId\n"
                    + "FROM doc_topic\n"
                    + "Where doc_topic.weight>0.1 \n"
                    + " and doc_topic.ExperimentId='" + experimentId + "'  \n"
                    + "GROUP BY  ExperimentId) SumTopicWeightView on SumTopicWeightView.ExperimentId= doc_topic.ExperimentId\n"
                    + "group By doc_topic.TopicId, doc_topic.ExperimentId, SumTopicWeightView.SumWeight\n"
                    + "Order by  NormWeight Desc";

            statement.executeUpdate(SQLstr);


            if (experimentType == Config.ExperimentType.PubMed) {

                logger.info("Trend Topic distribution for the whole coprus");

                SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                        + "select Document.BatchId,  doc_topic.TopicId, '', 'CorpusTrend', \n"
                        + "round(sum(weight)/SumTopicWeightPerBatchView.BatchSumWeight,5) as NormWeight,  doc_topic.ExperimentId\n"
                        + "from doc_topic\n"
                        + "Inner Join Document on doc_topic.docid= document.id and doc_topic.weight>0.1\n"
                        + "INNER JOIN (SELECT Document.BatchId, sum(weight) AS BatchSumWeight, ExperimentId\n"
                        + "FROM doc_topic\n"
                        + "INNER JOIN Document ON doc_topic.docid= Document.id AND\n"
                        + "doc_topic.weight>0.1\n "
                        + "and doc_topic.ExperimentId='" + experimentId + "'   \n"
                        + "GROUP BY Document.BatchId, ExperimentId) SumTopicWeightPerBatchView on SumTopicWeightPerBatchView.BatchId = Document.BatchId and SumTopicWeightPerBatchView.ExperimentId= doc_topic.ExperimentId\n"
                        + "group By Document.BatchId,SumTopicWeightPerBatchView.BatchSumWeight, doc_topic.TopicId, doc_topic.ExperimentId\n"
                        + "Order by Document.BatchId,   NormWeight Desc";

                statement.executeUpdate(SQLstr);

                logger.info("Project Topic distribution");

                SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                        + "SELECT '', doc_topic.TopicId, Doc_Project.ProjectId,'Project',\n"
                        + "           round(sum(doc_topic.weight) / SumTopicWeightPerProjectView.ProjectSumWeight,5) AS NormWeight,\n"
                        + "             doc_topic.ExperimentId\n"
                        + "      FROM doc_topic\n"
                        + "      INNER JOIN  Doc_Project ON doc_topic.docid = Doc_Project.Docid AND doc_topic.weight > 0.1\n"
                        + "      and  doc_topic.ExperimentId='" + experimentId + "' \n"
                        + "           INNER JOIN (SELECT Doc_Project.ProjectId, sum(weight) AS ProjectSumWeight,    ExperimentId\n"
                        + "           FROM doc_topic\n"
                        + "           INNER JOIN   Doc_Project ON doc_topic.docid = Doc_Project.docid AND  doc_topic.weight > 0.1\n"
                        + "           GROUP BY  ExperimentId,Doc_Project.ProjectId)\n"
                        + "           SumTopicWeightPerProjectView ON SumTopicWeightPerProjectView.ProjectId = Doc_Project.ProjectId AND \n"
                        + "                                           SumTopicWeightPerProjectView.ExperimentId = doc_topic.ExperimentId                                            \n"
                        + "     GROUP BY Doc_Project.ProjectId,\n"
                        + "              SumTopicWeightPerProjectView.ProjectSumWeight,\n"
                        + "              doc_topic.TopicId,\n"
                        + "              doc_topic.ExperimentId\n"
                        + "              order by  doc_topic.ExperimentId, Doc_Project.ProjectId, NormWeight Desc,doc_topic.ExperimentId";

                statement.executeUpdate(SQLstr);

                logger.info("Funder Topic distribution");
                SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                        + " SELECT '', doc_topic.TopicId, doc_funder_view.funder,'Funder',\n"
                        + "                               round(sum(doc_topic.weight) / SumTopicWeightPerProjectView.ProjectSumWeight,5) AS NormWeight,\n"
                        + "                                 doc_topic.ExperimentId\n"
                        + "                          FROM doc_topic\n"
                        + "                          INNER JOIN  doc_funder_view ON doc_topic.docid = doc_funder_view.docid AND doc_topic.weight > 0.1\n"
                        + "                          and  doc_topic.ExperimentId='" + experimentId + "' \n"
                        + "                        \n"
                        + "                               INNER JOIN (SELECT doc_funder_view.funder, sum(weight) AS ProjectSumWeight,    ExperimentId\n"
                        + "                               FROM doc_topic\n"
                        + "                               INNER JOIN   doc_funder_view ON doc_topic.docid = doc_funder_view.docid AND  doc_topic.weight > 0.1\n"
                        + "                               \n"
                        + "                               GROUP BY  ExperimentId,doc_funder_view.funder)\n"
                        + "                               SumTopicWeightPerProjectView ON SumTopicWeightPerProjectView.funder = doc_funder_view.funder AND \n"
                        + "                                                               SumTopicWeightPerProjectView.ExperimentId = doc_topic.ExperimentId                                            \n"
                        + "                         GROUP BY doc_funder_view.funder,\n"
                        + "                                  SumTopicWeightPerProjectView.ProjectSumWeight,\n"
                        + "                                  doc_topic.TopicId,\n"
                        + "                                  doc_topic.ExperimentId\n"
                        + "                                  order by  doc_topic.ExperimentId, doc_funder_view.funder, NormWeight Desc,doc_topic.ExperimentId";

                statement.executeUpdate(SQLstr);

                logger.info("Funder Trend Topic distribution");

                SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                        + "SELECT Document.batchId, doc_topic.TopicId, doc_funder_view.funder,'FunderTrend',\n"
                        + "round(sum(doc_topic.weight) / SumTopicWeightPerProjectView.ProjectSumWeight,5) AS NormWeight, doc_topic.ExperimentId\n"
                        + "FROM doc_topic\n"
                        + "INNER JOIN Document on doc_topic.docid= document.id and doc_topic.weight>0.1\n"
                        + " and  doc_topic.ExperimentId='" + experimentId + "' \n"
                        + "INNER JOIN  doc_funder_view ON doc_topic.docid = doc_funder_view.docid                           \n"
                        + "INNER JOIN (SELECT doc_funder_view.funder, document.batchId, sum(weight) AS ProjectSumWeight,    ExperimentId\n"
                        + "FROM doc_topic\n"
                        + "Inner Join Document on doc_topic.docid= document.id and doc_topic.weight>0.1           \n"
                        + "INNER JOIN   doc_funder_view ON doc_topic.docid = doc_funder_view.docid                                \n"
                        + "GROUP BY  doc_funder_view.funder,Document.batchId,ExperimentId) SumTopicWeightPerProjectView \n"
                        + "ON SumTopicWeightPerProjectView.funder = doc_funder_view.funder AND SumTopicWeightPerProjectView.ExperimentId = doc_topic.ExperimentId  AND SumTopicWeightPerProjectView.batchId = Document.batchId\n"
                        + "GROUP BY doc_funder_view.funder,Document.batchId, SumTopicWeightPerProjectView.ProjectSumWeight,doc_topic.TopicId,doc_topic.ExperimentId\n"
                        + "order by  doc_topic.ExperimentId, doc_funder_view.funder, NormWeight Desc,doc_topic.ExperimentId";

                statement.executeUpdate(SQLstr);
            }
            logger.info("Entity and trends topic distribution finished");

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
        logger.info("Topic similarities calculation finished");
    }


    public void calcSimilarities(String experimentId) {

        if (!config.isCalcEntitySimilarities()) return;

        String SQLConnectionString = config.getOutputDataSourceParams();
        //calc similarities
        Config.ExperimentType experimentType = config.getExperimentType();
        int numTopics = config.getNumTopics();
        boolean ACMAuthorSimilarity = config.isACMAuthorSimilarity();
        Config.SimilarityType similarityType = config.getSimilarityType();

        logger.info("similarities calculation Started");

        Connection connection = null;
        try {
            // create a database connection
            //connection = DriverManager.getConnection(SQLConnectionString);
            connection = DriverManager.getConnection(SQLConnectionString);
            Statement statement = connection.createStatement();

            String sql = "";
            String entityType = "";
            switch (experimentType) {

                case PubMed:
                    entityType = "Project";
                    sql = "select EntityTopicDistribution.EntityId as projectId, EntityTopicDistribution.TopicId, EntityTopicDistribution.NormWeight as Weight \n"
                            + "                                                        from EntityTopicDistribution                                                        \n"
                            + "                                                        where EntityTopicDistribution.EntityType='Project' \n"
                            + "                                                        AND EntityTopicDistribution.experimentId= '" + experimentId + "'    \n"
                            + "                                                        AND EntityTopicDistribution.EntityId<>'' and EntityTopicDistribution.NormWeight>0.03\n"
                            + "                                                        and EntityTopicDistribution.EntityId in (Select ProjectId FROM Doc_Project GROUP BY ProjectId HAVING Count(*)>4)\n";

                    break;
                default:
            }

            // String sql = "select fundedarxiv.file from fundedarxiv inner join funds on file=filename Group By fundedarxiv.file LIMIT 10" ;
            ResultSet rs = statement.executeQuery(sql);

            HashMap<String, SparseVector> labelVectors = null;
            HashMap<String, double[]> similarityVectors = null;
            if (similarityType == Config.SimilarityType.cos) {
                labelVectors = new HashMap<String, SparseVector>();
            } else {
                similarityVectors = new HashMap<String, double[]>();
            }

            String labelId = "";
            int[] topics = new int[numTopics];
            double[] weights = new double[numTopics];
            int cnt = 0;
            double a;
            while (rs.next()) {

                String newLabelId = "";

                switch (experimentType) {

                    case PubMed:
                        newLabelId = rs.getString("projectId");
                        break;
                    default:
                }

                if (!newLabelId.equals(labelId) && !labelId.isEmpty()) {
                    if (similarityType == Config.SimilarityType.cos) {
                        labelVectors.put(labelId, new SparseVector(topics, weights, topics.length, topics.length, true, true, true));
                    } else {
                        similarityVectors.put(labelId, weights);
                    }
                    topics = new int[numTopics];
                    weights = new double[numTopics];
                    cnt = 0;
                }
                labelId = newLabelId;
                topics[cnt] = rs.getInt("TopicId");
                weights[cnt] = rs.getDouble("Weight");
                cnt++;

            }

            cnt = 0;
            double similarity = 0;
            double similarityThreshold = 0.15;
            NormalizedDotProductMetric cosineSimilarity = new NormalizedDotProductMetric();

            //statement.executeUpdate("create table if not exists EntitySimilarity (EntityType int, EntityId1 nvarchar(50), EntityId2 nvarchar(50), Similarity double, ExperimentId nvarchar(50)) ");
            String deleteSQL = String.format("Delete from EntitySimilarity where  ExperimentId = '%s' and entityType='%s'", experimentId, entityType);
            statement.executeUpdate(deleteSQL);

            PreparedStatement bulkInsert = null;
            sql = "insert into EntitySimilarity values(?,?,?,?,?);";

            try {

                connection.setAutoCommit(false);
                bulkInsert = connection.prepareStatement(sql);

                if (similarityType == Config.SimilarityType.Jen_Sha_Div) {
                    for (String fromGrantId : similarityVectors.keySet()) {
                        boolean startCalc = false;

                        for (String toGrantId : similarityVectors.keySet()) {
                            if (!fromGrantId.equals(toGrantId) && !startCalc) {
                                continue;
                            } else {
                                startCalc = true;
                                similarity = Maths.jensenShannonDivergence(similarityVectors.get(fromGrantId), similarityVectors.get(toGrantId)); // the function returns distance not similarity
                                if (similarity > similarityThreshold && !fromGrantId.equals(toGrantId)) {

                                    bulkInsert.setString(1, entityType);
                                    bulkInsert.setString(2, fromGrantId);
                                    bulkInsert.setString(3, toGrantId);
                                    bulkInsert.setDouble(4, (double) Math.round(similarity * 1000) / 1000);
                                    bulkInsert.setString(5, experimentId);
                                    bulkInsert.executeUpdate();
                                }
                            }
                        }
                    }
                } else if (similarityType == Config.SimilarityType.cos) {
                    for (String fromGrantId : labelVectors.keySet()) {
                        boolean startCalc = false;

                        for (String toGrantId : labelVectors.keySet()) {
                            if (!fromGrantId.equals(toGrantId) && !startCalc) {
                                continue;
                            } else {
                                startCalc = true;
                                similarity = 1 - Math.abs(cosineSimilarity.distance(labelVectors.get(fromGrantId), labelVectors.get(toGrantId))); // the function returns distance not similarity
                                if (similarity > similarityThreshold && !fromGrantId.equals(toGrantId)) {
                                    bulkInsert.setString(1, entityType);
                                    bulkInsert.setString(2, fromGrantId);
                                    bulkInsert.setString(3, toGrantId);
                                    bulkInsert.setDouble(4, (double) Math.round(similarity * 1000) / 1000);
                                    bulkInsert.setString(5, experimentId);
                                    bulkInsert.executeUpdate();
                                }
                            }
                        }
                    }
                }
                connection.commit();
            } catch (SQLException e) {

                if (connection != null) {
                    try {
                        logger.error("Transaction is being rolled back");
                        connection.rollback();
                    } catch (SQLException excep) {
                        logger.error("Error in insert grantSimilarity");
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
        logger.info("similarities calculation finished");
    }


    public void CalcTopicSimilarities(String experimentId) {

        if (!config.isCalcTopicSimilarities()) return;

        String SQLConnectionString = config.getOutputDataSourceParams();

        Connection connection = null;
        try {

            connection = DriverManager.getConnection(SQLConnectionString);
            Statement statement = connection.createStatement();

            logger.info("Calc topic similarities started");

            String distinctTopicsSQL = "Select  TopicId,  ExperimentId, count(*) as cnt\n"
                    + "from TopicVector\n  "
                    + (StringUtils.isBlank(experimentId) ? "" : String.format("where experimentId = '%s' \n  ", experimentId))
                    + "group by TopicId,  ExperimentId";

            ResultSet rs = statement.executeQuery(distinctTopicsSQL);

            List<TopicVector> topicVectors = new ArrayList<>();

            while (rs.next()) {

                TopicVector topicVector = new TopicVector();

                topicVector.ExperimentId = rs.getString("ExperimentId");
                topicVector.TopicId = rs.getInt("TopicId");
                //String newLabelId = experimentId + "_" + topicId;
                int dimension = rs.getInt("cnt");
                topicVector.Vector = new double[dimension];

                String selectVectorSQL = String.format("Select Weight from topicVector where ExperimentId= '%s'  and TopicId=%d order by ColumnId", topicVector.ExperimentId, topicVector.TopicId);

                Statement statement2 = connection.createStatement();
                ResultSet rs1 = statement2.executeQuery(selectVectorSQL);
                int cnt = 0;
                while (rs1.next()) {
                    topicVector.Vector[cnt++] = rs1.getDouble("Weight");
                }

                topicVectors.add(topicVector);

            }

            double similarity = 0;
            double similarityThreshold = 0.3;

            statement.executeUpdate("create table if not exists TopicSimilarity (ExperimentId1 TEXT, TopicId1 TEXT, ExperimentId2 TEXT, TopicId2 TEXT, Similarity double) ");
            String deleteSQL = String.format("Delete from TopicSimilarity");
            statement.executeUpdate(deleteSQL);

            PreparedStatement bulkInsert = null;
            String insertSql = "insert into TopicSimilarity values(?,?,?,?,?);";

            try {

                connection.setAutoCommit(false);
                bulkInsert = connection.prepareStatement(insertSql);

                for (int t1 = 0; t1 < topicVectors.size(); t1++) {
                    for (int t2 = t1; t2 < topicVectors.size(); t2++) {

                        similarity = Math.max(cosineSimilarity(topicVectors.get(t1).Vector, topicVectors.get(t2).Vector), 0);

                        if (similarity > similarityThreshold && !(topicVectors.get(t1).TopicId == topicVectors.get(t2).TopicId && topicVectors.get(t1).ExperimentId == topicVectors.get(t2).ExperimentId)) {

                            bulkInsert.setString(1, topicVectors.get(t1).ExperimentId);
                            bulkInsert.setInt(2, topicVectors.get(t1).TopicId);
                            bulkInsert.setString(3, topicVectors.get(t2).ExperimentId);
                            bulkInsert.setInt(4, topicVectors.get(t2).TopicId);
                            bulkInsert.setDouble(5, (double) Math.round(similarity * 1000) / 1000);

                            bulkInsert.executeUpdate();
                        }
                    }
                }

                connection.commit();

            } catch (SQLException e) {

                if (connection != null) {
                    try {
                        logger.error("Transaction is being rolled back");
                        connection.rollback();
                    } catch (SQLException excep) {
                        logger.error("Error in insert grantSimilarity");
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

        logger.info("Topic similarities calculation finished");
    }

    public void calcPPRSimilarities() {

        if (!config.isCalcPPRSimilarities()) return;
        String SQLConnectionString = config.getOutputDataSourceParams();
        //calc similarities

        //logger.info("PPRSimilarities calculation Started");
        Connection connection = null;
        try {
            // create a database connection
            //connection = DriverManager.getConnection(SQLConnectionString);
            connection = DriverManager.getConnection(SQLConnectionString);
            Statement statement = connection.createStatement();

            logger.info("PPRSimilarities calculation Started");

            String sql = "SELECT source.OrigId||'PPR' AS PubID, target.OrigId  AS CitationId, prLinks.Counts As Counts FROM prLinks\n"
                    + "INNER JOIN PubCitationPPRAlias source ON source.RowId = PrLinks.Source\n"
                    + "INNER JOIN PubCitationPPRAlias target ON target.RowId = PrLinks.Target\n"
                    + "Union\n"
                    + "Select Doc_id, CitationId, 1 as Counts From PubCitation\n"
                    + "ORDER by Doc_id ";

            ResultSet rs = statement.executeQuery(sql);

            HashMap<String, SparseVector> labelVectors = null;
            //HashMap<String, double[]> similarityVectors = null;
            labelVectors = new HashMap<String, SparseVector>();

            String labelId = "";

            int[] citations = new int[350];
            double[] weights = new double[350];
            int cnt = 0;

            while (rs.next()) {

                String newLabelId = "";
                newLabelId = rs.getString("Doc_id");
                if (!newLabelId.equals(labelId) && !labelId.isEmpty()) {
                    labelVectors.put(labelId, new SparseVector(citations, weights, citations.length, citations.length, true, true, true));
                    citations = new int[350];
                    weights = new double[350];
                    cnt = 0;
                }
                labelId = newLabelId;
                citations[cnt] = rs.getInt("CitationId");
                weights[cnt] = rs.getDouble("Counts");
                cnt++;

            }

            cnt = 0;
            double similarity = 0;

            NormalizedDotProductMetric cosineSimilarity = new NormalizedDotProductMetric();

            statement.executeUpdate("create table if not exists PPRPubCitationSimilarity (Doc_id TEXT,  Similarity double) ");
            String deleteSQL = String.format("Delete from PPRPubCitationSimilarity");
            statement.executeUpdate(deleteSQL);

            PreparedStatement bulkInsert = null;
            sql = "insert into PPRPubCitationSimilarity values(?,?);";

            try {

                connection.setAutoCommit(false);
                bulkInsert = connection.prepareStatement(sql);

                for (String fromDoc_id : labelVectors.keySet()) {

                    if (fromDoc_id.contains("PPR")) {
                        continue;
                    }
                    String toDoc_id = fromDoc_id + "PPR";
                    similarity = -1;

                    if (labelVectors.get(fromDoc_id) != null && labelVectors.get(toDoc_id) != null) {
                        similarity = 1 - Math.abs(cosineSimilarity.distance(labelVectors.get(fromDoc_id), labelVectors.get(toDoc_id))); // the function returns distance not similarity
                    }
                    bulkInsert.setString(1, fromDoc_id);
                    bulkInsert.setDouble(2, (double) Math.round(similarity * 1000) / 1000);

                    bulkInsert.executeUpdate();

                }

                connection.commit();

            } catch (SQLException e) {

                if (connection != null) {
                    try {
                        logger.error("Transaction is being rolled back");
                        connection.rollback();
                    } catch (SQLException excep) {
                        logger.error("Error in insert grantSimilarity");
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

        logger.info("Pub citation similarities calculation finished");
    }

    /**
     * Perform post-topic modelling analysis as per the configuration flags
     * @param experimentId
     */
    public void doPostAnalysis(String experimentId){

        System.out.println("Post-analysis is TODO!");
        if (true)
            return;

        this.CalcEntityTopicDistributionsAndTrends(experimentId);
        this.calcSimilarities(experimentId);
        this.CalcTopicSimilarities(experimentId);
        this.calcPPRSimilarities();
    }

    public static void main(String[] args) {
        String SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                    + "select '',  doc_topic.TopicId, '', 'Corpus', round(sum(weight)/SumTopicWeightView.SumWeight, 5) as NormWeight, doc_topic.ExperimentId\n"
                    + "from doc_topic\n"
                    + "INNER JOIN (SELECT  sum(weight) AS SumWeight, ExperimentId\n"
                    + "FROM doc_topic\n"
                    + "Where doc_topic.weight>0.1 \n"
                    + " and doc_topic.ExperimentId='" + "PubMed_500T_550IT_7000CHRs_4M_OneWay" + "'  \n"
                    + "GROUP BY  ExperimentId) SumTopicWeightView on SumTopicWeightView.ExperimentId= doc_topic.ExperimentId\n"
                    + "group By doc_topic.TopicId, doc_topic.ExperimentId, SumTopicWeightView.SumWeight\n"
                    + "Order by  NormWeight Desc";
        System.out.println(SQLstr);
    }
}

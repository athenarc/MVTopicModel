package org.madgik.evaluation;

import cc.mallet.types.NormalizedDotProductMetric;
import cc.mallet.types.SparseVector;
import cc.mallet.util.Maths;
import org.apache.commons.lang.StringUtils;
import org.madgik.MVTopicModel.SciTopicFlow;
import org.madgik.model.TopicVector;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.madgik.MVTopicModel.SciTopicFlow.logger;
import static org.madgik.utils.Utils.cosineSimilarity;

public class TrendCalculator {


    public static void CalcEntityTopicDistributionsAndTrends(String SQLConnectionString, String experimentId, SciTopicFlow.ExperimentType experimentType) throws SQLException {
        Connection connection = null;
        try {

            connection = DriverManager.getConnection(SQLConnectionString);
            Statement statement = connection.createStatement();

            logger.info("Calc topic Entity Topic Distributions and Trends started");

            String deleteSQL = String.format("Delete from EntityTopicDistribution where ExperimentId= '%s'", experimentId);
            statement.executeUpdate(deleteSQL);

            logger.info("Insert Full Topic Distribution ");

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
            //statement.executeUpdate(SQLstr);

            if (experimentType == SciTopicFlow.ExperimentType.ACM) {

                logger.info("Trend Topic distribution for the whole coprus");

                SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                        + "select Document.BatchId,  doc_topic.TopicId, '', 'CorpusTrend', \n"
                        + "round(sum(weight)/SumTopicWeightPerBatchView.BatchSumWeight,5) as NormWeight,  doc_topic.ExperimentId\n"
                        + "from doc_topic\n"
                        + "Inner Join Document on doc_topic.docid= document.docid and doc_topic.weight>0.1\n"
                        + "INNER JOIN (SELECT Document.BatchId, sum(weight) AS BatchSumWeight, ExperimentId\n"
                        + "FROM doc_topic\n"
                        + "INNER JOIN Document ON doc_topic.docid= Document.docid AND\n"
                        + "doc_topic.weight>0.1\n "
                        + "and doc_topic.ExperimentId='" + experimentId + "'   \n"
                        + "GROUP BY Document.BatchId, ExperimentId) SumTopicWeightPerBatchView on SumTopicWeightPerBatchView.BatchId = Document.BatchId and SumTopicWeightPerBatchView.ExperimentId= doc_topic.ExperimentId\n"
                        + "group By Document.BatchId,SumTopicWeightPerBatchView.BatchSumWeight, doc_topic.TopicId, doc_topic.ExperimentId\n"
                        + "Order by Document.BatchId,   NormWeight Desc";

                logger.info("Author Topic distribution");

                SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                        + "SELECT '', doc_topic.TopicId, Doc_author.AuthorId,'Author',\n"
                        + "                              round(sum(doc_topic.weight) / SumTopicWeightPerProjectView.ProjectSumWeight,5) AS NormWeight,\n"
                        + "                                doc_topic.ExperimentId\n"
                        + "                         FROM doc_topic\n"
                        + "                         INNER JOIN  Doc_author ON doc_topic.Docid = Doc_author.Docid AND doc_topic.weight > 0.1\n"
                        + "      and  doc_topic.ExperimentId='" + experimentId + "' \n"
                        + "                              INNER JOIN (SELECT Doc_author.authorid, sum(weight) AS ProjectSumWeight,    ExperimentId\n"
                        + "                              FROM doc_topic\n"
                        + "                              INNER JOIN   Doc_author ON doc_topic.Docid = Doc_author.Docid AND  doc_topic.weight > 0.1\n"
                        + "                              GROUP BY  ExperimentId,Doc_author.AuthorId)\n"
                        + "                              SumTopicWeightPerProjectView ON SumTopicWeightPerProjectView.AuthorId = Doc_author.AuthorId AND \n"
                        + "                                                              SumTopicWeightPerProjectView.ExperimentId = doc_topic.ExperimentId                                        \n"
                        + "                        GROUP BY Doc_author.AuthorId,\n"
                        + "                                 SumTopicWeightPerProjectView.ProjectSumWeight,\n"
                        + "                                 doc_topic.TopicId,\n"
                        + "                                 doc_topic.ExperimentId\n"
                        + "                                 order by  doc_topic.ExperimentId, Doc_author.AuthorId, NormWeight Desc,doc_topic.ExperimentId";

                statement.executeUpdate(SQLstr);

                logger.info("Journal Topic distribution");

                SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                        + "SELECT '', doc_topic.TopicId, Doc_journal.issn,'Journal',\n"
                        + "round(sum(doc_topic.weight) / SumTopicWeightPerProjectView.ProjectSumWeight,5) AS NormWeight,\n"
                        + "doc_topic.ExperimentId\n"
                        + "FROM doc_topic\n"
                        + "INNER JOIN  doc_journal ON doc_topic.Docid = doc_journal.Docid AND doc_topic.weight > 0.1\n"
                        + " and  doc_topic.ExperimentId='" + experimentId + "' \n"
                        + "INNER JOIN (SELECT doc_journal.issn, sum(weight) AS ProjectSumWeight,    ExperimentId\n"
                        + "FROM doc_topic\n"
                        + "INNER JOIN   doc_journal ON doc_topic.Docid = doc_journal.Docid AND  doc_topic.weight > 0.1\n"
                        + "GROUP BY  ExperimentId,doc_journal.issn) SumTopicWeightPerProjectView ON SumTopicWeightPerProjectView.issn = doc_journal.issn AND SumTopicWeightPerProjectView.ExperimentId = doc_topic.ExperimentId                                        \n"
                        + "GROUP BY doc_journal.issn,SumTopicWeightPerProjectView.ProjectSumWeight,doc_topic.TopicId, doc_topic.ExperimentId\n"
                        + "order by  doc_topic.ExperimentId, doc_journal.issn, NormWeight Desc,doc_topic.ExperimentId";

                statement.executeUpdate(SQLstr);

                logger.info("Journal Trend Topic distribution");

                SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                        + "SELECT Document.batchId, doc_topic.TopicId, doc_journal.issn,'JournalTrend',\n"
                        + "round(sum(doc_topic.weight) / SumTopicWeightPerProjectView.ProjectSumWeight,5) AS NormWeight, doc_topic.ExperimentId\n"
                        + "FROM doc_topic\n"
                        + "INNER JOIN Document on doc_topic.docid= document.docid and doc_topic.weight>0.1\n"
                        + " and  doc_topic.ExperimentId='" + experimentId + "' \n"
                        + "INNER JOIN  doc_journal ON doc_topic.docid = doc_journal.docid                           \n"
                        + "INNER JOIN (SELECT doc_journal.issn, document.batchId, sum(weight) AS ProjectSumWeight,    ExperimentId\n"
                        + "FROM doc_topic\n"
                        + "Inner Join Document on doc_topic.docid= document.docid and doc_topic.weight>0.1           \n"
                        + "INNER JOIN   doc_journal ON doc_topic.docid = doc_journal.docid                                \n"
                        + "GROUP BY  doc_journal.issn,Document.batchId,ExperimentId) SumTopicWeightPerProjectView \n"
                        + "ON SumTopicWeightPerProjectView.issn = doc_journal.issn AND SumTopicWeightPerProjectView.ExperimentId = doc_topic.ExperimentId  AND SumTopicWeightPerProjectView.batchId = Document.batchId\n"
                        + "GROUP BY doc_journal.issn,Document.batchId, SumTopicWeightPerProjectView.ProjectSumWeight,doc_topic.TopicId,doc_topic.ExperimentId\n"
                        + "order by  doc_topic.ExperimentId, doc_journal.issn, NormWeight Desc,doc_topic.ExperimentId";

                statement.executeUpdate(SQLstr);

                logger.info("Conference Topic distribution");

                SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                        + "SELECT '', doc_topic.TopicId, doc_conference.acronymBase,'Conference',\n"
                        + "round(sum(doc_topic.weight) / SumTopicWeightPerProjectView.ProjectSumWeight,5) AS NormWeight,\n"
                        + "doc_topic.ExperimentId\n"
                        + "FROM doc_topic\n"
                        + "INNER JOIN  doc_conference ON doc_topic.Docid = doc_conference.Docid AND doc_topic.weight > 0.1\n"
                        + " and  doc_topic.ExperimentId='" + experimentId + "' \n"
                        + "INNER JOIN (SELECT doc_conference.acronymBase, sum(weight) AS ProjectSumWeight,    ExperimentId\n"
                        + "FROM doc_topic\n"
                        + "INNER JOIN   doc_conference ON doc_topic.Docid = doc_conference.Docid AND  doc_topic.weight > 0.1\n"
                        + "GROUP BY  ExperimentId,doc_conference.acronymBase) SumTopicWeightPerProjectView ON SumTopicWeightPerProjectView.acronymBase = doc_conference.acronymBase AND SumTopicWeightPerProjectView.ExperimentId = doc_topic.ExperimentId                                        \n"
                        + "GROUP BY doc_conference.acronymBase,SumTopicWeightPerProjectView.ProjectSumWeight,doc_topic.TopicId, doc_topic.ExperimentId\n"
                        + "order by  doc_topic.ExperimentId, doc_conference.acronymBase, NormWeight Desc,doc_topic.ExperimentId";

                statement.executeUpdate(SQLstr);

                logger.info("Conference Trend Topic distribution");

                SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                        + " SELECT Document.batchId, doc_topic.TopicId, doc_conference.acronymBase,'ConferenceTrend',\n"
                        + "round(sum(doc_topic.weight) / SumTopicWeightPerProjectView.ProjectSumWeight,5) AS NormWeight, doc_topic.ExperimentId\n"
                        + "FROM doc_topic\n"
                        + "INNER JOIN Document on doc_topic.docid= document.docid and doc_topic.weight>0.1\n"
                        + " and  doc_topic.ExperimentId='" + experimentId + "' \n"
                        + "INNER JOIN  doc_conference ON doc_topic.docid = doc_conference.docid                           \n"
                        + "INNER JOIN (SELECT doc_conference.acronymBase, document.batchId, sum(weight) AS ProjectSumWeight,    ExperimentId\n"
                        + "FROM doc_topic\n"
                        + "Inner Join Document on doc_topic.docid= document.docid and doc_topic.weight>0.1           \n"
                        + "INNER JOIN   doc_conference ON doc_topic.docid = doc_conference.docid                                \n"
                        + "GROUP BY  doc_conference.acronymBase,Document.batchId,ExperimentId) SumTopicWeightPerProjectView \n"
                        + "ON SumTopicWeightPerProjectView.acronymBase = doc_conference.acronymBase AND SumTopicWeightPerProjectView.ExperimentId = doc_topic.ExperimentId  AND SumTopicWeightPerProjectView.batchId = Document.batchId\n"
                        + "GROUP BY doc_conference.acronymBase,Document.batchId, SumTopicWeightPerProjectView.ProjectSumWeight,doc_topic.TopicId,doc_topic.ExperimentId\n"
                        + "order by  doc_topic.ExperimentId, doc_conference.acronymBase, NormWeight Desc,doc_topic.ExperimentId";

                statement.executeUpdate(SQLstr);

            }

            if (experimentType == SciTopicFlow.ExperimentType.PubMed) {

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


    public static void calcSimilarities(String SQLConnectionString, SciTopicFlow.ExperimentType experimentType, String experimentId, boolean ACMAuthorSimilarity, SciTopicFlow.SimilarityType similarityType, int numTopics) {
        //calc similarities

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
                case ACM:
                    if (ACMAuthorSimilarity) {
                        entityType = "Author";
                        sql = "select EntityTopicDistribution.EntityId as authorId, EntityTopicDistribution.TopicId, EntityTopicDistribution.NormWeight as Weight \n"
                                + "                            from EntityTopicDistribution\n"
                                + "                            where EntityTopicDistribution.EntityType='Author' AND EntityTopicDistribution.EntityId<>'' AND\n"
                                + "                            EntityTopicDistribution.experimentId= '" + experimentId + "'   and EntityTopicDistribution.NormWeight>0.03\n"
                                + "                            and EntityTopicDistribution.EntityId in (Select AuthorId FROM doc_author GROUP BY AuthorId HAVING Count(*)>4)\n"
                                + "                            and EntityTopicDistribution.topicid in (select TopicId from topic \n"
                                + "                            where topic.experimentId='" + experimentId + "' and topic.VisibilityIndex>0)";

                    } else {
                        entityType = "JournalConference";
                        sql = "select EntityTopicDistribution.EntityId as VenueId, EntityTopicDistribution.TopicId  as TopicId, EntityTopicDistribution.NormWeight as Weight \n"
                                + "                                                          from EntityTopicDistribution\n"
                                + "                                                          where EntityTopicDistribution.EntityType='Journal' AND EntityTopicDistribution.EntityId<>'' AND\n"
                                + "                                                          EntityTopicDistribution.experimentId= '" + experimentId + "'   and EntityTopicDistribution.NormWeight>0.03\n"
                                + "                                                          and EntityTopicDistribution.EntityId in (Select ISSN FROM doc_journal GROUP BY ISSN HAVING Count(*)>100)\n"
                                + "                                                          and EntityTopicDistribution.topicid in (select TopicId from topicdescription \n"
                                + "                                                          where topicdescription.experimentId='" + experimentId + "' and topicdescription.VisibilityIndex=1)\n"
                                + "          UNION                                                \n"
                                + "select EntityTopicDistribution.EntityId as VenueId, EntityTopicDistribution.TopicId as TopicId, EntityTopicDistribution.NormWeight as Weight \n"
                                + "                                                          from EntityTopicDistribution\n"
                                + "                                                          where EntityTopicDistribution.EntityType='Conference' AND EntityTopicDistribution.EntityId<>'' AND\n"
                                + "                                                          EntityTopicDistribution.experimentId= '" + experimentId + "'   and EntityTopicDistribution.NormWeight>0.03\n"
                                + "                                                          and EntityTopicDistribution.EntityId in (Select SeriesId FROM doc_conference GROUP BY SeriesId HAVING Count(*)>400)\n"
                                + "                                                          and EntityTopicDistribution.topicid in (select TopicId from topicdescription \n"
                                + "                                                          where topicdescription.experimentId='" + experimentId + "' and topicdescription.VisibilityIndex=1)";
                    }

                    break;
                default:
            }

            // String sql = "select fundedarxiv.file from fundedarxiv inner join funds on file=filename Group By fundedarxiv.file LIMIT 10" ;
            ResultSet rs = statement.executeQuery(sql);

            HashMap<String, SparseVector> labelVectors = null;
            HashMap<String, double[]> similarityVectors = null;
            if (similarityType == SciTopicFlow.SimilarityType.cos) {
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

                    case ACM:
                        if (ACMAuthorSimilarity) {
                            newLabelId = rs.getString("AuthorId");
                        } else {
                            newLabelId = rs.getString("VenueId");
                        }
                        break;

                    default:
                }

                if (!newLabelId.equals(labelId) && !labelId.isEmpty()) {
                    if (similarityType == SciTopicFlow.SimilarityType.cos) {
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

                if (similarityType == SciTopicFlow.SimilarityType.Jen_Sha_Div) {
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
                } else if (similarityType == SciTopicFlow.SimilarityType.cos) {
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


    public static void CalcTopicSimilarities(String SQLConnectionString, String experimentId) {

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

    public static void calcPPRSimilarities(String SQLConnectionString) {
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


}

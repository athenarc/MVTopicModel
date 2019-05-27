package org.madgik.preproc;

import com.sree.textbytes.jtopia.Configuration;
import com.sree.textbytes.jtopia.TermDocument;
import com.sree.textbytes.jtopia.TermsExtractor;
import org.apache.log4j.Logger;
import org.madgik.MVTopicModel.SciTopicFlow;
import org.madgik.config.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class KeywordExtractor {


    Logger logger = Logger.getLogger(SciTopicFlow.LOGGER);

    public void FindKeyPhrasesPerTopic(Map<Integer, String> topicIdsTitles, Config config) {
        String experimentId = config.getExperimentId();
        String tagger = config.getTagger();
        //for default lexicon POS tags
        //Configuration.setTaggerType("default");
        if (tagger == "openNLP") {
            // for openNLP POS tagger
            Configuration.setTaggerType(tagger);
            //for Stanford POS tagger
            // if tagger type is "openNLP" then give the openNLP POS tagger path
            Configuration.setModelFileLocation("model/openNLP/en-pos-maxent.bin");
        } else if (tagger == "stanford") {
            Configuration.setTaggerType("stanford");
            Configuration.setModelFileLocation("model/stanford/english-left3words-distsim.tagger");

        }

        Configuration.setSingleStrength(4);
        Configuration.setNoLimitStrength(2);
        // if tagger type is "default" then give the default POS lexicon file
        //Configuration.setModelFileLocation("model/default/english-lexicon.txt");
        // if tagger type is "stanford "
        //Configuration.setModelFileLocation("model/stanford/english-left3words-distsim.tagger");

        TermsExtractor termExtractor = new TermsExtractor();
        TermDocument topiaDoc = new TermDocument();
        StringBuffer stringBuffer = new StringBuffer();


        HashMap<Integer, Map<String, ArrayList<Integer>>> topicTitles = null;
        topicTitles = new HashMap<>();
        Integer topicId = -1;

        for (int newTopicId : topicIdsTitles.keySet()) {
            String title = topicIdsTitles.get(newTopicId);

            if (newTopicId != topicId && topicId != -1) {
                logger.info("Finding key phrases for topic " + topicId);
                topiaDoc = termExtractor.extractTerms(stringBuffer.toString());
                topicTitles.put(topicId, topiaDoc.getFinalFilteredTerms());
                stringBuffer = new StringBuffer();
            }
            stringBuffer.append(title.replace('-', ' ').toLowerCase() + "\n");
            //stringBuffer.append(rs.getString("abstract").replace('-', ' ').toLowerCase() + "\n");
            topicId = newTopicId;

        }

        logger.info("Finding key phrases for topic " + topicId);
        topiaDoc = termExtractor.extractTerms(stringBuffer.toString());
        topicTitles.put(topicId, topiaDoc.getFinalFilteredTerms());

        logger.info("Finding Key phrases finished");
    }
}

//            statement.executeUpdate("create table if not exists TopicKeyPhrase ( TopicId Integer, Tagger TEXT, Phrase Text, Count Integer, WordsNum Integer, Weight double, ExperimentId TEXT) ");
//            String deleteSQL = String.format("Delete from TopicKeyPhrase WHERE ExperimentId='" + experimentId + "' AND Tagger ='" + tagger + "'");
//            statement.executeUpdate(deleteSQL);
//
//            PreparedStatement bulkInsert = null;
//            sql = "insert into TopicKeyPhrase values(?,?,?,?,?,?,?);";
//
//            logger.info("Saving key phrases....");
//            try {
//
//                connection.setAutoCommit(false);
//                bulkInsert = connection.prepareStatement(sql);
//
//                for (Integer tmpTopicId : topicTitles.keySet()) {
//                    //boolean startComparison = false;fuyhgjlkfdytrdfuikol
//                    Map<String, ArrayList<Integer>> extractedPhrases = topicTitles.get(tmpTopicId);
//                    for (String phrase : extractedPhrases.keySet()) {
//
//                        bulkInsert.setInt(1, tmpTopicId);
//                        bulkInsert.setString(2, tagger);
//                        bulkInsert.setString(3, phrase);
//                        bulkInsert.setInt(4, extractedPhrases.get(phrase).get(0));
//                        bulkInsert.setInt(5, extractedPhrases.get(phrase).get(1));
//                        bulkInsert.setDouble(6, 0);
//                        bulkInsert.setString(7, experimentId);
//
//                        bulkInsert.executeUpdate();
//                    }
//
//                }
//
//                connection.commit();
//
//            } catch (SQLException e) {
//
//                logger.error("Error in insert topicPhrases: " + e);
//                if (connection != null) {
//                    try {
//                        logger.error("Transaction is being rolled back");
//                        connection.rollback();
//                    } catch (SQLException excep) {
//                        logger.error("Error in insert topicPhrases: " + excep);
//                    }
//                }
//            } finally {
//
//                if (bulkInsert != null) {
//                    bulkInsert.close();
//                }
//                connection.setAutoCommit(true);
//            }
//
//        } catch (SQLException e) {
//            // if the error message is "out of memory",
//            // it probably means no database file is found
//            logger.error(e.getMessage());
//        } finally {
//            try {
//                if (connection != null) {
//                    connection.close();
//                }
//            } catch (SQLException e) {
//                // connection close failed.
//                logger.error(e);
//            }
//        }
//        logger.info("Finding Key phrases finished");
//    }

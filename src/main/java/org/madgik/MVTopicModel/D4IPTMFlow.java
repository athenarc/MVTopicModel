        package org.madgik.MVTopicModel;

import cc.mallet.types.*;
import cc.mallet.pipe.*;
import cc.mallet.util.Maths;
import com.sree.textbytes.jtopia.Configuration;
import com.sree.textbytes.jtopia.TermDocument;
import com.sree.textbytes.jtopia.TermsExtractor;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.*;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.madgik.utils.CSV2FeatureSequence;
import org.madgik.utils.FeatureSequenceRemovePlural;
import static org.madgik.utils.Utils.cosineSimilarity;

public class D4IPTMFlow {

    public enum ExperimentType {

                
        D4I
    }

    public enum SimilarityType {

        cos,
        Jen_Sha_Div,
        symKL
    }

    public enum Net2BoWType {

        OneWay,
        TwoWay,
        PPR
    }

    public static Logger logger = Logger.getLogger(D4IPTMFlow.class.getName());

    int topWords = 20;
    int showTopicsInterval = 50;
    byte numModalities = 6;
    
    
    int numOfThreads = 4;
    int numTopics = 400;
    int numIterations = 800; //Max 2000
    int numChars = 4000;
    int burnIn = 50;
    int optimizeInterval = 50;
    ExperimentType experimentType = ExperimentType.D4I;

    double pruneCntPerc = 0.002;    //Remove features that appear less than PruneCntPerc* TotalNumberOfDocuments times (-->very rare features)
    double pruneLblCntPerc = 0.002;   //Remove features that appear less than PruneCntPerc* TotalNumberOfDocuments times (-->very rare features)
    double pruneMaxPerc = 10;//Remove features that occur in more than (X)% of documents. 0.05 is equivalent to IDF of 3.0.

    boolean ACMAuthorSimilarity = true;
    boolean calcTopicDistributionsAndTrends = true;
    boolean calcEntitySimilarities = false;
    boolean calcTopicSimilarities = false;
    boolean calcPPRSimilarities = false;
    boolean runTopicModelling = true;
    boolean runWordEmbeddings = false;
    boolean useTypeVectors = true;
    boolean trainTypeVectors = true;
    boolean findKeyPhrases = false;
    double useTypeVectorsProb = 0.6;
    Net2BoWType PPRenabled = Net2BoWType.OneWay;
    int vectorSize = 200;
    String SQLConnectionString = "jdbc:postgresql://localhost:5432/tender?user=postgres&password=postgres&ssl=false"; //"jdbc:sqlite:C:/projects/OpenAIRE/fundedarxiv.db";
    String experimentId = "";
    String previousModelFile = "";
    int limitDocs = 0;

    public D4IPTMFlow() throws IOException {
        this(null);
    }

    public D4IPTMFlow(Map<String, String> runtimeProp) throws IOException {

        SimilarityType similarityType = SimilarityType.cos; //Cosine 1 jensenShannonDivergence 2 symmetric KLP

        String dictDir = "";

        Connection connection = null;

        getPropValues(runtimeProp);

        String experimentString = experimentType.toString() + "_" + numTopics + "T_"
                + numIterations + "IT_" + numChars + "CHRs_" + numModalities + "M_" + (trainTypeVectors ? "WV" : "") + ((limitDocs > 0) ? ("Lmt_" + limitDocs) : "") + PPRenabled.name();

        String experimentDetails = String.format("Multi View Topic Modeling Analysis \n pruneMaxPerc:%.1f  pruneCntPerc:%.4f pruneLblCntPerc:%.4f burnIn:%d numOfThreads:%d similarityType:%s", this.pruneMaxPerc, pruneCntPerc, pruneLblCntPerc, burnIn, numOfThreads, similarityType.toString());

        String experimentDescription = experimentString + ": \n";

        if (runtimeProp != null) {
            experimentId = runtimeProp.get("ExperimentId");
        }

        if (StringUtils.isBlank(experimentId)) {
            experimentId = experimentString;
        }

        if (findKeyPhrases) {
            FindKeyPhrasesPerTopic(SQLConnectionString, experimentId, "openNLP");
        }

        if (runWordEmbeddings) {
            logger.info(" calc word embeddings starting");
            InstanceList[] instances = GenerateAlphabets(SQLConnectionString, experimentType, dictDir, numModalities, pruneCntPerc,
                    pruneLblCntPerc, pruneMaxPerc, numChars, PPRenabled,
                    false, limitDocs);

            logger.info(" instances added through pipe");

            //int numDimensions = 50;
            int windowSizeOption = 5;
            int numSamples = 5;
            int numEpochs = 5;
            WordEmbeddings matrix = new WordEmbeddings(instances[0].getDataAlphabet(), vectorSize, windowSizeOption);
            //TopicWordEmbeddings matrix = new TopicWordEmbeddings(instances[0].getDataAlphabet(), vectorSize, windowSizeOption,0);
            matrix.queryWord = "skin";
            matrix.countWords(instances[0], 0.0001); //Sampling factor : "Down-sample words that account for more than ~2.5x this proportion or the corpus."
            matrix.train(instances[0], numOfThreads, numSamples, numEpochs);
            logger.info(" calc word embeddings ended");
            //PrintWriter out = new PrintWriter("vectors.txt");
            //matrix.write(out);
            //out.close();
            matrix.write(SQLConnectionString, 0);
            logger.info(" writing word embeddings ended");
        }

        if (runTopicModelling) {

            logger.info(" TopicModelling has started");
            String batchId = "-1";
            InstanceList[] instances = GenerateAlphabets(SQLConnectionString, experimentType, dictDir, numModalities, pruneCntPerc,
                    pruneLblCntPerc, pruneMaxPerc, numChars, PPRenabled,
                    false
            ,limitDocs);
            logger.info("Instances added through pipe");

            double beta = 0.01;
            double[] betaMod = new double[numModalities];
            Arrays.fill(betaMod, 0.01);
            boolean useCycleProposals = false;
            double alpha = 0.1;

            double[] alphaSum = new double[numModalities];
            Arrays.fill(alphaSum, 1);

            double[] gamma = new double[numModalities];
            Arrays.fill(gamma, 1);

            //double gammaRoot = 4;
            FastQMVWVParallelTopicModel model = new FastQMVWVParallelTopicModel(numTopics, numModalities, alpha, beta, useCycleProposals, SQLConnectionString, useTypeVectors, useTypeVectorsProb, trainTypeVectors);

            model.CreateTables(SQLConnectionString, experimentId);

            // ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);
            model.setNumIterations(numIterations);
            model.setTopicDisplay(showTopicsInterval, topWords);
            // model.setIndependentIterations(independentIterations);
            model.optimizeInterval = optimizeInterval;
            model.burninPeriod = burnIn;
            model.setNumThreads(numOfThreads);

            model.addInstances(instances, batchId, vectorSize, "");//trainingInstances);//instances);
            logger.info(" instances added");

            //model.readWordVectorsDB(SQLConnectionString, vectorSize);
            model.estimate();
            logger.info("Model estimated");

            model.saveResults(SQLConnectionString, experimentId, experimentDetails);
            logger.info("Model saved");
            
            logger.info("Model Id: \n" + experimentId);
            logger.info("Model Metadata: \n" + model.getExpMetadata());

            //if (modelEvaluationFile != null) {
            try {

                double perplexity = 0;

                FastQMVWVTopicModelDiagnostics diagnostics = new FastQMVWVTopicModelDiagnostics(model, topWords);
                diagnostics.saveToDB(SQLConnectionString, experimentId, perplexity, batchId);
                logger.info("full diagnostics calculation finished");

            } catch (Exception e) {

                logger.error(e.getMessage());
            }

            
          
        }
        if (calcTopicDistributionsAndTrends) {

            CalcEntityTopicDistributionsAndTrends(SQLConnectionString, experimentId);

        }

        if (calcEntitySimilarities) {

            calcSimilarities(SQLConnectionString, experimentType, experimentId, ACMAuthorSimilarity, similarityType, numTopics);

        }

        if (calcTopicSimilarities) {
            experimentId = "HEALTHTenderPM_500T_600IT_7000CHRs_10.0 3.0E-4_2.0E-4PRN50B_4M_4TH_cosOneWay";
            CalcTopicSimilarities(SQLConnectionString, experimentId);
        }

        if (calcPPRSimilarities) {
            calcPPRSimilarities(SQLConnectionString);
        }

    }

    public void getPropValues(Map<String, String> runtimeProp) throws IOException {

        InputStream inputStream = null;
        try {
            Properties prop = new Properties();
            String propFileName = "config.properties";

            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            if (runtimeProp != null) {
                prop.putAll(runtimeProp);
            }

            // get the property value and print it out
            numTopics = Integer.parseInt(prop.getProperty("TopicsNumber"));
            topWords = Integer.parseInt(prop.getProperty("TopWords"));
            numModalities = Byte.parseByte(prop.getProperty("NumModalities"));
            numIterations = Integer.parseInt(prop.getProperty("Iterations"));
            numOfThreads = Integer.parseInt(prop.getProperty("NumOfThreads"));
            numChars = Integer.parseInt(prop.getProperty("NumOfChars"));
            burnIn = Integer.parseInt(prop.getProperty("BurnIn"));
            optimizeInterval = Integer.parseInt(prop.getProperty("OptimizeInterval"));
            pruneCntPerc = Double.parseDouble(prop.getProperty("PruneCntPerc"));
            pruneLblCntPerc = Double.parseDouble(prop.getProperty("PruneLblCntPerc"));
            pruneMaxPerc = Double.parseDouble(prop.getProperty("PruneMaxPerc"));
            SQLConnectionString = prop.getProperty("SQLConnectionString");
            experimentId = prop.getProperty("ExperimentId");

        } catch (Exception e) {
            logger.error("Exception in reading properties: " + e);
        } finally {
            inputStream.close();
        }

    }

//    private void writeProperties() {
//        Properties prop = new Properties();
//        OutputStream output = null;
//
//        try {
//
//            String propFileName = "config.properties";
//
//            output = getClass().getClassLoader().getResourceAsStream(propFileName);
//
//            if (inputStream != null) {
//                prop.load(inputStream);
//            } else {
//                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
//            }
//            
//            output = new FileOutputStream("config.properties");
//            
//            
//            // set the properties value
//            prop.setProperty("topWords", String.valueOf(topWords));
//            prop.setProperty("SQLConnectionString", SQLConnectionString);
//            prop.setProperty("showTopicsInterval", String.valueOf(showTopicsInterval));
//            prop.setProperty("numModalities", numModalities);
//            prop.setProperty("numOfThreads", numOfThreads);
//            prop.setProperty("numTopics", numTopics);
//            prop.setProperty("numIterations", numIterations);
//            prop.setProperty("numChars", numChars);
//            prop.setProperty("burnIn", burnIn);
//            prop.setProperty("optimizeInterval", optimizeInterval);
//            prop.setProperty("experimentType", experimentType);
//            prop.setProperty("pruneCntPerc", pruneCntPerc);
//            prop.setProperty("pruneLblCntPerc", pruneLblCntPerc);
//            prop.setProperty("pruneMaxPerc", pruneMaxPerc);
//            prop.setProperty("pruneMinPerc", pruneMinPerc);
//            prop.setProperty("calcEntitySimilarities", calcEntitySimilarities);
//            prop.setProperty("runTopicModelling", runTopicModelling);
//            prop.setProperty("findKeyPhrases", findKeyPhrases);
//            prop.setProperty("PPRenabled", PPRenabled);
//           
//
//            // save properties to project root folder
//            prop.store(output, null);
//
//        } catch (IOException io) {
//            io.printStackTrace();
//        } finally {
//            if (output != null) {
//                try {
//                    output.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//        }
//    }
    private void FindKeyPhrasesPerTopic(String SQLConnection, String experimentId, String tagger) {
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

        Connection connection = null;
        try {
            // create a database connection
            //connection = DriverManager.getConnection(SQLConnectionString);
            connection = DriverManager.getConnection(SQLConnection);
            Statement statement = connection.createStatement();

            logger.info("Finding key phrases calculation started");

            String sql = "select pubtopic.TopicId, publication.title, publication.abstract from \n"
                    + "PubTopic\n"
                    + "inner join publication on PubTopic.pubId= publication.PubId and pubTOpic.Weight>0.55 \n"
                    + "where experimentId='" + experimentId + "' \n"
                    + "order by pubtopic.topicid, weight desc";

            ResultSet rs = statement.executeQuery(sql);

            HashMap<Integer, Map<String, ArrayList<Integer>>> topicTitles = null;

            topicTitles = new HashMap<Integer, Map<String, ArrayList<Integer>>>();

            Integer topicId = -1;

            while (rs.next()) {

                int newTopicId = rs.getInt("TopicId");

                if (newTopicId != topicId && topicId != -1) {
                    logger.info("Finding key phrases for topic " + topicId);
                    topiaDoc = termExtractor.extractTerms(stringBuffer.toString());
                    topicTitles.put(topicId, topiaDoc.getFinalFilteredTerms());
                    stringBuffer = new StringBuffer();
                }
                stringBuffer.append(rs.getString("title").replace('-', ' ').toLowerCase() + "\n");
                //stringBuffer.append(rs.getString("abstract").replace('-', ' ').toLowerCase() + "\n");
                topicId = newTopicId;

            }

            logger.info("Finding key phrases for topic " + topicId);
            topiaDoc = termExtractor.extractTerms(stringBuffer.toString());
            topicTitles.put(topicId, topiaDoc.getFinalFilteredTerms());

            statement.executeUpdate("create table if not exists TopicKeyPhrase ( TopicId Integer, Tagger TEXT, Phrase Text, Count Integer, WordsNum Integer, Weight double, ExperimentId TEXT) ");
            String deleteSQL = String.format("Delete from TopicKeyPhrase WHERE ExperimentId='" + experimentId + "' AND Tagger ='" + tagger + "'");
            statement.executeUpdate(deleteSQL);

            PreparedStatement bulkInsert = null;
            sql = "insert into TopicKeyPhrase values(?,?,?,?,?,?,?);";

            logger.info("Saving key phrases....");
            try {

                connection.setAutoCommit(false);
                bulkInsert = connection.prepareStatement(sql);

                for (Integer tmpTopicId : topicTitles.keySet()) {
                    //boolean startComparison = false;fuyhgjlkfdytrdfuikol
                    Map<String, ArrayList<Integer>> extractedPhrases = topicTitles.get(tmpTopicId);
                    for (String phrase : extractedPhrases.keySet()) {

                        bulkInsert.setInt(1, tmpTopicId);
                        bulkInsert.setString(2, tagger);
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

    private void TfIdfWeighting(InstanceList instances, String SQLConnection, String experimentId, int itemType) {

        int N = instances.size();

        Alphabet alphabet = instances.getDataAlphabet();
        Object[] tokens = alphabet.toArray();
        System.out.println("# Number of dimensions: " + tokens.length);
        // determine document frequency for each term
        int[] df = new int[tokens.length];
        for (Instance instance : instances) {
            FeatureVector fv = new FeatureVector((FeatureSequence) instance.getData());
            int[] indices = fv.getIndices();
            for (int index : indices) {
                df[index]++;
            }
        }

        // determine document length for each document
        int[] lend = new int[N];
        double lenavg = 0;
        for (int i = 0; i < N; i++) {
            Instance instance = instances.get(i);
            FeatureVector fv = new FeatureVector((FeatureSequence) instance.getData());
            int[] indices = fv.getIndices();
            double length = 0.0;
            for (int index : indices) {
                length += fv.value(index);
            }
            lend[i] = (int) length;
            lenavg += length;
        }
        if (N > 1) {
            lenavg /= (double) N;
        }

        Connection connection = null;
        Statement statement = null;
        PreparedStatement bulkInsert = null;

        try {
            // create a database connection
            if (!SQLConnection.isEmpty()) {
                connection = DriverManager.getConnection(SQLConnection);
                statement = connection.createStatement();
                statement.executeUpdate("create table if not exists TokensPerEntity (EntityId nvarchar(100), ItemType int, Token nvarchar(100), Counts double, TFIDFCounts double, ExperimentId nvarchar(50)) ");

                statement.executeUpdate("create Index if not exists IX_TokensPerEntity_Entity_Counts ON TokensPerEntity ( EntityId, ExperimentId, ItemType, Counts DESC, TFIDFCounts DESC, Token)");
                statement.executeUpdate("create Index if not exists IX_TokensPerEntity_Entity_TFIDFCounts ON TokensPerEntity ( EntityId, ExperimentId, ItemType,  TFIDFCounts DESC, Counts DESC, Token)");

                statement.executeUpdate("create View if not exists TokensPerEntityView AS select rv1.EntityId, rv1.ItemType, rv1.Token, rv1.Counts, rv1.TFIDFCounts, rv1.ExperimentId \n"
                        + "FROM TokensPerEntity rv1\n"
                        + "WHERE Token in\n"
                        + "(\n"
                        + "SELECT Token\n"
                        + "FROM TokensPerEntity rv2\n"
                        + "WHERE EntityId = rv1.EntityId AND Counts>2 AND ItemType=rv1.ItemType AND ExperimentId=rv1.ExperimentId \n"
                        + "ORDER BY\n"
                        + "TFIDFCounts DESC\n"
                        + "LIMIT 20\n"
                        + ")");

                String deleteSQL = String.format("Delete from TokensPerEntity where  ExperimentId = '%s' and itemtype= %d", experimentId, itemType);
                statement.executeUpdate(deleteSQL);

                String sql = "insert into TokensPerEntity values(?,?,?,?,?,?);";

                connection.setAutoCommit(false);
                bulkInsert = connection.prepareStatement(sql);

                for (int i = 0; i < N; i++) {
                    Instance instance = instances.get(i);

                    FeatureVector fv = new FeatureVector((FeatureSequence) instance.getData());
                    int[] indices = fv.getIndices();
                    for (int index : indices) {
                        double tf = fv.value(index);
                        double tfcomp = tf / (tf + 0.5 + 1.5 * (double) lend[i] / lenavg);
                        double idfcomp = Math.log((double) N / (double) df[index]) / Math.log(N + 1);
                        double tfIdf = tfcomp * idfcomp;
                        fv.setValue(index, tfIdf);
                        String token = fv.getAlphabet().lookupObject(index).toString();

                        bulkInsert.setString(1, instance.getName().toString());
                        bulkInsert.setInt(2, itemType);
                        bulkInsert.setString(3, token);
                        bulkInsert.setDouble(4, tf);
                        bulkInsert.setDouble(5, tfIdf);
                        bulkInsert.setString(6, experimentId);

                        bulkInsert.executeUpdate();
                    }
                }

                connection.commit();
            }
        } catch (SQLException e) {

            if (connection != null) {
                try {
                    logger.error("Transaction is being rolled back");
                    connection.rollback();
                } catch (SQLException excep) {
                    logger.error("Error in insert TokensPerEntity");
                }
            }
        } finally {
            try {
                if (bulkInsert != null) {
                    bulkInsert.close();
                }
                connection.setAutoCommit(true);
            } catch (SQLException excep) {
                logger.error("Error in insert TokensPerEntity");
            }
        }

        //TODO: Sort Feature Vector Values
        // FeatureVector.toSimpFilefff
    }

    private void GenerateStoplist(SimpleTokenizer prunedTokenizer, ArrayList<Instance> instanceBuffer, int pruneCount, double docProportionMaxCutoff, boolean preserveCase)
            throws IOException {

        //SimpleTokenizer st = new SimpleTokenizer(new File("stoplists/en.txt"));
        ArrayList<Instance> input = new ArrayList<Instance>();
        for (Instance instance : instanceBuffer) {
            input.add((Instance) instance.clone());
        }

        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        Alphabet alphabet = new Alphabet();

        CharSequenceLowercase csl = new CharSequenceLowercase();
        //prunedTokenizer = st.deepClone();
        SimpleTokenizer st = prunedTokenizer.deepClone();
        StringList2FeatureSequence sl2fs = new StringList2FeatureSequence(alphabet);
        FeatureCountPipe featureCounter = new FeatureCountPipe(alphabet, null);
        FeatureDocFreqPipe docCounter = new FeatureDocFreqPipe(alphabet, null);

        pipes.add(new Input2CharSequence()); //homer

        if (!preserveCase) {
            pipes.add(csl);
        }
        pipes.add(st);
        pipes.add(sl2fs);
        if (pruneCount > 0) {
            pipes.add(featureCounter);
        }
        if (docProportionMaxCutoff < 1.0) {
            //if (docProportionMaxCutoff < 1.0 || docProportionMinCutoff > 0) {
            pipes.add(docCounter);
        }
        //TODO: TEST pipes.add(new FeatureSequenceRemovePlural(alphabet));

        Pipe serialPipe = new SerialPipes(pipes);
        Iterator<Instance> iterator = serialPipe.newIteratorFrom(input.iterator());

        int count = 0;

        // We aren't really interested in the instance itself,
        //  just the total feature counts.
        while (iterator.hasNext()) {
            count++;
            if (count % 100000 == 0) {
                System.out.println(count);
            }
            iterator.next();
        }

        Iterator<String> wordIter = alphabet.iterator();
        while (wordIter.hasNext()) {
            String word = (String) wordIter.next();

            if (!word.matches("^(?!.*(-[^-]*-|_[^_]*_))[A-Za-z0-9][\\w-]*[A-Za-z0-9]$") || word.length() < 3 || word.contains("cid") || word.contains("italic") || word.contains("null") || word.contains("usepackage") || word.contains("fig")) {
                prunedTokenizer.stop(word);
            }
        }

        prunedTokenizer.stop("tion");
        prunedTokenizer.stop("ing");
        prunedTokenizer.stop("ment");
        prunedTokenizer.stop("ytem");
        prunedTokenizer.stop("wth");
        prunedTokenizer.stop("whch");
        prunedTokenizer.stop("nfrmatn");
        prunedTokenizer.stop("uer");
        prunedTokenizer.stop("ther");
        prunedTokenizer.stop("frm");
        prunedTokenizer.stop("hypermeda");
        prunedTokenizer.stop("anuae");
        prunedTokenizer.stop("dcument");
        prunedTokenizer.stop("tudent");
        prunedTokenizer.stop("appcatn");
        prunedTokenizer.stop("tructure");
        prunedTokenizer.stop("prram");
        prunedTokenizer.stop("den");
        prunedTokenizer.stop("aed");
        prunedTokenizer.stop("cmputer");
        prunedTokenizer.stop("prram");

        prunedTokenizer.stop("mre");
        prunedTokenizer.stop("cence");
        prunedTokenizer.stop("tures");
        prunedTokenizer.stop("ture");
        prunedTokenizer.stop("ments");
        prunedTokenizer.stop("cations");
        prunedTokenizer.stop("tems");
        prunedTokenizer.stop("tem");
        prunedTokenizer.stop("tional");
        prunedTokenizer.stop("ity");
        prunedTokenizer.stop("ware");
        prunedTokenizer.stop("opment");
        prunedTokenizer.stop("guage");
        prunedTokenizer.stop("niques");

        if (pruneCount > 0) {
            featureCounter.addPrunedWordsToStoplist(prunedTokenizer, pruneCount);
        }
        if (docProportionMaxCutoff < 1.0) {
            docCounter.addPrunedWordsToStoplist(prunedTokenizer, docProportionMaxCutoff);
        }

//        if (pruneCount > 0) {
//            featureCounter.addPrunedWordsToStoplist(prunedTokenizer, pruneCount);
//        }
//        if (docProportionMaxCutoff < 1.0 || docProportionMinCutoff > 0) {
//            docCounter.addPrunedWordsToStoplist(prunedTokenizer, docProportionMaxCutoff, docProportionMinCutoff);
//        }
    }

    public void createCitationGraphFile(String outputCsv, String SQLConnectionString) {
        //String SQLConnectionString = "jdbc:sqlite:C:/projects/OpenAIRE/fundedarxiv.db";

        Connection connection = null;
        try {

            FileWriter fwrite = new FileWriter(outputCsv);
            BufferedWriter out = new BufferedWriter(fwrite);
            String header = "# DBLP citation graph \n"
                    + "# fromNodeId, toNodeId \n";
            out.write(header);

            connection = DriverManager.getConnection(SQLConnectionString);

            String sql = "select id, ref_id from papers where ref_num >0 ";
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                // read the result set
                int Id = rs.getInt("Id");
                String citationNums = rs.getString("ref_id");

                String csvLine = "";//Id + "\t" + citationNums;

                String[] str = citationNums.split("\t");
                for (int i = 0; i < str.length - 1; i++) {
                    csvLine = Id + "\t" + str[i];
                    out.write(csvLine + "\n");
                }

            }
            out.flush();
        } catch (SQLException e) {
            // if the error message is "out of memory", 
            // it probably means no database file is found
            logger.error(e.getMessage());
        } catch (Exception e) {
            logger.error("File input error");
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

    }

    private class TopicVector {

        public int TopicId;
        public String ExperimentId;
        public double[] Vector;
    }

    public boolean isAlphanumeric(String str) {

        return str.matches("^(?!.*(-[^-]*-|_[^_]*_))[A-Za-z0-9][\\w-]*[A-Za-z0-9]$");
        //Pattern p = Pattern.compile("[^a-zA-Z0-9]+$");

    }

    public void CalcEntityTopicDistributionsAndTrends(String SQLConnectionString, String experimentId) {
        Connection connection = null;
        try {

            connection = DriverManager.getConnection(SQLConnectionString);
            Statement statement = connection.createStatement();

            logger.info("Calc topic Entity Topic Distributions and Trends started");

            String deleteSQL = String.format("Delete from EntityTopicDistribution where ExperimentId= '%s'", experimentId);
            statement.executeUpdate(deleteSQL);

            logger.info("Insert Full Topic Distribution ");

            String SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                    + "select '',  PubTopic.TopicId, '', 'Corpus', round(sum(weight)/SumTopicWeightView.SumWeight, 5) as NormWeight, PubTopic.ExperimentId\n"
                    + "from PubTopic\n"
                    + "INNER JOIN (SELECT  sum(weight) AS SumWeight, ExperimentId\n"
                    + "FROM PubTopic\n"
                    + "Where PubTopic.weight>0.1 \n"
                    + " and PubTopic.ExperimentId='" + experimentId + "'  \n"
                    + "GROUP BY  ExperimentId) SumTopicWeightView on SumTopicWeightView.ExperimentId= PubTopic.ExperimentId\n"
                    + "group By PubTopic.TopicId, PubTopic.ExperimentId, SumTopicWeightView.SumWeight\n"
                    + "Order by  NormWeight Desc";

            statement.executeUpdate(SQLstr);

            logger.info("Trend Topic distribution for the whole coprus");

            SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                    + "select Publication.BatchId,  PubTopic.TopicId, '', 'CorpusTrend', \n"
                    + "round(sum(weight)/SumTopicWeightPerBatchView.BatchSumWeight,5) as NormWeight,  PubTopic.ExperimentId\n"
                    + "from PubTopic\n"
                    + "Inner Join Publication on PubTopic.PubId= Publication.PubId and PubTopic.weight>0.1\n"
                    + "INNER JOIN (SELECT Publication.BatchId, sum(weight) AS BatchSumWeight, ExperimentId\n"
                    + "FROM PubTopic\n"
                    + "INNER JOIN Publication ON PubTopic.PubId= Publication.PubId AND\n"
                    + "PubTopic.weight>0.1\n "
                    + "and PubTopic.ExperimentId='" + experimentId + "'   \n"
                    + "GROUP BY Publication.BatchId, ExperimentId) SumTopicWeightPerBatchView on SumTopicWeightPerBatchView.BatchId = Publication.BatchId and SumTopicWeightPerBatchView.ExperimentId= PubTopic.ExperimentId\n"
                    + "group By Publication.BatchId,SumTopicWeightPerBatchView.BatchSumWeight, PubTopic.TopicId, PubTopic.ExperimentId\n"
                    + "Order by Publication.BatchId,   NormWeight Desc";

            statement.executeUpdate(SQLstr);
            logger.info("Project Topic distribution");

            SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                    + "SELECT '', PubTopic.TopicId, PubProject.ProjectId,'Project',\n"
                    + "           round(sum(PubTopic.weight) / SumTopicWeightPerProjectView.ProjectSumWeight,5) AS NormWeight,\n"
                    + "             PubTopic.ExperimentId\n"
                    + "      FROM PubTopic\n"
                    + "      INNER JOIN  PubProject ON PubTopic.PubId = PubProject.PubId AND PubTopic.weight > 0.1\n"
                    + "      and  PubTopic.ExperimentId='" + experimentId + "' \n"
                    + "           INNER JOIN (SELECT PubProject.ProjectId, sum(weight) AS ProjectSumWeight,    ExperimentId\n"
                    + "           FROM PubTopic\n"
                    + "           INNER JOIN   PubProject ON PubTopic.PubId = PubProject.PubId AND  PubTopic.weight > 0.1\n"
                    + "           GROUP BY  ExperimentId,PubProject.ProjectId)\n"
                    + "           SumTopicWeightPerProjectView ON SumTopicWeightPerProjectView.ProjectId = PubProject.ProjectId AND \n"
                    + "                                           SumTopicWeightPerProjectView.ExperimentId = PubTopic.ExperimentId                                            \n"
                    + "     GROUP BY PubProject.ProjectId,\n"
                    + "              SumTopicWeightPerProjectView.ProjectSumWeight,\n"
                    + "              PubTopic.TopicId,\n"
                    + "              PubTopic.ExperimentId\n"
                    + "              order by  PubTopic.ExperimentId, PubProject.ProjectId, NormWeight Desc,PubTopic.ExperimentId";

            statement.executeUpdate(SQLstr);

            logger.info("Funder Topic distribution");
            SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                    + " SELECT '', PubTopic.TopicId, PubFunder.funder,'Funder',\n"
                    + "                               round(sum(PubTopic.weight) / SumTopicWeightPerProjectView.ProjectSumWeight,5) AS NormWeight,\n"
                    + "                                 PubTopic.ExperimentId\n"
                    + "                          FROM PubTopic\n"
                    + "                          INNER JOIN  PubFunder ON PubTopic.PubId = PubFunder.PubId AND PubTopic.weight > 0.1\n"
                    + "                          and  PubTopic.ExperimentId='" + experimentId + "' \n"
                    + "                        \n"
                    + "                               INNER JOIN (SELECT PubFunder.funder, sum(weight) AS ProjectSumWeight,    ExperimentId\n"
                    + "                               FROM PubTopic\n"
                    + "                               INNER JOIN   PubFunder ON PubTopic.PubId = PubFunder.PubId AND  PubTopic.weight > 0.1\n"
                    + "                               \n"
                    + "                               GROUP BY  ExperimentId,PubFunder.funder)\n"
                    + "                               SumTopicWeightPerProjectView ON SumTopicWeightPerProjectView.funder = PubFunder.funder AND \n"
                    + "                                                               SumTopicWeightPerProjectView.ExperimentId = PubTopic.ExperimentId                                            \n"
                    + "                         GROUP BY PubFunder.funder,\n"
                    + "                                  SumTopicWeightPerProjectView.ProjectSumWeight,\n"
                    + "                                  PubTopic.TopicId,\n"
                    + "                                  PubTopic.ExperimentId\n"
                    + "                                  order by  PubTopic.ExperimentId, PubFunder.funder, NormWeight Desc,PubTopic.ExperimentId";

            statement.executeUpdate(SQLstr);

            logger.info("Funder Trend Topic distribution");

            SQLstr = "INSERT INTO EntityTopicDistribution (BatchId , TopicId ,  EntityId, EntityType,  NormWeight , ExperimentId )\n"
                    + " SELECT Publication.batchId, PubTopic.TopicId, PubFunder.funder,'FunderTrend',\n"
                    + "                               round(sum(PubTopic.weight) / SumTopicWeightPerProjectView.ProjectSumWeight,5) AS NormWeight,\n"
                    + "                                 PubTopic.ExperimentId\n"
                    + "                          FROM PubTopic\n"
                    + "                          INNER JOIN Publication on PubTopic.PubId= Publication.PubId and PubTopic.weight>0.1\n"
                    + "                          and  PubTopic.ExperimentId='" + experimentId + "' \n"
                    + "                          INNER JOIN  PubFunder ON PubTopic.PubId = PubFunder.PubId                           \n"
                    + "                               INNER JOIN (SELECT PubFunder.funder, Publication.batchId, sum(weight) AS ProjectSumWeight,    ExperimentId\n"
                    + "                               FROM PubTopic\n"
                    + "                               Inner Join Publication on PubTopic.PubId= Publication.PubId and PubTopic.weight>0.1           \n"
                    + "                               INNER JOIN   PubFunder ON PubTopic.PubId = PubFunder.PubId                                \n"
                    + "                               GROUP BY  PubFunder.funder,Publication.batchId,ExperimentId)\n"
                    + "                               SumTopicWeightPerProjectView ON SumTopicWeightPerProjectView.funder = PubFunder.funder AND \n"
                    + "                                                               SumTopicWeightPerProjectView.ExperimentId = PubTopic.ExperimentId  AND                                          \n"
                    + "                                                               SumTopicWeightPerProjectView.batchId = Publication.batchId\n"
                    + "                         GROUP BY PubFunder.funder,\n"
                    + "                             Publication.batchId,\n"
                    + "                                  SumTopicWeightPerProjectView.ProjectSumWeight,\n"
                    + "                                  PubTopic.TopicId,\n"
                    + "                                  PubTopic.ExperimentId\n"
                    + "                                  order by  PubTopic.ExperimentId, PubFunder.funder, NormWeight Desc,PubTopic.ExperimentId";

            statement.executeUpdate(SQLstr);

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

    public void CalcTopicSimilarities(String SQLConnectionString, String experimentId) {

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

            List<TopicVector> topicVectors = new ArrayList<TopicVector>();

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

    public void calcPPRSimilarities(String SQLConnectionString) {
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
                    + "Select PubId, CitationId, 1 as Counts From PubCitation\n"
                    + "ORDER by PubId ";

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
                newLabelId = rs.getString("PubId");
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

            statement.executeUpdate("create table if not exists PPRPubCitationSimilarity (PubId TEXT,  Similarity double) ");
            String deleteSQL = String.format("Delete from PPRPubCitationSimilarity");
            statement.executeUpdate(deleteSQL);

            PreparedStatement bulkInsert = null;
            sql = "insert into PPRPubCitationSimilarity values(?,?);";

            try {

                connection.setAutoCommit(false);
                bulkInsert = connection.prepareStatement(sql);

                for (String fromPubId : labelVectors.keySet()) {

                    if (fromPubId.contains("PPR")) {
                        continue;
                    }
                    String toPubId = fromPubId + "PPR";
                    similarity = -1;

                    if (labelVectors.get(fromPubId) != null && labelVectors.get(toPubId) != null) {
                        similarity = 1 - Math.abs(cosineSimilarity.distance(labelVectors.get(fromPubId), labelVectors.get(toPubId))); // the function returns distance not similarity
                    }
                    bulkInsert.setString(1, fromPubId);
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

    public void FindGroundTruthCommunities(String SQLConnectionString, String experimentId) {
        //calc similarities

        //logger.info("PPRSimilarities calculation Started");
        Connection connection = null;
        try {
            // create a database connection
            //connection = DriverManager.getConnection(SQLConnectionString);
            connection = DriverManager.getConnection(SQLConnectionString);
            Statement statement = connection.createStatement();

            logger.info("PPRSimilarities calculation Started");

            String sql = "select TopicId||'EXP' as GroupId, PubId as NodeId from PubTopic\n"
                    + "WHERE ExperimentId = '" + experimentId + "'\n"
                    + "UNION \n"
                    + "Select  GroupId,NodeId from GroundTruth\n"
                    + "Order By GroupId";

            ResultSet rs = statement.executeQuery(sql);

            HashMap<String, SparseVector> labelVectors = null;
            //HashMap<String, double[]> similarityVectors = null;
            labelVectors = new HashMap<String, SparseVector>();

            String labelId = "";

            int[] citations = new int[30000];
            double[] weights = new double[30000];
            int cnt = 0;

            while (rs.next()) {

                String newLabelId = "";
                newLabelId = rs.getString("GroupId");
                if (!newLabelId.equals(labelId) && !labelId.isEmpty()) {
                    labelVectors.put(labelId, new SparseVector(citations, weights, citations.length, citations.length, true, true, true));
                    citations = new int[30000];
                    weights = new double[30000];
                    cnt = 0;
                }
                labelId = newLabelId;
                citations[cnt] = rs.getInt("NodeId");
                weights[cnt] = 1; // rs.getDouble("Counts");
                cnt++;

            }

            cnt = 0;
            double similarity = 0;

            NormalizedDotProductMetric cosineSimilarity = new NormalizedDotProductMetric();

            statement.executeUpdate("create table if not exists GroundTruth2TopicId (Community TEXT,  TopicId TEXT, Similarity double, ExperimentId TEXT) ");
            String deleteSQL = String.format("Delete from GroundTruth2TopicId WHERE ExperimentId='" + experimentId + "'");
            statement.executeUpdate(deleteSQL);

            PreparedStatement bulkInsert = null;
            sql = "insert into GroundTruth2TopicId values(?,?,?,?);";

            try {

                connection.setAutoCommit(false);
                bulkInsert = connection.prepareStatement(sql);

                for (String fromPubId : labelVectors.keySet()) {
                    //boolean startComparison = false;
                    for (String toPubId : labelVectors.keySet()) {

                        if (fromPubId.contains("EXP") || !toPubId.contains("EXP")) {
                            //if (! startComparison) {
                            //      startComparison = (fromPubId == toPubId );
                            continue;
                        }
                        //String toPubId = fromPubId + "EXP";
                        similarity = -1;

                        if (labelVectors.get(fromPubId) != null && labelVectors.get(toPubId) != null) {
                            similarity = 1 - Math.abs(cosineSimilarity.distance(labelVectors.get(fromPubId), labelVectors.get(toPubId))); // the function returns distance not similarity
                        }
                        if (similarity > 0.4) {
                            bulkInsert.setString(1, fromPubId);
                            bulkInsert.setString(2, toPubId);
                            bulkInsert.setDouble(3, (double) Math.round(similarity * 1000) / 1000);
                            bulkInsert.setString(4, experimentId);
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

        logger.info("Pub citation similarities calculation finished");
    }

    public void calcSimilarities(String SQLConnectionString, ExperimentType experimentType, String experimentId, boolean ACMAuthorSimilarity, SimilarityType similarityType, int numTopics) {
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

                case D4I:                
                    entityType = "Project";
                    sql = "select EntityTopicDistribution.EntityId as projectId, EntityTopicDistribution.TopicId, EntityTopicDistribution.NormWeight as Weight \n"
                            + "                                                        from EntityTopicDistribution                                                        \n"
                            + "                                                        where EntityTopicDistribution.EntityType='Project' \n"
                            + "                                                        AND EntityTopicDistribution.experimentId= '" + experimentId + "'    \n"
                            + "                                                        AND EntityTopicDistribution.EntityId<>'' and EntityTopicDistribution.NormWeight>0.03\n"
                            + "                                                        and EntityTopicDistribution.EntityId in (Select ProjectId FROM PubProject GROUP BY ProjectId HAVING Count(*)>4)\n";

                    break;

                default:
            }

            // String sql = "select fundedarxiv.file from fundedarxiv inner join funds on file=filename Group By fundedarxiv.file LIMIT 10" ;
            ResultSet rs = statement.executeQuery(sql);

            HashMap<String, SparseVector> labelVectors = null;
            HashMap<String, double[]> similarityVectors = null;
            if (similarityType == SimilarityType.cos) {
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

                    

                    case D4I:
                        newLabelId = rs.getString("projectId");
                        break;

                    

                    default:
                }

                if (!newLabelId.equals(labelId) && !labelId.isEmpty()) {
                    if (similarityType == SimilarityType.cos) {
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

                if (similarityType == SimilarityType.Jen_Sha_Div) {
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
                } else if (similarityType == SimilarityType.cos) {
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

    public InstanceList[] GenerateAlphabets(String SQLConnection, ExperimentType experimentType, String dictDir, byte numModalities,
            double pruneCntPerc, double pruneLblCntPerc, double pruneMaxPerc, int numChars, Net2BoWType PPRenabled, boolean ignoreText, int limitDocs) {

        //String txtAlphabetFile = dictDir + File.separator + "dict[0].txt";
        // Begin by importing documents from text to feature sequences
        ArrayList<Pipe> pipeListText = new ArrayList<Pipe>();

        // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeListText.add(new Input2CharSequence()); //homer
        pipeListText.add(new CharSequenceLowercase());

        SimpleTokenizer tokenizer = new SimpleTokenizer(new File("stoplists/en.txt"));
        pipeListText.add(tokenizer);

        Alphabet alphabet = new Alphabet();
        pipeListText.add(new StringList2FeatureSequence(alphabet));
        //pipeListText.add(new FeatureSequenceRemovePlural(alphabet));

        ArrayList<ArrayList<Instance>> instanceBuffer = new ArrayList<ArrayList<Instance>>(numModalities);
        InstanceList[] instances = new InstanceList[numModalities];

        if (!ignoreText) {
            instances[0] = new InstanceList(new SerialPipes(pipeListText));
        }
        // Other Modalities
        for (byte m = ignoreText ? (byte) 0 : (byte) 1; m < numModalities; m++) {
            Alphabet alphabetM = new Alphabet();
            ArrayList<Pipe> pipeListCSV = new ArrayList<Pipe>();
            pipeListCSV.add(new CSV2FeatureSequence(alphabetM, ","));

            if (m == 1 && experimentType == ExperimentType.D4I) //keywords
            {
                //  pipeListCSV.add(new FeatureSequenceRemovePlural(alphabetM));
            }
            instances[m] = new InstanceList(new SerialPipes(pipeListCSV));
        }

        //createCitationGraphFile("C:\\projects\\Datasets\\DBLPManage\\acm_output_NET.csv", "jdbc:sqlite:C:/projects/Datasets/DBLPManage/acm_output.db");
        for (byte m = 0; m < numModalities; m++) {
            instanceBuffer.add(new ArrayList<Instance>());

        }

        Connection connection = null;
        try {

            connection = DriverManager.getConnection(SQLConnection);
            connection.setAutoCommit(false);

            String sql = "";
            String txtsql = "select doctxt_view.pubId, text, fulltext from doctxt_view and  pmpubviewtxt.pubyear>='2004'" + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");

         
         /*   if (experimentType == ExperimentType.D4I) {
                txtsql = "select distinct on (pmpubviewtxt.pubId) pmpubviewtxt.pubId, text from pmpubviewtxt"
                        + " LEFT JOIN pubproject on pubproject.pubId = pmpubviewtxt.pubId\n"
                        + "                         LEFT JOIN project on pubproject.projectid = project.projectid  \n"
                        + "                         LEFT JOIN pubfunder on pubfunder.pubId = pmpubviewtxt.pubId \n"
                        + "                         WHERE ((pmpubviewtxt.referenceid like 'PMC%') or (project.fundinglevel2='FP7-HEALTH') or (project.fundinglevel2 like 'H2020-EU.3.1%') or (project.funder IN ('SRC','Vinnova', 'Formas', 'WT', 'NIH'))) and  pmpubviewtxt.pubyear>='2004'"
                        + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");
                //+ " LIMIT 1000";
            }
*/
            
        if (experimentType == ExperimentType.D4I) {
                sql = "select doctags_view.Id,   keywords, meshterms from doctags_view"                        
                        + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");
                //+ " LIMIT 1000";

            }

            logger.info(" Getting text from the database");
            // get txt data 
            Statement txtstatement = connection.createStatement();
            txtstatement.setFetchSize(10000);
            ResultSet rstxt = txtstatement.executeQuery(txtsql);

            while (rstxt.next()) {

                String txt = "";

                switch (experimentType) {

            
                    case D4I:                    
                        txt = rstxt.getString("text");
                        instanceBuffer.get(0).add(new Instance(txt.substring(0, Math.min(txt.length() - 1, numChars)), null, rstxt.getString("pubId"), "text"));

                        break;

                    default:
                }
            }

            if (numModalities > 1) {
                logger.info(" Getting side info from the database");
                Statement statement = connection.createStatement();
                statement.setFetchSize(10000);
                ResultSet rs = statement.executeQuery(sql);

                while (rs.next()) {
                    // read the result set

                    switch (experimentType) {

           
                        case D4I:                        
                            if (numModalities > 1) {
                                String tmpJournalStr = rs.getString("Keywords");//.replace("\t", ",");
                                if (tmpJournalStr != null && !tmpJournalStr.equals("")) {
                                    instanceBuffer.get(1).add(new Instance(tmpJournalStr.replace('-', ' ').toLowerCase(), null, rs.getString("pubId"), "Keywords"));
                                }
                            }

                            if (numModalities > 2) {
                                String tmpMeshTermsStr = rs.getString("meshterms");//.replace("\t", ",");
                                if (tmpMeshTermsStr != null && !tmpMeshTermsStr.equals("")) {
                                    instanceBuffer.get(2).add(new Instance(tmpMeshTermsStr.replace('-', ' ').toLowerCase(), null, rs.getString("pubId"), "MeshTerms"));
                                }
                            }

                          

                            break;

                        default:
                    }

                }
            }

            if (numModalities > 3 && (experimentType == ExperimentType.D4I )) {
                logger.info(" Getting DBpedia annotations from the database");
                // get txt data 
                Statement dbPediastatement = connection.createStatement();
                dbPediastatement.setFetchSize(10000);
                
                sql = "select docdbpedia_view.Id,   DBPediaResources  from docdbpedia_view"                        
                        + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");
                              
                ResultSet rs = txtstatement.executeQuery(sql);

                while (rs.next()) {
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
                        instanceBuffer.get(3).add(new Instance(DBPediaResourceStr, null, rs.getString("pubId"), "DBPediaResource"));
                    }
                }
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
                logger.error(e.getMessage());

            }
        }

        logger.info("Read " + instanceBuffer.get(0).size() + " instances modality: " + instanceBuffer.get(0).get(0).getSource().toString());

        if (!ignoreText) {
            try {
                int prunCnt = (int) Math.round(instanceBuffer.get(0).size() * pruneCntPerc);
                GenerateStoplist(tokenizer, instanceBuffer.get(0), prunCnt, pruneMaxPerc, false);
                instances[0].addThruPipe(instanceBuffer.get(0).iterator());
                //Alphabet tmpAlp = instances[0].getDataAlphabet();
                //ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(txtAlphabetFile)));
                //oos.writeObject(tmpAlp);
                //oos.close();
            } catch (IOException e) {
                logger.error("Problem adding text: "
                        + e);

            }
        }

        for (byte m = ignoreText ? (byte) 0 : (byte) 1; m < numModalities; m++) {

            logger.info("Read " + instanceBuffer.get(m).size() + " instances modality: " + (instanceBuffer.get(m).size() > 0 ? instanceBuffer.get(m).get(0).getSource().toString() : m));
            //instances[m] = new InstanceList(new SerialPipes(pipeListCSV));
            instances[m].addThruPipe(instanceBuffer.get(m).iterator());
        }

        logger.info(" instances added through pipe");

        // pruning for all other modalities no text
        for (byte m = ignoreText ? (byte) 0 : (byte) 1; m < numModalities; m++) {
            if (pruneLblCntPerc > 0) {

                // Check which type of data element the instances contain
                Instance firstInstance = instances[m].get(0);
                if (firstInstance.getData() instanceof FeatureSequence) {
                    // Version for feature sequences

                    Alphabet oldAlphabet = instances[m].getDataAlphabet();
                    Alphabet newAlphabet = new Alphabet();

                    // It's necessary to create a new instance list in
                    //  order to make sure that the data alphabet is correct.
                    Noop newPipe = new Noop(newAlphabet, instances[m].getTargetAlphabet());
                    InstanceList newInstanceList = new InstanceList(newPipe);

                    // Iterate over the instances in the old list, adding
                    //  up occurrences of features.
                    int numFeatures = oldAlphabet.size();
                    double[] counts = new double[numFeatures];
                    for (int ii = 0; ii < instances[m].size(); ii++) {
                        Instance instance = instances[m].get(ii);
                        FeatureSequence fs = (FeatureSequence) instance.getData();

                        fs.addFeatureWeightsTo(counts);
                    }

                    Instance instance;

                    // Next, iterate over the same list again, adding 
                    //  each instance to the new list after pruning.
                    while (instances[m].size() > 0) {
                        instance = instances[m].get(0);
                        FeatureSequence fs = (FeatureSequence) instance.getData();

                        int prCnt = (int) Math.round(instanceBuffer.get(m).size() * pruneLblCntPerc);
                        fs.prune(counts, newAlphabet, ((m == 3 && experimentType == ExperimentType.D4I )) ? prCnt * 2 : prCnt);

                        newInstanceList.add(newPipe.instanceFrom(new Instance(fs, instance.getTarget(),
                                instance.getName(),
                                instance.getSource())));

                        instances[m].remove(0);
                    }

//                logger.info("features: " + oldAlphabet.size()
                    //                       + " -> " + newAlphabet.size());
                    // Make the new list the official list.
                    instances[m] = newInstanceList;
                    // Alphabet tmp = newInstanceList.getDataAlphabet();
//                    String modAlphabetFile = dictDir + File.separator + "dict[" + m + "].txt";
//                    try {
//                        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(modAlphabetFile)));
//                        oos.writeObject(tmp);
//                        oos.close();
//                    } catch (IOException e) {
//                        logger.error("Problem serializing modality " + m + " alphabet to file "
//                                + txtAlphabetFile + ": " + e);
//                    }

                } else {
                    throw new UnsupportedOperationException("Pruning features from "
                            + firstInstance.getClass().getName()
                            + " is not currently supported");
                }

            }
        }
        return instances;

    }

    public void createRefACMTables(String SQLConnectionString) {
        //String SQLConnectionString = "jdbc:sqlite:C:/projects/OpenAIRE/fundedarxiv.db";

        Connection connection = null;
        try {

            connection = DriverManager.getConnection(SQLConnectionString);

            Statement statement = connection.createStatement();
            statement.executeUpdate("create table if not exists Author (AuthorId nvarchar(50), FirstName nvarchar(50), LastName nvarchar(50), MiddleName nvarchar(10), Affilication TEXT) ");
            String deleteSQL = String.format("Delete from Author ");
            statement.executeUpdate(deleteSQL);

            statement.executeUpdate("create table if not exists Citation (CitationId nvarchar(50), Reference TEXT) ");
            deleteSQL = String.format("Delete from Citation ");
            statement.executeUpdate(deleteSQL);

            statement.executeUpdate("create table if not exists Category (Category TEXT) ");
            deleteSQL = String.format("Delete from Category ");
            statement.executeUpdate(deleteSQL);

            statement = connection.createStatement();
            statement.executeUpdate("create table if not exists PubAuthor (PubId nvarchar(50), AuthorId nvarchar(50)) ");
            deleteSQL = String.format("Delete from PubAuthor ");
            statement.executeUpdate(deleteSQL);

            statement.executeUpdate("create table if not exists PubCitation (PubId nvarchar(50), CitationId nvarchar(50)) ");
            deleteSQL = String.format("Delete from PubCitation ");
            statement.executeUpdate(deleteSQL);

            statement.executeUpdate("create table if not exists PubCategory (PubId nvarchar(50), Category TEXT) ");
            deleteSQL = String.format("Delete from PubCategory ");
            statement.executeUpdate(deleteSQL);

            PreparedStatement authorBulkInsert = null;
            PreparedStatement citationBulkInsert = null;
            PreparedStatement categoryBulkInsert = null;

            PreparedStatement pubAuthorBulkInsert = null;
            PreparedStatement pubCitationBulkInsert = null;
            PreparedStatement pubCategoryBulkInsert = null;

            String authorInsertsql = "insert into Author values(?,?,?,?,?);";
            String citationInsertsql = "insert into Citation values(?,?);";
            String categoryInsertsql = "insert into Category values(?);";

            String pubAuthorInsertsql = "insert into pubAuthor values(?,?);";
            String pubCitationInsertsql = "insert into pubCitation values(?,?);";
            String pubCategoryInsertsql = "insert into pubCategory values(?,?);";

            TObjectIntHashMap<String> authorsLst = new TObjectIntHashMap<String>();
            TObjectIntHashMap<String> citationsLst = new TObjectIntHashMap<String>();
            TObjectIntHashMap<String> categorysLst = new TObjectIntHashMap<String>();

            try {

                connection.setAutoCommit(false);
                authorBulkInsert = connection.prepareStatement(authorInsertsql);
                citationBulkInsert = connection.prepareStatement(citationInsertsql);
                categoryBulkInsert = connection.prepareStatement(categoryInsertsql);

                pubAuthorBulkInsert = connection.prepareStatement(pubAuthorInsertsql);
                pubCitationBulkInsert = connection.prepareStatement(pubCitationInsertsql);
                pubCategoryBulkInsert = connection.prepareStatement(pubCategoryInsertsql);

                String sql = "	Select articleid,authors_id,authors_firstname,authors_lastname,authors_middlename,authors_affiliation,authors_role, \n"
                        + "			ref_objid,reftext,primarycategory,othercategory \n"
                        + "			 from ACMData1 \n";
                // + "			  LIMIT 10";

                ResultSet rs = statement.executeQuery(sql);

                while (rs.next()) {
                    // read the result set
                    String Id = rs.getString("articleid");

                    String authorIdsStr = rs.getString("authors_id");
                    String[] authorIds = authorIdsStr.split("\t");

                    String authors_firstnamesStr = rs.getString("authors_firstname");
                    String[] authors_firstnames = authors_firstnamesStr.split("\t");

                    String authors_lastnamesStr = rs.getString("authors_lastname");
                    String[] authors_lastnames = authors_lastnamesStr.split("\t");

                    String authors_middlenamesStr = rs.getString("authors_middlename");
                    String[] authors_middlenames = authors_middlenamesStr.split("\t");

                    String authors_affiliationsStr = rs.getString("authors_affiliation");
                    String[] authors_affiliations = authors_affiliationsStr.split("\t");

                    for (int i = 0; i < authorIds.length - 1; i++) {
                        String authorId = authorIds[i];
                        if (!authorsLst.containsKey(authorId)) {
                            authorsLst.put(authorId, 1);
                            String lstName = authors_lastnames.length - 1 > i ? authors_lastnames[i] : "";
                            String fstName = authors_firstnames.length - 1 > i ? authors_firstnames[i] : "";
                            String mName = authors_middlenames.length - 1 > i ? authors_middlenames[i] : "";
                            String affiliation = authors_affiliations.length - 1 > i ? authors_affiliations[i] : "";

                            authorBulkInsert.setString(1, authorId);
                            authorBulkInsert.setString(2, lstName);
                            authorBulkInsert.setString(3, fstName);
                            authorBulkInsert.setString(4, mName);
                            authorBulkInsert.setString(5, affiliation);
                            authorBulkInsert.executeUpdate();
                        }
                        pubAuthorBulkInsert.setString(1, Id);
                        pubAuthorBulkInsert.setString(2, authorId);
                        pubAuthorBulkInsert.executeUpdate();

                    }

                    String citationIdsStr = rs.getString("ref_objid");
                    String[] citationIds = citationIdsStr.split("\t");

                    String citationsStr = rs.getString("reftext");
                    String[] citations = citationsStr.split("\t");

                    for (int i = 0; i < citationIds.length - 1; i++) {
                        String citationId = citationIds[i];
                        if (!citationsLst.containsKey(citationId)) {
                            citationsLst.put(citationId, 1);
                            String ref = citations.length - 1 > i ? citations[i] : "";

                            citationBulkInsert.setString(1, citationId);
                            citationBulkInsert.setString(2, ref);
                            citationBulkInsert.executeUpdate();
                        }
                        pubCitationBulkInsert.setString(1, Id);
                        pubCitationBulkInsert.setString(2, citationId);
                        pubCitationBulkInsert.executeUpdate();
                    }

                    String prCategoriesStr = rs.getString("primarycategory");
                    String[] prCategories = prCategoriesStr.split("\t");

                    String categoriesStr = rs.getString("othercategory");
                    String[] categories = categoriesStr.split("\t");

                    for (int i = 0; i < prCategories.length - 1; i++) {
                        String category = prCategories[i];
                        if (!categorysLst.containsKey(category)) {
                            categorysLst.put(category, 1);
                            categoryBulkInsert.setString(1, category);
                            categoryBulkInsert.executeUpdate();
                        }
                        pubCategoryBulkInsert.setString(1, Id);
                        pubCategoryBulkInsert.setString(2, category);
                        pubCategoryBulkInsert.executeUpdate();
                    }

                    for (int i = 0; i < categories.length - 1; i++) {
                        String category = categories[i];
                        if (!categorysLst.containsKey(category)) {
                            categorysLst.put(category, 1);
                            categoryBulkInsert.setString(1, category);
                            categoryBulkInsert.executeUpdate();
                        }

                        pubCategoryBulkInsert.setString(1, Id);
                        pubCategoryBulkInsert.setString(2, category);
                        pubCategoryBulkInsert.executeUpdate();
                    }

                }

                connection.commit();

            } catch (SQLException e) {

                logger.error("Error in creatingrefACMTables: " + e.getMessage());
                if (connection != null) {
                    try {
                        logger.error("Transaction is being rolled back");

                        connection.rollback();
                    } catch (SQLException excep) {
                        logger.error("Error in ACMReferences extraction" + excep);
                    }
                }
            } finally {

                if (authorBulkInsert != null) {
                    authorBulkInsert.close();
                }
                if (categoryBulkInsert != null) {
                    categoryBulkInsert.close();
                }
                if (citationBulkInsert != null) {
                    citationBulkInsert.close();
                }
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            // if the error message is "out of memory", 
            // it probably means no database file is found
            logger.error(e.getMessage());
        } catch (Exception e) {
            logger.error("File input error: " + e);
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

    }

    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        //Class.forName("org.sqlite.JDBC");
        D4IPTMFlow trainer = new D4IPTMFlow();

    }
}
package org.madgik.MVTopicModel;

import cc.mallet.types.*;
import cc.mallet.pipe.*;
import com.sree.textbytes.jtopia.Configuration;
import com.sree.textbytes.jtopia.TermDocument;
import com.sree.textbytes.jtopia.TermsExtractor;

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

import org.madgik.evaluation.TrendCalculator;
import org.madgik.utils.CSV2FeatureSequence;
import org.madgik.utils.Utils;

public class SciTopicFlow {

    public enum ExperimentType {

        ACM,
        PubMed

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

    public static Logger logger = Logger.getLogger("SciTopic");

    int topWords = 20;
    int showTopicsInterval = 50;
    byte numModalities = 6;

    int numOfThreads = 4;
    int numTopics = 400;
    int numIterations = 800; //Max 2000
    int numChars = 4000;
    int burnIn = 50;
    int optimizeInterval = 50;
    ExperimentType experimentType = ExperimentType.PubMed;

    double pruneCntPerc = 0.002;    //Remove features that appear less than PruneCntPerc* TotalNumberOfDocuments times (-->very rare features)
    double pruneLblCntPerc = 0.002;   //Remove features that appear less than PruneCntPerc* TotalNumberOfDocuments times (-->very rare features)
    double pruneMaxPerc = 10;//Remove features that occur in more than (X)% of documents. 0.05 is equivalent to IDF of 3.0.

    boolean ACMAuthorSimilarity = true;
    boolean calcTopicDistributionsAndTrends = true;
    boolean calcEntitySimilarities = true;
    boolean calcTopicSimilarities = false;
    boolean calcPPRSimilarities = false;
    boolean doRunTopicModelling = true;
    boolean doRunWordEmbeddings = false;
    boolean useTypeVectors = false;
    boolean trainTypeVectors = false;
    boolean findKeyPhrases = false;
    double useTypeVectorsProb = 0.6;
    Net2BoWType PPRenabled = Net2BoWType.OneWay;
    int vectorSize = 200;
    String SQLConnectionString = "jdbc:postgresql://localhost:5432/tender?user=postgres&password=postgres&ssl=false"; //"jdbc:sqlite:C:/projects/OpenAIRE/fundedarxiv.db";
    String experimentId = "";
    int limitDocs = 0;
    boolean D4I = true;

    public SciTopicFlow() throws IOException {
        this(null);
    }

    public SciTopicFlow(Map<String, String> runtimeProp) throws IOException {

        SimilarityType similarityType = SimilarityType.cos; //Cosine 1 jensenShannonDivergence 2 symmetric KLP

        String dictDir = "";

        getPropValues(runtimeProp);

        String experimentString = experimentType.toString() + "_" + numTopics + "T_"
                + numIterations + "IT_" + numChars + "CHRs_" + numModalities + "M_" + (trainTypeVectors ? "WV" : "") + ((limitDocs > 0) ? ("Lmt_" + limitDocs) : "") + PPRenabled.name();

        String experimentDetails = String.format("Multi View Topic Modeling Analysis \n pruneMaxPerc:%.1f  pruneCntPerc:%.4f pruneLblCntPerc:%.4f burnIn:%d numOfThreads:%d similarityType:%s", this.pruneMaxPerc, pruneCntPerc, pruneLblCntPerc, burnIn, numOfThreads, similarityType.toString());

        if (runtimeProp != null) {
            experimentId = runtimeProp.get("ExperimentId");
        }

        if (StringUtils.isBlank(experimentId)) {
            experimentId = experimentString;
        }
        logger.info(String.format("Experiment ID: %s", experimentId));

        if (findKeyPhrases)
            FindKeyPhrasesPerTopic(SQLConnectionString, experimentId, "openNLP");

        if (doRunWordEmbeddings)
            runWordEmbeddings(dictDir);

        if (doRunTopicModelling) {
            runTopicModelling(dictDir, experimentDetails);
        }
        if (calcTopicDistributionsAndTrends) {
            try {
                TrendCalculator.CalcEntityTopicDistributionsAndTrends(SQLConnectionString, experimentId, experimentType);
            } catch (SQLException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        if (calcEntitySimilarities)
            TrendCalculator.calcSimilarities(SQLConnectionString, experimentType, experimentId, ACMAuthorSimilarity, similarityType, numTopics);

        if (calcTopicSimilarities) {
            experimentId = "HEALTHTenderPM_500T_600IT_7000CHRs_10.0 3.0E-4_2.0E-4PRN50B_4M_4TH_cosOneWay";
            TrendCalculator.CalcTopicSimilarities(SQLConnectionString, experimentId);
        }

        if (calcPPRSimilarities)
            TrendCalculator.calcPPRSimilarities(SQLConnectionString);

    }
    public void runWordEmbeddings(String dictDir){
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
    public void runTopicModelling(String dictDir, String experimentDetails){
        logger.info(" TopicModelling has started");
        String batchId = "-1";
        InstanceList[] instances = GenerateAlphabets(SQLConnectionString, experimentType, dictDir, numModalities, pruneCntPerc,
                pruneLblCntPerc, pruneMaxPerc, numChars, PPRenabled,
                false, limitDocs);
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
        try {
            model.estimate();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
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
            if (SQLConnectionString == null) throw new Exception("SQL params missing");
            experimentId = prop.getProperty("ExperimentId");
            limitDocs = Integer.parseInt(prop.getProperty("limitDocs"));

        } catch (Exception e) {
            logger.error("Exception in reading properties: " + e);
            System.exit(1);
        } finally {
            inputStream.close();
        }

    }


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

            String sql = "select doc_topic.TopicId, document.title, document.abstract from \n"
                    + "doc_topic\n"
                    + "inner join publication on doc_topic.pubId= document.doc_id and doc_topic.Weight>0.55 \n"
                    + "where experimentId='" + experimentId + "' \n"
                    + "order by doc_topic.topicid, weight desc";

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

    private void GenerateStoplist(SimpleTokenizer prunedTokenizer, ArrayList<Instance> instanceBuffer, int pruneCount,
                                  double docProportionMaxCutoff, boolean preserveCase) throws IOException {

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
        String[] terms = { "tion", "ing", "ment", "ytem", "wth", "whch", "nfrmatn", "uer", "ther", "frm", "hypermeda",
                "anuae", "dcument", "tudent", "appcatn", "tructure", "prram", "den", "aed", "cmputer", "prram", "mre",
                "cence", "tures", "ture", "ments", "cations", "tems", "tem", "tional", "ity", "ware", "opment", "guage",
                "niques"};
        for (String t: terms) prunedTokenizer.stop(t);

        if (pruneCount > 0) {
            featureCounter.addPrunedWordsToStoplist(prunedTokenizer, pruneCount);
        }
        if (docProportionMaxCutoff < 1.0) {
            docCounter.addPrunedWordsToStoplist(prunedTokenizer, docProportionMaxCutoff);
        }

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



    public InstanceList[] GenerateAlphabets(String SQLConnection, ExperimentType experimentType, String dictDir, byte numModalities,
            double pruneCntPerc, double pruneLblCntPerc, double pruneMaxPerc, int numChars, Net2BoWType PPRenabled, boolean ignoreText, int limitDocs) {

        //String txtAlphabetFile = dictDir + File.separator + "dict[0].txt";
        // Begin by importing documents from text to feature sequences
        ArrayList<Pipe> pipeListText = new ArrayList<Pipe>();

        // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeListText.add(new Input2CharSequence()); //homer
        pipeListText.add(new CharSequenceLowercase());

        String stoplistsPath = ClassLoader.getSystemResource("en.txt").getPath();
        SimpleTokenizer tokenizer = new SimpleTokenizer(new File(stoplistsPath));
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
            if (experimentType == ExperimentType.PubMed) {
                pipeListCSV.add(new CSV2FeatureSequence(alphabetM, ";"));
            } else {
                pipeListCSV.add(new CSV2FeatureSequence(alphabetM, ","));
            }

            if (m == 1 && experimentType == ExperimentType.PubMed) //keywords
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
            String txtsql = "select doctxt_view.docId, text, fulltext from doctxt_view " + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");

            if (experimentType == ExperimentType.PubMed) {
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

            if (experimentType == ExperimentType.ACM) {

                if (PPRenabled == Net2BoWType.PPR) {
                    sql = " select  docid,   citations, categories, period, keywords, venue, DBPediaResources from docsideinfo_view  " + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");
                } else if (PPRenabled == Net2BoWType.OneWay) {

                    sql = " select  docid,  citations, categories, keywords, venue, DBPediaResources from docsideinfo_view " + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");
                } else if (PPRenabled == Net2BoWType.TwoWay) {
                    sql = " select  docid, authors, citations, categories, keywords, venue, DBPediaResources from docsideinfo_view " + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");

                }

            } else if (experimentType == ExperimentType.PubMed) {
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
            if (pruneLblCntPerc > 0 & instances[m].size() > 10) {

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
                        fs.prune(counts, newAlphabet, ((m == 4 && experimentType == ExperimentType.ACM && PPRenabled == Net2BoWType.PPR) || (m == 3 && experimentType == ExperimentType.PubMed)) ? prCnt * 4 : prCnt);

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


    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        //Class.forName("org.sqlite.JDBC");
        SciTopicFlow trainer = new SciTopicFlow();

    }
}

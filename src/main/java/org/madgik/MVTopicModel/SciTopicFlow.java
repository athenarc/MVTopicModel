package org.madgik.MVTopicModel;

import cc.mallet.pipe.*;
import cc.mallet.types.*;
import com.sree.textbytes.jtopia.Configuration;
import com.sree.textbytes.jtopia.TermDocument;
import com.sree.textbytes.jtopia.TermsExtractor;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.madgik.config.Config;
import org.madgik.dbpediaspotlightclient.DBpediaAnnotator;
import org.madgik.io.JsonTMDataSource;
import org.madgik.io.TMDataSource;
import org.madgik.io.TMDataSourceFactory;
import org.madgik.io.modality.Modality;
import org.madgik.io.modality.Text;
import org.madgik.utils.CSV2FeatureSequence;
import org.madgik.config.Config.ExperimentType;

import java.io.*;
import java.sql.*;
import java.util.*;


public class SciTopicFlow {

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

    public static String LOGGERNAME = "SciTopic";
    public static Logger LOGGER = Logger.getLogger(LOGGERNAME);

    private byte numModalities = 6;

    private int numOfThreads = 4;
    private int numChars = 4000;
    private int burnIn = 50;
    private int optimizeInterval = 50;
    private Config.ExperimentType experimentType = Config.ExperimentType.PubMed;

    private double pruneCntPerc = 0.002;    //Remove features that appear less than PruneCntPerc* TotalNumberOfDocuments times (-->very rare features)
    private double pruneLblCntPerc = 0.002;   //Remove features that appear less than PruneCntPerc* TotalNumberOfDocuments times (-->very rare features)
    private double pruneMaxPerc = 10;//Remove features that occur in more than (X)% of documents. 0.05 is equivalent to IDF of 3.0.

    private boolean runTopicModelling = false;
    private boolean runInference = false;
    private boolean runWordEmbeddings = false;
    private boolean useTypeVectors = false;
    private boolean trainTypeVectors = false;
    private boolean findKeyPhrases = false;

    private double useTypeVectorsProb = 0.2;
    private int vectorSize = 200;
    private String SQLConnectionString = "jdbc:postgresql://localhost:5432/tender?user=postgres&password=postgres&ssl=false"; //"jdbc:sqlite:C:/projects/OpenAIRE/fundedarxiv.db";
    private String experimentId = "";
    private int limitDocs = 1000;

    public static String TOPICMODELLING="topicmodelling";
    public static String INFERENCE="inference";
    public SciTopicFlow() throws IOException {
        this(null, null);
    }

    public SciTopicFlow(Map<String, String> runtimeProp, String[] mode) throws IOException {
        if (mode == null) {
            mode = new String[1];
            mode[0]= SciTopicFlow.TOPICMODELLING;
        }
        if (Arrays.asList(mode).contains(SciTopicFlow.TOPICMODELLING))
            runTopicModelling=true;
        else
            runTopicModelling = false;

        if (Arrays.asList(mode).contains(SciTopicFlow.INFERENCE)) runInference=true;
        else runInference=false;

        Config config = new Config("config.properties");
        //LOGGER.setLevel(Level.DEBUG);
        String experimentString = config.makeExperimentString();
        config.makeExperimentDetails();

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
            LOGGER.info(" calc word embeddings starting");
            InstanceList[] instances = ImportInstancesWithNewPipes(ReadDataFromDB(SQLConnectionString, experimentType, numModalities, limitDocs, ""), config);

            LOGGER.info(" instances added through pipe");

            //int numDimensions = 50;
            int windowSizeOption = 5;
            int numSamples = 5;
            int numEpochs = 5;
            WordEmbeddings matrix = new WordEmbeddings(instances[0].getDataAlphabet(), vectorSize, windowSizeOption);
            //TopicWordEmbeddings matrix = new TopicWordEmbeddings(instances[0].getDataAlphabet(), vectorSize, windowSizeOption,0);
            matrix.queryWord = "skin";
            matrix.countWords(instances[0], 0.0001); //Sampling factor : "Down-sample words that account for more than ~2.5x this proportion or the corpus."
            matrix.train(instances[0], numOfThreads, numSamples, numEpochs);
            LOGGER.info(" calc word embeddings ended");
            //PrintWriter out = new PrintWriter("vectors.txt");
            //matrix.write(out);
            //out.close();
            matrix.write(SQLConnectionString, 0);
            LOGGER.info(" writing word embeddings ended");
        }

        if (runTopicModelling) {
            runTopicModelling(config);
        }

        if (runInference)
            runInference(config);

            Logger.getLogger(SciTopicFlow.LOGGERNAME).info("SciTopicFlow done.");
        }


        void runTopicModelling(Config config) throws IOException {
            int numModalities = config.getNumModalities();
            LOGGER.info(" TopicModelling has started");

            // input data
            TMDataSource inputDS = TMDataSourceFactory.instantiate(config.getModellingInputDataSourceType(), config.getModellingInputDataSourceParams());
            inputDS.getModellingInputs(config);
            if (config.doComputeNewSemanticAugmentations()) computeSemanticAnnotations(inputDS, config);
            inputDS.processInputs(config);

            ArrayList<ArrayList<Instance>> instanceBuffer = inputDS.getInputInstances();

            InstanceList[] instances = ImportInstancesWithNewPipes(instanceBuffer, config);
            // save tokenized instances
            new JsonTMDataSource("tokenized_instances").writeTokenized(instances, config.getModalities(), "tokenized.json");
            LOGGER.info("Instances added through pipe");

            // model init
            double beta = 0.01;
            double[] betaMod = new double[numModalities];
            Arrays.fill(betaMod, 0.01);
            boolean useCycleProposals = false;
            double alpha = 0.1;
            double[] alphaSum = new double[numModalities];
            Arrays.fill(alphaSum, 1);
            double[] gamma = new double[numModalities];
            Arrays.fill(gamma, 1);
            FastQMVWVParallelTopicModel model = new FastQMVWVParallelTopicModel(config.getNumTopics(), (byte) numModalities, alpha, beta,
                    useCycleProposals, config.getModellingInputDataSourceParams(), useTypeVectors, useTypeVectorsProb, trainTypeVectors, config.getSeed());
            model.setRandomSeed(config.getSeed());
            model.setSaveSerializedModel(50, "experimentId.model");
            model.setNumIterations(config.getNumIterations());
            model.setTopicDisplay(config.getShowTopicsInterval(), config.getNumTopWords());
            // model.setIndependentIterations(independentIterations);
            model.optimizeInterval = optimizeInterval;
            model.burninPeriod = burnIn;
            model.setNumThreads(config.getNumOfThreads());
            model.addInstances(instances, "-1", vectorSize, null);//trainingInstances);//instances);
            LOGGER.info(" instances added");

            LOGGER.info("Deleting previous experiment");
            TMDataSource output = TMDataSourceFactory.instantiate(config.getModellingOutputDataSourceType(), config.getModellingOutputDataSourceParams());
            output.deleteExistingExperiment(config);
            // model.DeletePreviousExperiment(config.getModellingOutputDataSourceParams(), config.getExperimentId());

            model.estimate();
            LOGGER.info("Model estimated");
            model.doPostEstimationCalculations();

            Object modelToSave = model.getInferencer();
            if (output.isNeedsSerializedModel())
                modelToSave = model.getSerializedModel();
            output.saveTopicsAndExperiment(config, model.getTopicAnalysisList(), model.getTopicDetailsList(), modelToSave, model.getExperimentMetadata());
            model.computeDocumentAssignments(0.00);
            output.saveDocumentTopicAssignments(config, model.getDocTopicMap(), config.getModellingOutputDataSourceParams());
            // model.saveResults(config.getModellingInputDataSourceParams(), config.getExperimentId(), config.getExperimentDetails());
            LOGGER.info("Model saved");

            LOGGER.info("Model Id: \n" + config.getExperimentId());
            LOGGER.info("Model Metadata: \n" + model.getExpMetadata());

            //if (modelEvaluationFile != null) {
            try {

                FastQMVWVTopicModelDiagnostics diagnostics = new FastQMVWVTopicModelDiagnostics(model, config.getNumTopWords());
                diagnostics.calcBundledScores();
                output.saveDiagnostics(config, diagnostics.getBundledScores());
                // diagnostics.saveToDB(config.getModellingInputDataSourceParams(), experimentId, perplexity, "-1");
                LOGGER.info("full diagnostics calculation finished");

            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }

        void computeSemanticAnnotations(TMDataSource inferenceDs, Config config){
            if (config.getModalities().contains(Modality.type.dbpedia.name())) {
                LOGGER.warn("Specified to extract modalities but they are already contained in the loaded inference data. Will replace.");
                inferenceDs.clearRawInputData(Modality.type.dbpedia.name());
            }
            DBpediaAnnotator dbann = new DBpediaAnnotator(config);
            List<Text> texts = new ArrayList<>();
            for(Modality text: inferenceDs.getRawInput(Modality.text())) texts.add((Text) text);
            dbann.setSemanticAugmentationInputs(texts);
            // run semantic augmentation and enrichment
            dbann.annotatePubs();
            dbann.updateResourceDetails();
            List<Modality> sems = dbann.getSemanticAnnotationModalityList();
            inferenceDs.setRawInput(sems, Modality.type.dbpedia.name());
        }

        void runInference(Config config) throws IOException {

                LOGGER.info("Inference on new docs has started");

                LOGGER.info("Loading inference model");

                TMDataSource infModelDS = TMDataSourceFactory.instantiate(config.getInferenceModelDataSourceType(), config.getInferenceModelDataSourceParams());
                FastQMVWVTopicInferencer inferencer = infModelDS.getInferenceModel(config);
                inferencer.setRandomSeed(config.getSeed());

                LOGGER.info("Loaded inference model");

                LOGGER.info("Getting input data for inference");
                TMDataSource inferenceDs = TMDataSourceFactory.instantiate(config.getInferenceDataSourceType(), config.getInferenceDataSourceParams());
                inferenceDs.getInferenceInputs(config);

                if (config.doComputeNewSemanticAugmentations()) computeSemanticAnnotations(inferenceDs, config);

                inferenceDs.processInputs(config);
                ArrayList<ArrayList<Instance>> instanceBuffer = inferenceDs.getInputInstances();

                InstanceList[] instances = ImportInstancesWithExistingPipes(instanceBuffer, inferencer.getPipes(), config.getNumModalities());
                LOGGER.info("Added " + instances.length + " instances through pipe. Beginning inference.");

                inferencer.inferTopicDistributionsOnNewDocs(instances);
                inferencer.computeDocumentAssignments(0.00);

                // write output
                TMDataSource infOutDS = TMDataSourceFactory.instantiate(config.getInferenceOutputDataSourceType(), config.getInferenceOutputDataSourceParams());
                infOutDS.saveDocumentTopicAssignments(config, inferencer.getDocTopicMap(), config.getInferenceOutputDataSourceParams());
                LOGGER.info("Inference on " + instances[0].size() + " instances completed.");
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

            LOGGER.info("Finding key phrases calculation started");

            String sql = "select doc_topic.TopicId, document.title, document.abstract from \n"
                    + "doc_topic\n"
                    + "inner join document on doc_topic.docId= document.docid and doc_topic.Weight>0.55 \n"
                    + "where experimentId='" + experimentId + "' \n"
                    + "order by doc_topic.topicid, weight desc";

            ResultSet rs = statement.executeQuery(sql);

            HashMap<Integer, Map<String, ArrayList<Integer>>> topicTitles;

            topicTitles = new HashMap<Integer, Map<String, ArrayList<Integer>>>();

            Integer topicId = -1;

            while (rs.next()) {

                int newTopicId = rs.getInt("TopicId");

                if (newTopicId != topicId && topicId != -1) {
                    LOGGER.info("Finding key phrases for topic " + topicId);
                    topiaDoc = termExtractor.extractTerms(stringBuffer.toString());
                    topicTitles.put(topicId, topiaDoc.getFinalFilteredTerms());
                    stringBuffer = new StringBuffer();
                }
                stringBuffer.append(rs.getString("title").replace('-', ' ').toLowerCase() + "\n");
                //stringBuffer.append(rs.getString("abstract").replace('-', ' ').toLowerCase() + "\n");
                topicId = newTopicId;

            }

            LOGGER.info("Finding key phrases for topic " + topicId);
            topiaDoc = termExtractor.extractTerms(stringBuffer.toString());
            topicTitles.put(topicId, topiaDoc.getFinalFilteredTerms());

            statement.executeUpdate("create table if not exists TopicKeyPhrase ( TopicId Integer, Tagger TEXT, Phrase Text, Count Integer, WordsNum Integer, Weight numeric, ExperimentId TEXT) ");
            String deleteSQL = String.format("Delete from TopicKeyPhrase WHERE ExperimentId='" + experimentId + "' AND Tagger ='" + tagger + "'");
            statement.executeUpdate(deleteSQL);

            PreparedStatement bulkInsert = null;
            sql = "insert into TopicKeyPhrase values(?,?,?,?,?,?,?);";

            LOGGER.info("Saving key phrases....");
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

                LOGGER.error("Error in insert topicPhrases: " + e);
                if (connection != null) {
                    try {
                        LOGGER.error("Transaction is being rolled back");
                        connection.rollback();
                    } catch (SQLException excep) {
                        LOGGER.error("Error in insert topicPhrases: " + excep);
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
            LOGGER.error(e.getMessage());
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                LOGGER.error(e);
            }
        }

        LOGGER.info("Finding Key phrases finished");

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
                    LOGGER.error("Transaction is being rolled back");
                    connection.rollback();
                } catch (SQLException excep) {
                    LOGGER.error("Error in insert TokensPerEntity");
                }
            }
        } finally {
            try {
                if (bulkInsert != null) {
                    bulkInsert.close();
                }
                connection.setAutoCommit(true);
            } catch (SQLException excep) {
                LOGGER.error("Error in insert TokensPerEntity");
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
            String word = wordIter.next();

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
    }

    // Read data from DB and convert them to a list of instances per view (modality)
    // MVTopicModelModality 0 should be text (e.g., "This a new doc. We will tokenize it later"
    // For all other modalities (metadata) we end up with a comma delimeted string (i.e., "keyword1, key phrase1, keyword2, keyword2")
    // Some modalities may be missing for some docs
    public ArrayList<ArrayList<Instance>> ReadDataFromDB(String SQLConnection, ExperimentType experimentType, byte numModalities, int limitDocs, String filter) {
        ArrayList<ArrayList<Instance>> instanceBuffer = new ArrayList<ArrayList<Instance>>(numModalities);

        //createCitationGraphFile("C:\\projects\\Datasets\\DBLPManage\\acm_output_NET.csv", "jdbc:sqlite:C:/projects/Datasets/DBLPManage/acm_output.db");
        for (byte m = 0; m < numModalities; m++) {
            instanceBuffer.add(new ArrayList<Instance>());

        }

        Connection connection = null;
        try {

            connection = DriverManager.getConnection(SQLConnection);
            connection.setAutoCommit(false);

            String sql = "";
            // String txtsql = "select doctxt_view.docId, text, fulltext from doctxt_view " + filter + " Order by doctxt_view.docId " + ((limitDocs > 0) ? String.format(" LIMIT %d", limitDocs) : "");
            String txtsql =  "select distinct ON (document.id)  document.id as docid, " +
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

    public InstanceList[] ImportInstancesWithExistingPipes(ArrayList<ArrayList<Instance>> instanceBuffer, Pipe[] existingPipes, byte numModalities) {
        if (existingPipes.length < numModalities) {
            LOGGER.error("ImportDataWithExistingPipes: Missing existing pipes");
            return null;

        }

        InstanceList[] instances = new InstanceList[numModalities];

        for (byte m = 0; m < numModalities; m++) {
            instances[m] = new InstanceList(existingPipes[m]);
            instances[m].addThruPipe(instanceBuffer.get(m).iterator());
        }

        return instances;

    }

    public InstanceList[] ImportInstancesWithNewPipes(ArrayList<ArrayList<Instance>> instanceBuffer, Config config){

        double pruneCntPerc = config.getPruneCntPerc();
        double pruneLblCntPerc = config.getPruneLblCntPerc();
        double pruneMaxPerc = config.getPruneMaxPerc();
        boolean ignoreText = config.isIgnoreText();
        int numModalities = config.getNumModalities();
        String csvDelimeter = ";";

        InstanceList[] instances = new InstanceList[numModalities];

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
        if (!ignoreText) {
            instances[0] = new InstanceList(new SerialPipes(pipeListText));            
            
        }
        // Other Modalities
        for (byte m = ignoreText ? (byte) 0 : (byte) 1; m < numModalities; m++) {
            Alphabet alphabetM = new Alphabet();
            ArrayList<Pipe> pipeListCSV = new ArrayList<Pipe>();
            pipeListCSV.add(new CSV2FeatureSequence(alphabetM, csvDelimeter));
            instances[m] = new InstanceList(new SerialPipes(pipeListCSV));
        }


        int textIndex = config.getModalities().indexOf(Modality.text());
        if (!ignoreText) {
            try {
                int prunCnt = (int) Math.round(instanceBuffer.get(textIndex).size() * pruneCntPerc);
                GenerateStoplist(tokenizer, instanceBuffer.get(textIndex), prunCnt, pruneMaxPerc, false);
                instances[0].addThruPipe(instanceBuffer.get(textIndex).iterator());
            } catch (IOException e) {
                LOGGER.error("Problem adding text: " + e);
            }
        }

        for (byte m = ignoreText ? (byte) 0 : (byte) 1; m < numModalities; m++) {

            instances[m].addThruPipe(instanceBuffer.get(m).iterator());
        }

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
                    SerialPipes newPipe =  new SerialPipes(((SerialPipes) instances[m].getPipe()).pipes()); 
                    //new Noop(newAlphabet, instances[m].getTargetAlphabet());
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
                        fs.prune(counts, newAlphabet, ((m == 3)) ? prCnt * 4 : prCnt);

                        newInstanceList.add(newPipe.instanceFrom(new Instance(fs, instance.getTarget(),
                                instance.getName(),
                                instance.getSource())));

                        instances[m].remove(0);
                    }
                    instances[m] = newInstanceList;

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
        String mode = "topicmodelling";
        if (args.length > 0){
            mode = args[0];
        }
        SciTopicFlow trainer = new SciTopicFlow(null, args);

    }
}

package org.madgik.MVTopicModel;

import cc.mallet.types.*;
import cc.mallet.pipe.*;

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

import org.madgik.config.Config;
import org.madgik.evaluation.TrendCalculator;
import org.madgik.io.SerializedFileTMDataSource;
import org.madgik.io.TMDataSourceFactory;
import org.madgik.io.TMDataSource;
import org.madgik.preproc.KeywordExtractor;
import org.madgik.preproc.TextPreprocessor;
import org.madgik.utils.CSV2FeatureSequence;
import org.madgik.utils.Utils;


public class SciTopicFlow {

    public static final String LOGGER = "SciTopic";
    public static Logger logger;

//    int topWords = 20;
//    int showTopicsInterval = 50;
//    byte numModalities = 6;
//
//    int numOfThreads = 4;
//    int numTopics = 400;
//    int numIterations = 800; //Max 2000
//    int numChars = 4000;
//    int burnIn = 50;
//    int optimizeInterval = 50;


//    double pruneCntPerc = 0.002;    //Remove features that appear less than PruneCntPerc* TotalNumberOfDocuments times (-->very rare features)
//    double pruneLblCntPerc = 0.002;   //Remove features that appear less than PruneCntPerc* TotalNumberOfDocuments times (-->very rare features)
//    double pruneMaxPerc = 10;//Remove features that occur in more than (X)% of documents. 0.05 is equivalent to IDF of 3.0.

//    boolean ACMAuthorSimilarity = true;
//    boolean calcTopicDistributionsAndTrends = true;
//    boolean calcEntitySimilarities = true;
//    boolean calcTopicSimilarities = false;
//    boolean calcPPRSimilarities = false;
//    boolean findKeyPhrases = false;

//    boolean useTypeVectors = false;
//    boolean trainTypeVectors = false;
//    double useTypeVectorsProb = 0.6;
//    int vectorSize = 200;

//    String SQLConnectionString = "jdbc:postgresql://localhost:5432/tender?user=postgres&password=postgres&ssl=false"; //"jdbc:sqlite:C:/projects/OpenAIRE/fundedarxiv.db";

//    String experimentId = "";

    int limitDocs = 0;
    boolean D4I = true;


    /**
     * Main driver method for the topic modelling workflow.
     * @param runtimeProp
     * @throws IOException
     */
    public SciTopicFlow(Map<String, String> runtimeProp) throws IOException {

        logger = Logger.getLogger(SciTopicFlow.LOGGER);

        Config config = new Config("config.properties");
        String experimentString = config.makeExperimentString();
        config.makeExperimentDetails();

        if (runtimeProp != null) config.setExperimentId(runtimeProp.get("ExperimentId"));
        if (StringUtils.isBlank(config.getExperimentId())) config.setExperimentId(experimentString);
        String experimentId = config.getExperimentId();

        logger.info(String.format("Experiment ID: %s", experimentId));

        if (config.isFindKeyPhrases())
            FindKeyPhrasesPerTopic(config);




        // output.saveResults(model.getTopicData(), model.getPhraseData(), model.getTopicDetails(), batchId,
                // config.getExperimentId(), config.getExperimentDetails(), model.getExperimentMetadata());
        // model.saveResults(config.getDataSourceParams(), experimentId, experimentDetails);
        // logger.info("Model Id: \n" + config.getExperimentId());
        // logger.info("Model Metadata: \n" + model.getExpMetadata());


        FastQMVWVParallelTopicModel model = runTopicModelling(config);
        if (model == null) return;
        TrendCalculator trendCalc = new TrendCalculator();
        trendCalc.setConfig(config);
        trendCalc.doPostAnalysis(experimentId);

    }
    public FastQMVWVParallelTopicModel runTopicModelling(Config config){
        logger.info(" TopicModelling has started");
        String batchId = "-1";

        InstanceList[] instances = GenerateAlphabets(config);
        logger.info("Instances added through pipe");

        double beta = 0.01;
        double[] betaMod = new double[config.getNumModalities()];
        Arrays.fill(betaMod, 0.01);
        boolean useCycleProposals = false;
        double alpha = 0.1;

        double[] alphaSum = new double[config.getNumModalities()];
        Arrays.fill(alphaSum, 1);

        double[] gamma = new double[config.getNumModalities()];
        Arrays.fill(gamma, 1);

        //double gammaRoot = 4;
        FastQMVWVParallelTopicModel model = new FastQMVWVParallelTopicModel(config.getNumTopics(),
                config.getNumModalities(), alpha, beta, useCycleProposals,
                false, 0.0, false);

        TMDataSource output = TMDataSourceFactory.instantiate(config.getOutputDataSourceType(), config.getOutputDataSourceParams());
        if (output == null){
            logger.error("Topic modelling run failed.");
            return null;
        }
        output.prepareOutput(config.getExperimentId());
        //model.CreateTables(config.getDataSourceParams(), config.getExperimentId());

        // ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);
        model.setNumIterations(config.getNumIterations());
        model.setTopicDisplay(config.getShowTopicsInterval(), config.getNumTopWords());
        // model.setIndependentIterations(independentIterations);
        model.setOptimizeInterval(config.getOptimizeInterval());
        model.setBurninPeriod(config.getBurnIn());
        model.setNumThreads(config.getNumOfThreads());

        model.addInstances(instances, batchId, 200, config.getInitModelFile());//trainingInstances);//instances);
        logger.info(" instances added");

        //model.readWordVectorsDB(SQLConnectionString, vectorSize);
        try {
            model.estimate();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        logger.info("Model estimated");

        model.gatherTopics();
        output.saveResults(model.getTopicData(), model.getDocTopics(), batchId, config.getExperimentId(), config.getExperimentDetails(), model.getExperimentMetadata());
        // model.saveResults(config.getDataSourceParams(), experimentId, experimentDetails);
        logger.info("Model saved");

        logger.info("Model Id: \n" + config.getExperimentId());
        logger.info("Model Metadata: \n" + model.getExpMetadata());

        //if (modelEvaluationFile != null) {
        // do inference
        try {
            List<FastQMVWVTopicModelDiagnostics.TopicScores> inferenceResults = model.doInference(config.getNumTopWords());
            double[][] perplexities = model.getPerplexities();
            //FastQMVWVTopicModelDiagnostics diagnostics = new FastQMVWVTopicModelDiagnostics(model, config.getNumTopWords());
            output.saveDiagnostics(config.getNumModalities(), batchId, config.getExperimentId(), model.getPerplexities(),
                    config.getNumTopics(), inferenceResults);
            // diagnostics.saveToDB(config.getOutputDataSourceParams(), config.getExperimentId(), 0, batchId);
            logger.info("full diagnostics calculation finished");

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return model;

    }

    private void FindKeyPhrasesPerTopic(Config config) {
        // get topics
        TMDataSource ds = TMDataSourceFactory.instantiate(config.getInputDataSourceType(), config.getInputDataSourceParams());
        if( ds == null){
            System.exit(1) ;
        }
        Map<Integer, String> topicIdsTitles = ds.getTopics(config.getExperimentId());
        KeywordExtractor kwe = new KeywordExtractor();
        kwe.FindKeyPhrasesPerTopic(topicIdsTitles, config);
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



    public InstanceList[] GenerateAlphabets(Config config) {


        Config.ExperimentType experimentType = config.getExperimentType();
        byte numModalities = config.getNumModalities();
        boolean ignoreText = config.isIgnoreText();

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

        InstanceList[] instances = new InstanceList[numModalities];

        if (!ignoreText) {
            instances[0] = new InstanceList(new SerialPipes(pipeListText));
        }
        // Other Modalities
        for (byte m = ignoreText ? (byte) 0 : (byte) 1; m < numModalities; m++) {
            Alphabet alphabetM = new Alphabet();
            ArrayList<Pipe> pipeListCSV = new ArrayList<Pipe>();
            if (experimentType == Config.ExperimentType.PubMed) {
                pipeListCSV.add(new CSV2FeatureSequence(alphabetM, ";"));
            } else {
                pipeListCSV.add(new CSV2FeatureSequence(alphabetM, ","));
            }

            if (m == 1 && experimentType == Config.ExperimentType.PubMed) //keywords
            {
                //  pipeListCSV.add(new FeatureSequenceRemovePlural(alphabetM));
            }
            instances[m] = new InstanceList(new SerialPipes(pipeListCSV));
        }
        //createCitationGraphFile("C:\\projects\\Datasets\\DBLPManage\\acm_output_NET.csv", "jdbc:sqlite:C:/projects/Datasets/DBLPManage/acm_output.db");

        // get inputs
        TMDataSource ds = TMDataSourceFactory.instantiate(config.getInputDataSourceType(), config.getInputDataSourceParams());
        ArrayList<ArrayList<Instance>> instanceBuffer = ds.getInputs(config);
        // logger.info("Read " + instanceBuffer.get(0).size() + " instances modality: " + instanceBuffer.get(0).get(0).getSource().toString());

        // apply source preprocessing
        TextPreprocessor preproc = new TextPreprocessor(config);
        return preproc.preprocess(instanceBuffer, instances, tokenizer);
    }

    public SciTopicFlow() throws IOException {
        this(null);
    }

    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        //Class.forName("org.sqlite.JDBC");
        SciTopicFlow trainer = new SciTopicFlow();

    }
}

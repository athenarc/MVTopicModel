package org.madgik.preproc;

import cc.mallet.pipe.*;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import org.apache.log4j.Logger;
import org.madgik.MVTopicModel.SciTopicFlow;
import org.madgik.config.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Text preprocessing class
 */
public class TextPreprocessor {
    Logger logger = Logger.getLogger(SciTopicFlow.LOGGER);
    Config config;

    public TextPreprocessor(Config config) {
        this.config = config;
    }

    public InstanceList[] preprocess(ArrayList<ArrayList<Instance>> instanceBuffer, InstanceList[] instances, SimpleTokenizer tokenizer){

        preprocessText(instanceBuffer, instances, tokenizer);
        preprocessMetadata(instanceBuffer, instances);
        return instances;
    }


    public void preprocessMetadata(ArrayList<ArrayList<Instance>> instanceBuffer, InstanceList[] instances){

        // pruning for all other modalities no text
        for (byte m = config.isIgnoreText() ? (byte) 0 : (byte) 1; m < config.getNumModalities(); m++) {
            if (config.getPruneLblCntPerc() > 0 & instances[m].size() > 10) {

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

                        int prCnt = (int) Math.round(instanceBuffer.get(m).size() * config.getPruneLblCntPerc());
                        fs.prune(counts, newAlphabet, ((m == 4 && config.getExperimentType() == Config.ExperimentType.ACM
                                && config.getPPRenabled() == Config.Net2BoWType.PPR) ||
                                (m == 3 && config.getExperimentType() == Config.ExperimentType.PubMed)) ? prCnt * 4 : prCnt);

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



    }

    public void preprocessText(ArrayList<ArrayList<Instance>> instanceBuffer, InstanceList[] instances, SimpleTokenizer tokenizer){
        if (config.isIgnoreText()) {
            // add as-is and exit
            for (byte m = 0; m < config.getNumModalities(); m++) {

                logger.info("Read " + instanceBuffer.get(m).size() + " instances modality: " +
                        (instanceBuffer.get(m).size() > 0 ? instanceBuffer.get(m).get(0).getSource().toString() : m));
                //instances[m] = new InstanceList(new SerialPipes(pipeListCSV));
                instances[m].addThruPipe(instanceBuffer.get(m).iterator());
            }
            return;
        }
        // generate and link stoplist
        try {
            int prunCnt = (int) Math.round(instanceBuffer.get(0).size() * config.getPruneCntPerc());
            GenerateStoplist(tokenizer, instanceBuffer.get(0), prunCnt, config.getPruneMaxPerc(), false);
            instances[0].addThruPipe(instanceBuffer.get(0).iterator());
            //Alphabet tmpAlp = instances[0].getDataAlphabet();
            //ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(txtAlphabetFile)));
            //oos.writeObject(tmpAlp);
            //oos.close();
        } catch (IOException e) {
            logger.error("Problem adding text: " + e);

        }

        logger.info("Preprocessed text instances added through pipe");
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


}

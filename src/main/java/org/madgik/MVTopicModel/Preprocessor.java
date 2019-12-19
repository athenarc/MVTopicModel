package org.madgik.MVTopicModel;

import org.apache.log4j.Logger;
import org.madgik.config.Config;
import org.madgik.io.modality.Modality;


/**
 * Text preprocessing class
 */
public class Preprocessor {
    final Logger LOGGER = Logger.getLogger(SciTopicFlow.LOGGERNAME);
    Config config;
    public Preprocessor(Config config) {
        this.config = config;
    }

    public String preprocess(String content, String modality){
        if (modality.equals(Modality.type.text.toString())) return preprocessText(content);
        if (modality.equals(Modality.type.keywords.toString())) return preprocessKeywords(content);
        if (modality.equals(Modality.type.mesh.toString())) return preprocessMesh(content);
        if (modality.equals(Modality.type.dbpedia.toString())) return preprocessDBPedia(content);
        LOGGER.error("Undefined modality " + modality);
        return null;
    }
    public String preprocessText(String text){
        return text.substring(0, Math.min(text.length() - 1, config.getNumChars()));
    }
    public String preprocessKeywords(String kw){
        return kw.replace('-', ' ').toLowerCase();
    }
    public String preprocessMesh(String mesh){
        return mesh.replace('-', ' ').toLowerCase();
    }
    public String preprocessDBPedia(String dbpedia){
        //http://dbpedia.org/resource/Aerosol:3;http://dbpedia.org/resource/Growth_factor:4;http://dbpedia.org/resource/Hygroscopy:4;http://dbpedia.org/resource/Planetary_boundary_layer:3
        if (dbpedia == null || dbpedia.equals("")) return null;
        String DBPediaResourceStr = "";
        String[] DBPediaResources = dbpedia.trim().split(";");
        for (int j = 0; j < DBPediaResources.length; j++) {
            String[] pairs = DBPediaResources[j].trim().split("#");
            if (pairs.length == 2) {
                for (int i = 0; i < Integer.parseInt(pairs[1]); i++)
                    DBPediaResourceStr += pairs[0] + ";";
            } else
                DBPediaResourceStr += DBPediaResources[j] + ";";
        }
        DBPediaResourceStr = DBPediaResourceStr.substring(0, DBPediaResourceStr.length() - 1);
        return DBPediaResourceStr;
    }
}

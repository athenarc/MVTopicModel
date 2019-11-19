package org.madgik.model;


// lightweight class for document visualization purposes
public class LightDocument {
    String id;

    public LightDocument(String id, String type, String content, String pubyear, String journal, String project) {
        this.id = id;
        this.type = type;
        this.content = content;
        this.pubyear = pubyear;
        this.journal = journal;
        this.project = project;
    }

    String type;


    String content;
    String pubyear;
    String journal;
    String project;
}

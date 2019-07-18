package org.madgik.persistence.entities;

import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Immutable
@Table(name = "visualization_documents")
public class VisualizationDocument extends AbstractEntity {

    @Column(name = "pubyear")
    private String pubyear;
    @Column(name = "abstract")
    private String abstractField;
    @Column(name = "abstract_pmc")
    private String abstractPmc;
    @Column(name = "other_abstract_pmc")
    private String otherAbstractPmc;
    @Column(name = "doctype")
    private String doctype;
    @Column(name = "projectacronym")
    private String projectAcronym;
    @Column(name = "project")
    private String project;
    @Column(name = "journal")
    private String journal;
    @Column(name = "doi_id")
    private String doiId;
    @Column(name = "pmc_id")
    private String pmcId;

    public VisualizationDocument() {

    }

    public VisualizationDocument(String id, String pubyear, String abstractField, String abstractPmc, String otherAbstractPmc, String doctype, String projectAcronym,
                                 String project, String journal, String doiId, String pmcId) {
        this.id = id;
        this.pubyear = pubyear;
        this.abstractField = abstractField;
        this.abstractPmc = abstractPmc;
        this.otherAbstractPmc = otherAbstractPmc;
        this.doctype = doctype;
        this.projectAcronym = projectAcronym;
        this.project = project;
        this.journal = journal;
        this.doiId = doiId;
        this.pmcId = pmcId;
    }

    public VisualizationDocument(String pubyear, String abstractField, String abstractPmc, String otherAbstractPmc, String doctype, String projectAcronym,
                                 String project, String journal, String doiId, String pmcId) {
        this.pubyear = pubyear;
        this.abstractField = abstractField;
        this.abstractPmc = abstractPmc;
        this.otherAbstractPmc = otherAbstractPmc;
        this.doctype = doctype;
        this.projectAcronym = projectAcronym;
        this.project = project;
        this.journal = journal;
        this.doiId = doiId;
        this.pmcId = pmcId;
    }

    public String getPubyear() {
        return pubyear;
    }

    public void setPubyear(String pubyear) {
        this.pubyear = pubyear;
    }

    public String getAbstractField() {
        return abstractField;
    }

    public void setAbstractField(String abstractField) {
        this.abstractField = abstractField;
    }

    public String getAbstractPmc() {
        return abstractPmc;
    }

    public void setAbstractPmc(String abstractPmc) {
        this.abstractPmc = abstractPmc;
    }

    public String getOtherAbstractPmc() {
        return otherAbstractPmc;
    }

    public void setOtherAbstractPmc(String otherAbstractPmc) {
        this.otherAbstractPmc = otherAbstractPmc;
    }

    public String getDoctype() {
        return doctype;
    }

    public void setDoctype(String doctype) {
        this.doctype = doctype;
    }

    public String getProjectAcronym() {
        return projectAcronym;
    }

    public void setProjectAcronym(String projectAcronym) {
        this.projectAcronym = projectAcronym;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getJournal() {
        return journal;
    }

    public void setJournal(String journal) {
        this.journal = journal;
    }

    public String getDoiId() {
        return doiId;
    }

    public void setDoiId(String doiId) {
        this.doiId = doiId;
    }

    public String getPmcId() {
        return pmcId;
    }

    public void setPmcId(String pmcId) {
        this.pmcId = pmcId;
    }

    @Override
    public String toString() {
        return "VisualizationDocument{" +
                "pubyear='" + pubyear + '\'' +
                ", abstractField='" + abstractField + '\'' +
                ", abstractPmc='" + abstractPmc + '\'' +
                ", otherAbstractPmc='" + otherAbstractPmc + '\'' +
                ", doctype='" + doctype + '\'' +
                ", projectAcronym='" + projectAcronym + '\'' +
                ", project='" + project + '\'' +
                ", journal='" + journal + '\'' +
                ", doiId='" + doiId + '\'' +
                ", pmcId='" + pmcId + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
